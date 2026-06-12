// File: java/com/iispl/serviceImpl/CheckerServiceImpl.java

package com.iispl.serviceImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;

import com.iispl.dao.OutwardBatchDao;
import com.iispl.dao.OutwardChequeDao;
import com.iispl.daoImpl.OutwardBatchDaoImpl;
import com.iispl.daoImpl.OutwardChequeDaoImpl;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.service.AuditService;
import com.iispl.service.CheckerService;
import com.iispl.service.NotificationService;
import com.iispl.util.HibernateUtil;

/**
 * File    : com/iispl/serviceImpl/CheckerServiceImpl.java
 * Purpose : Implements all Checker Outward business logic.
 *
 * Design notes:
 *
 *   DAOs are instantiated directly (no Spring DI) — consistent with
 *   every other ServiceImpl in this project.
 *
 *   OutwardCheckerAction audit records are saved directly via a Hibernate
 *   session opened in the private helper saveCheckerActionLog().
 *   This is intentional — the audit log save is non-critical; if it fails,
 *   we log the error but do NOT roll back the cheque status update.
 *
 *   Batch total decrement rule:
 *     Maker rejection  → uses rejectWithReason() → decrements cheque_count + actual_amount
 *     Checker rejection → uses updateCheckerStatus() only → NO decrement
 *     This is a deliberate business rule.
 *
 *   Auto-approval:
 *     After every passCheque() and rejectCheque() call, isAllActioned() is
 *     checked. If it returns true, approveBatch() is called automatically.
 *
 *   Refer loop awareness:
 *     referCheque() sets the batch to CHECKER_HOLD. The batch will NOT
 *     be auto-approved until the Maker corrects and re-submits.
 */
public class CheckerServiceImpl implements CheckerService {

