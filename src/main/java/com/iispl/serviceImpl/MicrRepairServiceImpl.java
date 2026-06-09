package com.iispl.serviceImpl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.iispl.dao.OutwardBatchDao;
import com.iispl.dao.OutwardChequeDao;
import com.iispl.dao.OutwardMicrRepairDao;
import com.iispl.daoImpl.OutwardBatchDaoImpl;
import com.iispl.daoImpl.OutwardChequeDaoImpl;
import com.iispl.daoImpl.OutwardMicrRepairDaoImpl;
import com.iispl.entity.User;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.entity.outward.OutwardMicrRepair;
import com.iispl.service.AuditService;
import com.iispl.service.MicrRepairService;

/**
 * File    : com/iispl/serviceImpl/MicrRepairServiceImpl.java
 * Purpose : Implements MICR Repair business logic.
 *
 * Key responsibilities:
 *   1. Load batch + MICR error cheques for the repair screen
 *   2. Save corrected MICR sub-fields + rebuild micrCodeCorrected
 *   3. Log each changed field to outward_micr_repair audit table
 *   4. After every save or reject → check if all repairs done
 *      If done → update batch status to ENTRY_DONE automatically
 */
public class MicrRepairServiceImpl implements MicrRepairService {

    private final OutwardBatchDao      batchDao  = new OutwardBatchDaoImpl();
    private final OutwardChequeDao     chequeDao = new OutwardChequeDaoImpl();
    private final OutwardMicrRepairDao repairDao = new OutwardMicrRepairDaoImpl();
    private final AuditService auditService = new AuditServiceImpl();

    // ════════════════════════════════════════════════════
    //  Load Batch
    // ════════════════════════════════════════════════════

