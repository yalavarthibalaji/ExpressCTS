package com.iispl.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iispl.dao.InwardChequeDao;
import com.iispl.daoImpl.InwardChequeDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.DateAmountService;

/**
 * DateAmountServiceImpl
 *
 * Business layer for:
 *   Step 2 — Date & Amount Repair  (save / reject / referBack)
 *   Step 3 — Payee Name & Account Entry  (savePayeeAndAccount)
 *   Batch submission to Checker  (submitBatchToChecker)
 *
 * Uses InwardChequeDao exclusively — ChequeRepairDao and DateAmountDao
 * have been removed. ChequeRepairServiceImpl and PayeeAccountServiceImpl
 * have been deleted; their responsibilities are covered here.
 *
 * NOTE: Batch-level operations (findBatchById, updateBatchStatus) are
 * handled by RejectRepairServiceImpl via InwardBatchDao.
 * This service is purely cheque-level.
 */
public class DateAmountServiceImpl implements DateAmountService {

    private static final Logger LOG =
            Logger.getLogger(DateAmountServiceImpl.class.getName());

    private final InwardChequeDao chequeDao;

    // ── Constructors ──────────────────────────────────────────────────────

    /** Default — creates its own DAO (non-Spring). */
    public DateAmountServiceImpl() {
        this.chequeDao = new InwardChequeDaoImpl();
    }

    /** Injectable constructor for testing. */
    public DateAmountServiceImpl(InwardChequeDao chequeDao) {
        this.chequeDao = chequeDao;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  QUERIES
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Fetch all cheques in a batch by numeric batch PK.
     * Used by both Step 2 and Step 3 composers.
     *
     * @param batchId numeric PK of the parent InwardBatch row
     */
    public List<InwardCheque> getChequesByBatchId(Long batchId) {
        if (batchId == null) {
            LOG.warning("getChequesByBatchId: null batchId");
            return new ArrayList<>();
        }
        try {
            List<InwardCheque> list = chequeDao.findByBatchId(batchId);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getChequesByBatchId failed, batchId=" + batchId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Fetch cheques in a batch filtered by repairStatus.
     *
     * @param batchId      numeric PK of the parent InwardBatch row
     * @param repairStatus e.g. "NEEDS_REPAIR", "REPAIRED", "REFERRED_BACK"
     */
    public List<InwardCheque> getChequesByBatchIdAndStatus(
            Long batchId, String repairStatus) {

        if (batchId == null || isBlank(repairStatus)) {
            LOG.warning("getChequesByBatchIdAndStatus: null/blank argument");
            return new ArrayList<>();
        }
        try {
            List<InwardCheque> list =
                    chequeDao.findByBatchIdAndRepairStatus(batchId, repairStatus.trim());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "getChequesByBatchIdAndStatus failed, batchId=" + batchId, e);
            return new ArrayList<>();
        }
    }

    public InwardCheque getChequeById(Long chequeId) {
        if (chequeId == null) {
            LOG.warning("getChequeById: null chequeId");
            return null;
        }
        try {
            return chequeDao.findById(chequeId);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getChequeById failed, id=" + chequeId, e);
            return null;
        }
    }

    /**
     * Count cheques still pending repair in a batch.
     * Absorbed from the deleted PayeeAccountServiceImpl.countPending().
     * Uses the DAO count query (single DB round-trip) instead of loading
     * the full list and streaming.
     *
     * @param batchId numeric PK of the parent InwardBatch row
     */
    public long countPendingRepairs(Long batchId) {
        if (batchId == null) return 0;
        try {
            return chequeDao.countPendingRepairsByBatchId(batchId);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "countPendingRepairs failed, batchId=" + batchId, e);
            return 0;
        }
    }

    /**
     * Count Step-3 cheques still awaiting payee/account entry.
     * A cheque is pending when payeeName or draweeAccountNumber is blank
     * AND it is not REFERRED_BACK (those are intentionally skipped).
     *
     * Absorbed from the deleted PayeeAccountServiceImpl.countPending(List).
     */
    public long countPendingStep3(List<InwardCheque> cheques) {
        if (cheques == null) return 0;
        return cheques.stream()
                .filter(c -> !"REFERRED_BACK".equalsIgnoreCase(c.getRepairStatus()))
                .filter(c -> isBlank(c.getPayeeName())
                          || isBlank(c.getDraweeAccountNumber()))
                .count();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  STEP 2 — DATE & AMOUNT REPAIR
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Persist corrected OCR date and amount.
     * Caller sets chequeDateOcr, amountOcr, and optionally repairStatus
     * on the entity before calling. Defaults to "REPAIRED" if null.
     *
     * @return true if exactly one row was updated
     */
    @Override
    public boolean saveRepairDateAndAmount(InwardCheque cheque) {
        if (cheque == null || cheque.getId() == null) {
            LOG.warning("saveRepairDateAndAmount: null cheque or id");
            return false;
        }
        try {
            if (cheque.getRepairStatus() == null) {
                cheque.setRepairStatus("REPAIRED");
            }
            int rows = chequeDao.updateDateAndAmount(cheque);
            if (rows != 1)
                LOG.warning("saveRepairDateAndAmount: expected 1 row, got " + rows
                        + " for chequeId=" + cheque.getId());
            return rows == 1;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "saveRepairDateAndAmount failed, chequeId=" + cheque.getId(), e);
            return false;
        }
    }

    /**
     * Reject a cheque.
     * DAO sets repairStatus = 'REJECTED', status = 'RETURNED'.
     *
     * @return true if exactly one row was updated
     */
    @Override
    public boolean rejectCheque(Long chequeId, String rejectReason, String remarks) {
        if (chequeId == null) {
            LOG.warning("rejectCheque: null chequeId");
            return false;
        }
        if (isBlank(rejectReason)) {
            LOG.warning("rejectCheque: blank rejectReason for chequeId=" + chequeId);
            return false;
        }
        try {
            int rows = chequeDao.rejectCheque(
                    chequeId,
                    rejectReason.trim(),
                    remarks != null ? remarks.trim() : "");
            return rows == 1;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "rejectCheque failed, chequeId=" + chequeId, e);
            return false;
        }
    }