    private final OutwardBatchDao  batchDao  = new OutwardBatchDaoImpl();
    private final OutwardChequeDao chequeDao = new OutwardChequeDaoImpl();
    private final AuditService auditService = new AuditServiceImpl();
    private final NotificationService notifService = new NotificationServiceImpl();
    // ════════════════════════════════════════════════════════════════════════
    //  Queue Loading
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns all batches in the Checker queue.
     * Statuses: SUBMITTED, CHECKER_IN_PROGRESS, CHECKER_HOLD
     */
    @Override
    public List<OutwardBatch> getCheckerQueueBatches() {
        try {
            List<OutwardBatch> batches = batchDao.findCheckerQueueBatches();
            System.out.println("CheckerService → getCheckerQueueBatches: found "
                    + batches.size() + " batch(es)");
            return batches;
        } catch (Exception e) {
            System.err.println("CheckerService → getCheckerQueueBatches failed: "
                    + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns the five dashboard summary counts.
     * Map keys: "pending", "inProgress", "hold", "approved", "exported"
     */
    @Override
    public Map<String, Integer> getDashboardCounts() {
        Map<String, Integer> counts = new HashMap<>();
        try {
            counts.put("pending",    batchDao.countByStatus("SUBMITTED"));
            counts.put("inProgress", batchDao.countByStatus("CHECKER_IN_PROGRESS"));
            counts.put("hold",       batchDao.countByStatus("CHECKER_HOLD") + batchDao.countByStatus("REFER_BACK"));
            counts.put("approved",   batchDao.countByStatus("CHECKER_APPROVED"));
            counts.put("exported",   batchDao.countByStatus("EXPORTED"));

            System.out.println("CheckerService → getDashboardCounts:"
                    + " pending="    + counts.get("pending")
                    + " inProgress=" + counts.get("inProgress")
                    + " hold="       + counts.get("hold")
                    + " approved="   + counts.get("approved")
                    + " exported="   + counts.get("exported"));

        } catch (Exception e) {
            System.err.println("CheckerService → getDashboardCounts failed: "
                    + e.getMessage());
            counts.putIfAbsent("pending",    0);
            counts.putIfAbsent("inProgress", 0);
            counts.putIfAbsent("hold",       0);
            counts.putIfAbsent("approved",   0);
            counts.putIfAbsent("exported",   0);
        }
        return counts;
    }
    // ════════════════════════════════════════════════════════════════════════
    //  Batch Operations
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Opens a batch for Checker processing.
     *
     * Business rules:
     *   SUBMITTED           → update to CHECKER_IN_PROGRESS  (first open)
     *   CHECKER_IN_PROGRESS → no change                      (resume)
     *   CHECKER_HOLD        → no change                      (resume after refer loop)
     *   any other status    → no-op, return true
     */
    @Override
    public boolean openBatch(Long batchDbId, Long checkerId) {
        if (batchDbId == null || checkerId == null) {
            System.err.println("CheckerService → openBatch: null batchDbId or checkerId");
            return false;
        }

        try {
            // Load the batch from queue to inspect its current status
            List<OutwardBatch> queue = batchDao.findCheckerQueueBatches();
            OutwardBatch target = null;
            for (OutwardBatch b : queue) {
                if (b.getId().equals(batchDbId)) {
                    target = b;
                    break;
                }
            }

            if (target == null) {
                // Not in queue — may already be CHECKER_APPROVED or does not exist
                System.out.println("CheckerService → openBatch: batch id=" + batchDbId
                        + " not found in checker queue. May already be approved.");
                return true;
            }

            String currentStatus = target.getStatus();

            if ("SUBMITTED".equals(currentStatus)) {
                boolean ok = batchDao.updateStatus(batchDbId, "CHECKER_IN_PROGRESS");
                System.out.println("CheckerService → openBatch: batch id=" + batchDbId
                        + " SUBMITTED → CHECKER_IN_PROGRESS. result=" + ok);
                return ok;
            }

            if ("CHECKER_IN_PROGRESS".equals(currentStatus)
                    || "CHECKER_HOLD".equals(currentStatus)) {
                // Resuming — no status change needed
                System.out.println("CheckerService → openBatch: batch id=" + batchDbId
                        + " already " + currentStatus + " — resuming, no change.");
                return true;
            }

            // Any other status — no-op
            System.out.println("CheckerService → openBatch: batch id=" + batchDbId
                    + " has status " + currentStatus + " — no-op.");
            return true;

        } catch (Exception e) {
            System.err.println("CheckerService → openBatch failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Marks a batch as fully verified (CHECKER_APPROVED).
     *
     * FIX: The previous version called batchDao.updateApproved() which does not
     * exist in OutwardBatchDao. This version uses the existing updateStatus()
     * for the status change, then runs a separate native SQL update to stamp
     * verified_by and verified_at directly via Hibernate.
     *
     * Called automatically from checkAndApproveBatch() after all cheques
     * are actioned (passed or rejected — not referred).
     */
    @Override
    public boolean approveBatch(Long batchDbId, Long checkerId) {
        if (batchDbId == null || checkerId == null) {
            System.err.println("CheckerService → approveBatch: null input");
            return false;
        }

        Session     session = null;
        Transaction tx      = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx      = session.beginTransaction();

            // Single SQL: update status + verified_by + verified_at together
            String sql = "UPDATE outward_batch "
                       + "SET status       = 'CHECKER_APPROVED', "
                       + "    verified_by  = :checkerId, "
                       + "    verified_at  = NOW(), "
                       + "    updated_at   = NOW() "
                       + "WHERE id = :batchDbId";

            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("checkerId",  checkerId);
            q.setParameter("batchDbId",  batchDbId);
            int rows = q.executeUpdate();
            tx.commit();

            System.out.println("CheckerService → approveBatch: batch id=" + batchDbId
                    + " → CHECKER_APPROVED by checkerId=" + checkerId
                    + " rows updated=" + rows);
            return rows > 0;

        } catch (Exception e) {
            if (tx != null) {
                try { tx.rollback(); } catch (Exception rb) { /* ignore */ }
            }
            System.err.println("CheckerService → approveBatch failed: " + e.getMessage());
            return false;
        } finally {
            if (session != null) {
                try { session.close(); } catch (Exception sc) { /* ignore */ }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Cheque Loading
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns ALL cheques for a batch, ordered by seq_no ASC.
     * No status filtering — Checker sees every cheque including already-actioned ones.
     */
    @Override
    public List<OutwardCheque> getChequesForBatch(Long batchDbId) {
        if (batchDbId == null) {
            System.err.println("CheckerService → getChequesForBatch: null batchDbId");
            return Collections.emptyList();
        }
        try {
            List<OutwardCheque> cheques = chequeDao.findAllByBatchDbId(batchDbId);
            System.out.println("CheckerService → getChequesForBatch: found "
                    + cheques.size() + " cheque(s) for batchDbId=" + batchDbId);
            return cheques;
        } catch (Exception e) {
            System.err.println("CheckerService → getChequesForBatch failed: "
                    + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Cheque Actions
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Passes a single cheque.
     *
     * Steps:
     *   1. Update cheque status → CHECKER_PASSED
     *   2. Save audit log to outward_checker_actions
     *   3. Check if all cheques in batch are now actioned
     *   4. If all done → approve batch automatically
     */
    @Override
    public boolean passCheque(Long chequeId, Long checkerId, Long batchDbId) {
        if (chequeId == null || checkerId == null || batchDbId == null) {
            System.err.println("CheckerService → passCheque: null input");
            return false;
        }

        boolean updated = chequeDao.updateCheckerStatus(chequeId, "CHECKER_PASSED");
        if (!updated) {
            System.err.println("CheckerService → passCheque: DB update failed for chequeId="
                    + chequeId);
            return false;
        }

        System.out.println("CheckerService → passCheque: chequeId=" + chequeId
                + " → CHECKER_PASSED");

        // Audit log — non-critical
        saveCheckerActionLog(chequeId, batchDbId, checkerId, "PASSED", null, null);

        // Auto-approve batch if all cheques are now done
        checkAndApproveBatch(batchDbId, checkerId);

        return true;
    }

    /**
     * Rejects a single cheque at the Checker stage.
     * Sets cheque status to CHECKER_REJECTED, saves action log,
     * decrements batch count and amount, then checks if batch is fully actioned.
     */
    @Override
    public boolean rejectCheque(Long chequeId,
                                 String reasonCode,
                                 String remarks,
                                 Long checkerId,
                                 Long batchDbId) {
        if (chequeId == null || checkerId == null || batchDbId == null) {
            System.err.println("CheckerService → rejectCheque: null input");
            return false;
        }
        if (isBlank(reasonCode)) {
            System.err.println("CheckerService → rejectCheque: reasonCode is required");
            return false;
        }

        boolean updated = chequeDao.updateCheckerStatus(chequeId, "CHECKER_REJECTED");
        if (!updated) {
            System.err.println("CheckerService → rejectCheque: DB update failed for chequeId="
                    + chequeId);
            return false;
        }

        // Decrement batch cheque_count and actual_amount for this rejected cheque
        OutwardCheque rejected = chequeDao.findById(chequeId);
        if (rejected != null) {
            batchDao.decrementChequeFromBatch(batchDbId, rejected.getAmount());
        } else {
            System.err.println("CheckerService → rejectCheque: cheque not found for decrement, chequeId="
                    + chequeId);
        }

        System.out.println("CheckerService → rejectCheque: chequeId=" + chequeId
                + " → CHECKER_REJECTED. reason=" + reasonCode);

        saveCheckerActionLog(chequeId, batchDbId, checkerId,
                "REJECTED", reasonCode.trim(), remarks);

        checkAndApproveBatch(batchDbId, checkerId);

        return true;
    }

    /**
     * Refers a single cheque back to the Maker for correction.
     *
     * Does NOT check isAllActioned() — a referred cheque is not a final decision.
     * The batch cannot be approved while cheques are referred.
     */
    @Override
    public boolean referCheque(Long chequeId,
                                String reasonCode,
                                String remarks,
                                Long checkerId,
                                Long batchDbId) {
        if (chequeId == null || checkerId == null || batchDbId == null) {
            System.err.println("CheckerService → referCheque: null input");
            return false;
        }
        if (isBlank(reasonCode)) {
            System.err.println("CheckerService → referCheque: reasonCode is required");
            return false;
        }

        boolean chequeUpdated = chequeDao.updateCheckerStatus(chequeId, "CHECKER_REFERRED");
        if (!chequeUpdated) {
            System.err.println("CheckerService → referCheque: cheque update failed for chequeId="
                    + chequeId);
            return false;
        }

        System.out.println("CheckerService → referCheque: chequeId=" + chequeId
                + " → CHECKER_REFERRED. reason=" + reasonCode);

        saveCheckerActionLog(chequeId, batchDbId, checkerId,
                "REFERRED", reasonCode.trim(), remarks);

        boolean batchUpdated = batchDao.updateStatus(batchDbId, "CHECKER_HOLD");
        if (!batchUpdated) {
            System.err.println("CheckerService → referCheque: batch hold failed for batchDbId="
                    + batchDbId);
            return false;
        }

        System.out.println("CheckerService → referCheque: batchDbId=" + batchDbId
                + " → CHECKER_HOLD");
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Completion Check
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns true when all cheques in the batch have been actioned.
     * Uses countPendingCheckerActions() which counts cheques with status = ENTRY_DONE.
     * Returns true when that count is 0.
     */
    @Override
    public boolean isAllActioned(Long batchDbId) {
        if (batchDbId == null) return false;
        try {
            int pending = chequeDao.countPendingCheckerActions(batchDbId);
            System.out.println("CheckerService → isAllActioned: batchDbId=" + batchDbId
                    + " pending ENTRY_DONE count=" + pending);
            return pending == 0;
        } catch (Exception e) {
            System.err.println("CheckerService → isAllActioned failed: " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Checks if all cheques are actioned and auto-approves the batch if so.
     * Called after every passCheque() and rejectCheque().
     * NOT called after referCheque() — referred batch goes to CHECKER_HOLD.
     */
    private void checkAndApproveBatch(Long batchDbId, Long checkerId) {
        try {
            if (isAllActioned(batchDbId)) {
                System.out.println("CheckerService → all cheques actioned for batchDbId="
                        + batchDbId + " — approving batch.");
                boolean approved = approveBatch(batchDbId, checkerId);
                if (!approved) {
                    System.err.println("CheckerService → checkAndApproveBatch: "
                            + "approveBatch failed for batchDbId=" + batchDbId);
                }
            }
        } catch (Exception e) {
            System.err.println("CheckerService → checkAndApproveBatch failed: "
                    + e.getMessage());
        }
    }

    /**
     * Inserts a row into outward_checker_actions.
     * The 4-param version (existing) defaults reasonText to null.
     * The 7-param version (new) lets caller supply a richer reason_text.
     */
    private void saveCheckerActionLog(Long chequeId, Long batchDbId, Long checkerId,
                                       String action, String reasonCode, String remarks,
                                       String reasonText) {

        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();
            String sql = "INSERT INTO outward_checker_actions "
                       + " (outward_cheque_id, outward_batch_id, checker_id, "
                       + "  action, reason_code, reason_text, remarks, actioned_at) "
                       + "VALUES (:chequeId, :batchId, :checkerId, "
                       + "        :action, :reasonCode, :reasonText, :remarks, NOW())";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("chequeId",   chequeId);
            q.setParameter("batchId",    batchDbId);
            q.setParameter("checkerId",  checkerId);
            q.setParameter("action",     action);
            q.setParameter("reasonCode", reasonCode);
            q.setParameter("reasonText", reasonText);
            q.setParameter("remarks",    remarks);
            q.executeUpdate();
            tx.commit();
            System.out.println("CheckerService → audit logged: cheque=" + chequeId
                    + " action=" + action + " reasonText=" + reasonText);
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("CheckerService → audit log failed: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    // Keep your existing 6-param overload pointing to the new 7-param version
    private void saveCheckerActionLog(Long chequeId, Long batchDbId, Long checkerId,
                                       String action, String reasonCode, String remarks) {
        saveCheckerActionLog(chequeId, batchDbId, checkerId,
                action, reasonCode, remarks, null);
    }
    /**
     * Maps a 2-digit reason code to its human-readable description.
     * Returns null for PASSED actions where reasonCode is null.
     */
    private String buildReasonText(String reasonCode) {
        if (isBlank(reasonCode)) return null;
        switch (reasonCode.trim()) {
            case "01": return "Funds Insufficient";
            case "02": return "Exceeds Arrangement";
            case "03": return "Full Cover Not Received";
            case "04": return "Payment Stopped by Drawer";
            case "05": return "Payment Countermanded by Court Order";
            case "06": return "Payee's Endorsement Required";
            case "07": return "Payee's Endorsement Irregular / Requires Bank's Guarantee";
            case "08": return "Drawer's Signature Incomplete / Does Not Match / Requires Verification";
            case "09": return "Alteration in Date / Amount in Words / Amount in Figures Requires Drawer's Authentication";
            case "10": return "Cheque Post-Dated / Stale Dated (Instrument Outdated / Expired)";
            case "11": return "Amount in Words and Figures Differs";
            case "12": return "Crossed Cheque Cannot be Paid in Cash";
            case "13": return "Account Closed / Transferred";
            case "14": return "Account Blocked / Frozen / Under Attachment";
            case "15": return "Not Drawn on Us (Wrong Bank — Cheque Not Issued by This Bank)";
            case "16": return "MICR Band Damaged / Unreadable / Missing";
            case "17": return "Image Not Clear / Poor Image Quality";
            case "18": return "Non-CTS 2010 Compliant Instrument";
            case "19": return "Instrument Mutilated / Torn — Cannot be Processed";
            case "20": return "Other Reason (Specify in Remarks)";
            default:   return reasonCode.trim();
        }
    }

    /**
     * Returns true if a string is null or blank.
     */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    
    
    @Override
    public boolean referCheque(Long chequeId,
                               String reasonCode,
                               String referToModule,
                               String remarks,
                               Long checkerId,
                               Long batchDbId) {

        // ── Input validation ──
        if (chequeId == null || checkerId == null || batchDbId == null) {
            System.err.println("CheckerService → referCheque: null input");
            return false;
        }
        if (isBlank(reasonCode)) {
            System.err.println("CheckerService → referCheque: reasonCode is required");
            return false;
        }
        if (isBlank(referToModule)) {
            System.err.println("CheckerService → referCheque: referToModule is required");
            return false;
        }
        // Only 2 valid module values
        String mod = referToModule.trim();
        if (!"MICR_REPAIR".equals(mod) && !"DATA_ENTRY".equals(mod)) {
            System.err.println("CheckerService → referCheque: invalid module: " + mod);
            return false;
        }

        // ── Step 1: update cheque with status + module ──
        boolean ok = chequeDao.markReferredWithModule(chequeId, mod);
        if (!ok) {
            System.err.println("CheckerService → referCheque: cheque update failed for chequeId="
                    + chequeId);
            return false;
        }

        System.out.println("CheckerService → referCheque: chequeId=" + chequeId
                + " → CHECKER_REFERRED, module=" + mod + ", reason=" + reasonCode);

        // ── Step 2: audit log — encode module into reason_text ──
        String reasonText = "REFER_TO=" + mod + " | code=" + reasonCode;
        saveCheckerActionLog(chequeId, batchDbId, checkerId,
                "REFER", reasonCode.trim(), remarks, reasonText);

        // ── Step 3: hold the batch (working session, not done yet) ──
        boolean batchUpdated = batchDao.updateStatus(batchDbId, "CHECKER_HOLD");
        if (!batchUpdated) {
            System.err.println("CheckerService → referCheque: batch hold failed for batchDbId="
                    + batchDbId);
            return false;
        }

        System.out.println("CheckerService → referCheque: batchDbId=" + batchDbId
                + " → CHECKER_HOLD (temporary; finalizeBatchIfDone will set REFER_BACK)");
        return true;
    }

    /**
     * Called by the composer AFTER the last cheque in a batch is actioned.
     * Decides the final batch status:
     *   - Any referred cheque  → REFER_BACK   (goes back to Maker)
     *   - All passed (no refs) → CHECKER_APPROVED  (ready for DEM Export)
     *   - All rejected         → CHECKER_APPROVED  (with 0 cheques to export — still finalized)
     *
     * Returns the final batch status that was set.
     */
    @Override
    public String finalizeBatchIfDone(Long batchDbId, Long checkerId) {
        if (batchDbId == null) return null;

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT COUNT(*) FROM outward_cheque "
                       + "WHERE batch_id = :id "
                       + "  AND status = 'CHECKER_REFERRED'";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("id", batchDbId);
            Number n = (Number) q.uniqueResult();
            int referredCount = n != null ? n.intValue() : 0;

            if (referredCount > 0) {
                batchDao.updateStatus(batchDbId, "REFER_BACK");

                auditService.log(
                    checkerId,
                    AuditService.M_CHECKER_QUEUE,
                    AuditService.A_BATCH_REFERRED,
                    AuditService.E_OUTWARD_BATCH,
                    batchDbId,
                    "status=CHECKER_HOLD",
                    "status=REFER_BACK, referredCount=" + referredCount);

                // ── Notification: tell the maker their batch was referred back ──
                // Step 1: fetch batch_id string and maker's user id from DB
                String batchIdStr = fetchBatchIdStr(session, batchDbId);
                Long   makerId    = fetchBatchMakerId(session, batchDbId);

                // Step 2: build the module list so the maker knows where to go
                int micrCount = chequeDao.countReferredByModule(batchDbId, "MICR_REPAIR");
                int dataCount = chequeDao.countReferredByModule(batchDbId, "DATA_ENTRY");

                StringBuilder moduleSb = new StringBuilder();
                if (micrCount > 0) moduleSb.append("MICR_REPAIR");
                if (dataCount > 0) {
                    if (moduleSb.length() > 0) moduleSb.append(",");
                    moduleSb.append("DATA_ENTRY");
                }

                // Step 3: fire notification (non-blocking — any failure is swallowed inside service)
                notifService.notifyReferBack(
                        batchDbId, batchIdStr, moduleSb.toString(), makerId);

                System.out.println("CheckerService → finalizeBatchIfDone: batchId="
                        + batchDbId + " has " + referredCount
                        + " referred cheque(s) → REFER_BACK");
                return "REFER_BACK";

            } else {
                approveBatch(batchDbId, checkerId);

                auditService.log(
                    checkerId,
                    AuditService.M_CHECKER_QUEUE,
                    AuditService.A_BATCH_APPROVED,
                    AuditService.E_OUTWARD_BATCH,
                    batchDbId,
                    null,
                    "status=CHECKER_APPROVED");

                System.out.println("CheckerService → finalizeBatchIfDone: batchId="
                        + batchDbId + " → CHECKER_APPROVED");
                return "CHECKER_APPROVED";
            }
        } catch (Exception e) {
            System.err.println("CheckerService → finalizeBatchIfDone failed: "
                    + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    /**
     * Fetches the human-readable batch_id string (e.g. B-2026-0609-001)
     * for a given outward_batch primary key.
     * Uses the already-open session — no extra connection needed.
     */
    private String fetchBatchIdStr(Session session, Long batchDbId) {
        try {
            NativeQuery<?> q = session.createNativeQuery(
                "SELECT batch_id FROM outward_batch WHERE id = :id");
            q.setParameter("id", batchDbId);
            Object result = q.uniqueResult();
            return result != null ? result.toString() : ("BATCH-" + batchDbId);
        } catch (Exception e) {
            System.err.println("CheckerService → fetchBatchIdStr failed: " + e.getMessage());
            return "BATCH-" + batchDbId;
        }
    }

    /**
     * Fetches the created_by user id (maker's id) for a given outward_batch.
     * Uses the already-open session.
     */
    private Long fetchBatchMakerId(Session session, Long batchDbId) {
        try {
            NativeQuery<?> q = session.createNativeQuery(
                "SELECT created_by FROM outward_batch WHERE id = :id");
            q.setParameter("id", batchDbId);
            Object result = q.uniqueResult();
            return result != null ? ((Number) result).longValue() : null;
        } catch (Exception e) {
            System.err.println("CheckerService → fetchBatchMakerId failed: " + e.getMessage());
            return null;
        }
    }
}