    /**
     * Fetches batch by batch_id string (e.g. B-2026-0603-001).
     * Returns null if not found.
     */
    @Override
    public OutwardBatch getBatch(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            System.err.println("MicrRepairService → getBatch: batchId is empty");
            return null;
        }
        OutwardBatch batch = batchDao.findByBatchId(batchId.trim());
        if (batch == null) {
            System.err.println("MicrRepairService → getBatch: no batch found for "
                    + batchId);
        }
        return batch;
    }

    // ════════════════════════════════════════════════════
    //  Load MICR Error Cheques
    // ════════════════════════════════════════════════════

    /**
     * Returns all cheques for a batch that:
     *   - have is_micr_error = TRUE
     *   - have repair_status != REPAIRED
     *   - have status != REJECTED
     * These are the cheques the maker must fix on the repair screen.
     */
    @Override
    public List<OutwardCheque> getMicrErrorCheques(Long batchDbId) {
        if (batchDbId == null) {
            System.err.println("MicrRepairService → getMicrErrorCheques: batchDbId is null");
            return Collections.emptyList();
        }
        List<OutwardCheque> list = chequeDao.findMicrErrorCheques(batchDbId);
        System.out.println("MicrRepairService → Found " + list.size()
                + " MICR error cheque(s) for batch id=" + batchDbId);
        return list;
    }

    // ════════════════════════════════════════════════════
    //  Save Repair
    // ════════════════════════════════════════════════════

    /**
     * Saves corrected MICR fields for one cheque.
     *
     * Steps:
     *   1. Validate inputs
     *   2. Detect which fields changed (compare new vs original micrCode)
     *   3. Rebuild micrCodeCorrected from the corrected sub-fields
     *   4. Update outward_cheque in DB (corrected fields + repairStatus=REPAIRED)
     *   5. Log each changed field to outward_micr_repair audit table
     *   6. Check if all repairs done → update batch to ENTRY_DONE if yes
     *
     * @param chequeId   DB id of the cheque being repaired
     * @param chequeNo   cheque number (unchanged — used to rebuild micrCode)
     * @param cityCode   corrected city code   (3 digits)
     * @param bankCode   corrected bank code   (3 digits)
     * @param branchCode corrected branch code (3 digits)
     * @param baseNumber corrected base number (6 digits)
     * @param transCode  corrected transaction code (2 digits)
     * @param remarks    optional maker remarks
     * @param makerId    logged-in maker's DB user id
     */
    @Override
    public boolean saveRepair(Long   chequeId,
                               String chequeNo,
                               String cityCode,
                               String bankCode,
                               String branchCode,
                               String baseNumber,
                               String transCode,
                               String remarks,
                               Long   makerId) {

        // ── Step 1: Validate required inputs ──
        if (chequeId == null || makerId == null) {
            System.err.println("MicrRepairService → saveRepair: "
                    + "chequeId or makerId is null");
            return false;
        }
        if (isBlank(chequeNo) || isBlank(cityCode) || isBlank(bankCode)
                || isBlank(branchCode) || isBlank(baseNumber) || isBlank(transCode)) {
            System.err.println("MicrRepairService → saveRepair: "
                    + "one or more required MICR fields are empty");
            return false;
        }

        // ── Step 2: Rebuild corrected 23-char MICR code ──
        // Format: chequeNo(6) + cityCode(3) + bankCode(3)
        //         + branchCode(3) + baseNumber(6) + transCode(2) = 23 chars
        String correctedMicr = pad(chequeNo,   6)
                             + pad(cityCode,   3)
                             + pad(bankCode,   3)
                             + pad(branchCode, 3)
                             + pad(baseNumber, 6)
                             + pad(transCode,  2);

        System.out.println("MicrRepairService → Corrected MICR code: "
                + correctedMicr);

        // ── Step 3: Detect changed fields for audit log ──
        // We decompose the original raw micrCode to find what the correct
        // values should be. Any field that was changed gets logged.
        //
        // We need the original values from DB, but since the service doesn't
        // load the cheque object itself (to keep DB calls minimal), we just
        // log the new value as a combined "MICR_REPAIR" audit record.
        // Individual field logs would require loading the cheque — can be
        // added in a future phase.

     // ── Step 4: Load ORIGINAL cheque BEFORE update (for audit snapshot) ──
        OutwardCheque before = chequeDao.findById(chequeId);
        if (before == null) {
            System.err.println("MicrRepairService → saveRepair: cheque not found id="
                    + chequeId);
            return false;
        }

        // ── Step 5: Update cheque in DB ──
        boolean updated = chequeDao.updateMicrRepaired(
                chequeId,
                cityCode.trim(),
                bankCode.trim(),
                branchCode.trim(),
                baseNumber.trim(),
                transCode.trim(),
                correctedMicr);

        if (!updated) {
            System.err.println("MicrRepairService → saveRepair: "
                    + "DB update failed for cheque id=" + chequeId);
            return false;
        }

        // ── Step 6: Log audit with REAL old + new values per field (Phase F1) ──
        writeRepairAuditLog(before,
                             cityCode.trim(),
                             bankCode.trim(),
                             branchCode.trim(),
                             baseNumber.trim(),
                             transCode.trim(),
                             correctedMicr,
                             remarks,
                             makerId);

        // ── Step 6: Clear referral if this cheque was sent by Checker ──
        // Safe to call unconditionally — DAO method only updates when
        // referred_to_module IS NOT NULL.
        // For REFER_BACK flow:  status moves CHECKER_REFERRED → ENTRY_DONE
        //                       and referred_to_module is cleared.
        // For normal flow    :  no-op (referred_to_module was already NULL).
        chequeDao.clearReferral(chequeId, "ENTRY_DONE");

        System.out.println("MicrRepairService → Repair saved. "
                + "chequeId=" + chequeId
                + " | correctedMicr=" + correctedMicr);
     // F3-B audit log
        auditService.log(
            makerId,
            AuditService.M_MICR_REPAIR,
            AuditService.A_MICR_REPAIRED,
            AuditService.E_OUTWARD_CHEQUE,
            chequeId,
            before != null ? "micr=" + before.getMicrCode() : null,
            "micr=" + correctedMicr);

        return true;
    }

    // ════════════════════════════════════════════════════
    //  Reject Cheque
    // ════════════════════════════════════════════════════

    /**
     * Rejects a cheque during MICR Repair.
     * Marks it REJECTED with reason code + remarks.
     * Decrements batch cheque_count and actual_amount.
     */
    @Override
    public boolean rejectCheque(Long   chequeId,
                                  String reasonCode,
                                  String remarks,
                                  Long   makerId) {
        if (chequeId == null || makerId == null) {
            System.err.println("MicrRepairService → rejectCheque: "
                    + "chequeId or makerId is null");
            return false;
        }
        if (isBlank(reasonCode)) {
            System.err.println("MicrRepairService → rejectCheque: "
                    + "reasonCode is required");
            return false;
        }

        boolean ok = chequeDao.rejectWithReason(
                chequeId,
                reasonCode.trim(),
                remarks != null ? remarks.trim() : "",
                makerId);

        if (ok) {
            chequeDao.clearReferral(chequeId, "REJECTED");

            auditService.log(
                makerId,
                AuditService.M_MICR_REPAIR,
                AuditService.A_CHEQUE_REJECTED,
                AuditService.E_OUTWARD_CHEQUE,
                chequeId,
                null,
                "reason=" + reasonCode + ", remarks=" + remarks);

            System.out.println("MicrRepairService → Cheque id=" + chequeId
                    + " rejected. Reason=" + reasonCode);
        }
        return ok;
    }

    // ════════════════════════════════════════════════════
    //  Check All Repairs Done
    // ════════════════════════════════════════════════════

    /**
     * Returns true when no more MICR-error cheques are pending repair.
     * A cheque is considered "done" if it is either:
     *   - repairStatus = REPAIRED
     *   - status = REJECTED
     */
    @Override
    public boolean isAllRepaired(Long batchDbId) {
        if (batchDbId == null) return false;
        int pending = chequeDao.countPendingMicrRepairs(batchDbId);
        System.out.println("MicrRepairService → Pending MICR repairs for batch id="
                + batchDbId + ": " + pending);
        return pending == 0;
    }

    /**
     * Called by MicrRepairComposer after the last MICR-error cheque in a batch
     * has been repaired. Moves the batch forward to ENTRY_PENDING AND keeps
     * the (denormalized) outward_batch.repair_status in sync.
     *
     * Phase F2 fix:
     *   - Previously only updated status='ENTRY_PENDING'.
     *   - The repair_status column on outward_batch was left at 'NEEDS_REPAIR'
     *     forever, so dashboard counts and reports lied about the batch.
     *   - Now we mark BOTH columns in a single DB call.
     */
    @Override
    public boolean markBatchEntryDone(Long batchDbId) {
        if (batchDbId == null) return false;

        boolean ok = batchDao.markRepairsCompleted(batchDbId);
        if (ok) {
            System.out.println("MicrRepairService → Batch id=" + batchDbId
                    + " → status=ENTRY_PENDING, repair_status=REPAIRED. "
                    + "Ready for Account & Amount Entry.");
        } else {
            System.err.println("MicrRepairService → markBatchEntryDone failed "
                    + "for batch id=" + batchDbId);
        }
        return ok;
    }
    // ════════════════════════════════════════════════════
    //  Private Helpers
    // ════════════════════════════════════════════════════

    /**
     * Saves one audit record to outward_micr_repair.
     * Records the full corrected MICR code for traceability.
     * In a future phase this can be expanded to log each
     * individual field change separately.
     */
    /**
     * Loads the original cheque and emits ONE outward_micr_repair row
     * per field that actually changed. Fixes C4 + C5:
     *   - field_name now stores the real field that changed (e.g. 'city_code')
     *     instead of the meaningless constant 'MICR_REPAIR'.
     *   - old_value now stores the ACTUAL previous value (e.g. '989')
     *     instead of the literal placeholder string 'original_micr'.
     *
     * Called AFTER updateMicrRepaired() succeeds. The "before" cheque
     * snapshot is fetched at the start of saveRepair() and passed in here.
     */
    private void writeRepairAuditLog(OutwardCheque before,
                                       String correctedCity,
                                       String correctedBank,
                                       String correctedBranch,
                                       String correctedBase,
                                       String correctedTxnCode,
                                       String correctedMicr,
                                       String remarks,
                                       Long   makerId) {

        if (before == null) {
            System.err.println("MicrRepairService → writeRepairAuditLog: "
                    + "no 'before' snapshot — skipping audit");
            return;
        }

        Long   chequeId  = before.getId();
        String oldCity   = nz(before.getCityCode());
        String oldBank   = nz(before.getBankCode());
        String oldBranch = nz(before.getBranchCode());
        String oldBase   = nz(before.getBaseNumber());
        String oldTxn    = nz(before.getTransactionCode());
        String oldMicr   = nz(before.getMicrCode());

        // Per-field comparison — only log fields that actually changed
        logFieldChange(chequeId, makerId, "city_code",        oldCity,   nz(correctedCity));
        logFieldChange(chequeId, makerId, "bank_code",        oldBank,   nz(correctedBank));
        logFieldChange(chequeId, makerId, "branch_code",      oldBranch, nz(correctedBranch));
        logFieldChange(chequeId, makerId, "base_number",      oldBase,   nz(correctedBase));
        logFieldChange(chequeId, makerId, "transaction_code", oldTxn,    nz(correctedTxnCode));
        // Also log the full reconstructed MICR for convenience
        logFieldChange(chequeId, makerId, "micr_code",        oldMicr,   nz(correctedMicr));

        // Maker remarks attached as a single info row (only if provided)
        if (remarks != null && !remarks.trim().isEmpty()) {
            try {
                OutwardMicrRepair row = new OutwardMicrRepair();
                OutwardCheque chequeRef = new OutwardCheque();
                chequeRef.setId(chequeId);
                row.setOutwardCheque(chequeRef);

                row.setFieldName("remarks");
                row.setOldValue("");
                row.setNewValue(remarks.trim());
                row.setRepairType("MAKER_REMARK");

                User maker = new User();
                maker.setId(makerId);
                row.setRepairedBy(maker);
                row.setRepairedAt(LocalDateTime.now());

                repairDao.save(row);
            } catch (Exception e) {
                System.err.println("MicrRepairService → remark audit failed: "
                        + e.getMessage());
            }
        }
    }

    /**
     * Inserts ONE outward_micr_repair row for a single field, but only
     * if old != new. Empty / unchanged fields are skipped.
     */
    private void logFieldChange(Long   chequeId,
                                  Long   makerId,
                                  String fieldName,
                                  String oldValue,
                                  String newValue) {
        if (oldValue == null) oldValue = "";
        if (newValue == null) newValue = "";
        if (oldValue.equals(newValue)) return;  // no change → no audit row

        try {
            OutwardMicrRepair row = new OutwardMicrRepair();
            OutwardCheque cheque = new OutwardCheque();
            cheque.setId(chequeId);
            row.setOutwardCheque(cheque);

            row.setFieldName(fieldName);
            row.setOldValue(oldValue);
            row.setNewValue(newValue);
            row.setRepairType("MICR_FIELD_FIX");

            User maker = new User();
            maker.setId(makerId);
            row.setRepairedBy(maker);
            row.setRepairedAt(LocalDateTime.now());

            repairDao.save(row);
            System.out.println("MicrRepairService → audit: cheque=" + chequeId
                    + " field=" + fieldName
                    + " old='" + oldValue + "' → new='" + newValue + "'");
        } catch (Exception e) {
            System.err.println("MicrRepairService → logFieldChange failed: "
                    + e.getMessage());
        }
    }

    /** Null-safe string helper. */
    private String nz(String s) { return s == null ? "" : s; }
    /**
     * Pads a string with leading zeros to the required length.
     * Truncates if longer than required.
     *
     * Examples:
     *   pad("7",      3) → "007"
     *   pad("987",    3) → "987"
     *   pad("12345",  6) → "012345"
     *   pad("123456", 6) → "123456"
     *   pad(null,     3) → "000"
     */
    private String pad(String value, int length) {
        if (value == null) value = "";
        value = value.trim();
        // Truncate if too long
        if (value.length() > length) {
            return value.substring(0, length);
        }
        // Pad with leading zeros if too short
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < length) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }

    /**
     * Returns true if a string is null or blank.
     */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    
    @Override
    public List<OutwardBatch> getBatchesNeedingRepair(Long makerId) {
        if (makerId == null) return Collections.emptyList();
        List<OutwardBatch> list = batchDao.findNeedsRepairByMaker(makerId);
        System.out.println("MicrRepairService → Found " + list.size()
                + " batch(es) needing repair for maker id=" + makerId);
        return list;
    }    
}