    /**
     * Refer a cheque back for clarification.
     * DAO sets repairStatus = 'REFERRED_BACK'.
     *
     * @return true if exactly one row was updated
     */
    @Override
    public boolean referChequeBack(Long chequeId, String referReason, String remarks) {
        if (chequeId == null) {
            LOG.warning("referChequeBack: null chequeId");
            return false;
        }
        try {
            int rows = chequeDao.referChequeBack(
                    chequeId,
                    referReason != null ? referReason.trim() : "REFER",
                    remarks     != null ? remarks.trim()     : "");
            return rows == 1;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "referChequeBack failed, chequeId=" + chequeId, e);
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  STEP 3 — PAYEE NAME & ACCOUNT ENTRY
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Persist payeeName and draweeAccountNumber.
     * DAO sets repairStatus = 'ENTRY_DONE'.
     *
     * @return true if exactly one row was updated
     */
    @Override
    public boolean savePayeeAndAccount(InwardCheque cheque) {
        if (cheque == null || cheque.getId() == null) {
            LOG.warning("savePayeeAndAccount: null cheque or id");
            return false;
        }
        try {
            int rows = chequeDao.updatePayeeAndAccount(cheque);
            if (rows != 1)
                LOG.warning("savePayeeAndAccount: expected 1 row, got " + rows
                        + " for chequeId=" + cheque.getId());
            return rows == 1;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "savePayeeAndAccount failed, chequeId=" + cheque.getId(), e);
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  BATCH SUBMISSION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Mark all cheques in a batch as submitted to the checker.
     * Sets repairStatus = 'SUBMITTED_TO_CHECKER', status = 'SUBMITTED'.
     *
     * @param batchId string batchId on InwardBatch
     * @return number of cheque rows updated
     */
    @Override
    public int submitBatchToChecker(String batchId) {
        if (isBlank(batchId)) {
            LOG.warning("submitBatchToChecker: blank batchId");
            return 0;
        }
        try {
            int rows = chequeDao.submitBatchToChecker(batchId.trim());
            LOG.info("submitBatchToChecker: " + rows
                    + " cheques submitted for batch=" + batchId);
            return rows;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "submitBatchToChecker failed, batchId=" + batchId, e);
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}