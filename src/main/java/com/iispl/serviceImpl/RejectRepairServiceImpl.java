package com.iispl.serviceImpl;

import com.iispl.dao.InwardBatchDao;
import com.iispl.dao.InwardChequeDao;
import com.iispl.daoImpl.InwardBatchDaoImpl;
import com.iispl.daoImpl.InwardChequeDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.RejectRepairService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RejectRepairServiceImpl
 *
 * Business layer for the full Reject & Repair workflow:
 *   Step 1 — MICR Repair
 *   Step 2 — Date & Amount Repair  (delegates to DateAmountServiceImpl)
 *   Step 3 — Payee Name & Account Entry  (delegates to DateAmountServiceImpl)
 *   Batch submission to Checker
 *
 * RejectRepairDao has been deleted. Cheque operations now go through
 * InwardChequeDao; batch operations go through InwardBatchDao.
 */
public class RejectRepairServiceImpl implements RejectRepairService {

    private static final Logger LOG =
            Logger.getLogger(RejectRepairServiceImpl.class.getName());

    // ── Repair-status constants ────────────────────────────────────────────
    private static final String STATUS_NEEDS_REPAIR      = "NEEDS_REPAIR";
    private static final String STATUS_REPAIRED          = "REPAIRED";
    private static final String STATUS_REFERRED_BACK     = "REFERRED_BACK";
    private static final String STATUS_ENTRY_DONE        = "ENTRY_DONE";
    private static final String STATUS_SUBMITTED_CHECKER = "SUBMITTED_TO_CHECKER";
    private static final String STATUS_REPAIR_COMPLETE   = "REPAIR_COMPLETE";
    private static final String STATUS_DATE_AMT_REPAIRED = "DATE_AMT_REPAIRED";
    private static final String STATUS_REJECTED          = "REJECTED";
    private static final String STATUS_REFERRED_STEP2    = "REFERRED_MICR";

    // ── DAOs ──────────────────────────────────────────────────────────────
    private final InwardChequeDao chequeDao;
    private final InwardBatchDao  batchDao;

    // ── Constructors ──────────────────────────────────────────────────────

    /** Default — creates its own DAOs (non-Spring). */
    public RejectRepairServiceImpl() {
        this.chequeDao = new InwardChequeDaoImpl();
        this.batchDao  = new InwardBatchDaoImpl();
    }

    /** Injectable constructor for testing. */
    public RejectRepairServiceImpl(InwardChequeDao chequeDao, InwardBatchDao batchDao) {
        this.chequeDao = chequeDao;
        this.batchDao  = batchDao;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BATCH
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<InwardBatch> getRepairEligibleBatches() {
        try {
            List<InwardBatch> list = batchDao.findRepairEligibleBatches();
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getRepairEligibleBatches failed", e);
            return new ArrayList<>();
        }
    }

    @Override
    public InwardBatch getBatchById(String batchId) {
        if (isBlank(batchId)) return null;
        try {
            return batchDao.findByBatchId(batchId.trim());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getBatchById failed, batchId=" + batchId, e);
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  STEP 1 — MICR REPAIR
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Fetch cheques for Step 1 MICR repair by numeric batch PK.
     *
     * @param batchId numeric PK of the parent InwardBatch row
     */
    @Override
    public List<InwardCheque> getChequesByBatchId(Long batchId) {
        if (batchId == null) return new ArrayList<>();
        try {
            List<InwardCheque> list = chequeDao.findByBatchId(batchId);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getChequesByBatchId failed, batchId=" + batchId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Save MICR repair for a cheque.
     *
     * FIX: Does not call cheque.getBatch().getBatchId() — the batch
     * association is LAZY and the entity is detached at this point.
     * The caller (composer) passes batchId explicitly.
     *
     * @param cheque  the repaired cheque entity
     * @param batchId string batchId passed from the composer (never from cheque.getBatch())
     */
    @Override
    public void saveRepair(InwardCheque cheque, String batchId) {
        if (cheque == null) return;
        try {
            if (isBlank(cheque.getMicrCodeCorrected())) {
                cheque.setMicrCodeCorrected(buildMicr(
                        cheque.getCityCode(),
                        cheque.getBankCode(),
                        cheque.getBranchCode(),
                        cheque.getChequeNo()));
            }
            cheque.setMicrError(false);
            

            chequeDao.updateCheque(cheque);

            if (!isBlank(batchId)) {
                updateBatchMicrCount(batchId.trim());
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "saveRepair failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Save repair failed", e);
        }
    }

    @Override
    public void referBack(Long chequeId, String remarks) {
        if (chequeId == null) return;
        try {
            InwardCheque cheque = chequeDao.findById(chequeId);
            if (cheque == null) {
                LOG.warning("referBack: cheque not found, id=" + chequeId);
                return;
            }
            cheque.setRepairStatus(STATUS_REFERRED_BACK);
            if (!isBlank(remarks)) cheque.setRemarks(remarks.trim());
            chequeDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "referBack failed, chequeId=" + chequeId, e);
            throw new RuntimeException("Refer back failed", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  STEP 2 — DATE & AMOUNT REPAIR
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Fetch cheques for Step 2 by numeric batch PK.
     * Step 2 shows all cheques — no status filter needed.
     */
    @Override
    public List<InwardCheque> getStep2ChequesByBatchId(Long batchId) {
        if (batchId == null) return new ArrayList<>();
        try {
            List<InwardCheque> list = chequeDao.findByBatchId(batchId);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "getStep2ChequesByBatchId failed, batchId=" + batchId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveStep2Repair(InwardCheque cheque) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_DATE_AMT_REPAIRED);
            chequeDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "saveStep2Repair failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Step 2 save failed", e);
        }
    }

    @Override
    public void rejectStep2(InwardCheque cheque, String rejectReason) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_REJECTED);
            if (!isBlank(rejectReason)) cheque.setRemarks(rejectReason.trim());
            chequeDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "rejectStep2 failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Step 2 reject failed", e);
        }
    }

    @Override
    public void referStep2(InwardCheque cheque, String referReason) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_REFERRED_STEP2);
            if (!isBlank(referReason)) cheque.setRemarks(referReason.trim());
            chequeDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "referStep2 failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Step 2 refer failed", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  STEP 3 — PAYEE NAME & ACCOUNT ENTRY
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Fetch cheques for Step 3 by numeric batch PK.
     */
    @Override
    public List<InwardCheque> getStep3ChequesByBatchId(Long batchId) {
        if (batchId == null) return new ArrayList<>();
        try {
            List<InwardCheque> list = chequeDao.findByBatchId(batchId);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "getStep3ChequesByBatchId failed, batchId=" + batchId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveStep3Entry(InwardCheque cheque) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_ENTRY_DONE);
            chequeDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "saveStep3Entry failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Step 3 save failed", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SUBMIT TO CHECKER
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void proceedToInwardChecker(String batchId) {
        if (isBlank(batchId)) return;
        try {
            batchDao.updateBatchStatus(
                    batchId.trim(),
                    STATUS_SUBMITTED_CHECKER,
                    STATUS_REPAIR_COMPLETE);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "proceedToInwardChecker failed, batchId=" + batchId, e);
            throw new RuntimeException("Proceed to checker failed", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private String buildMicr(
            String city, String bank, String branch, String chequeNo) {
        return (city     != null ? city.trim()     : "000")
             + (bank     != null ? bank.trim()     : "000")
             + (branch   != null ? branch.trim()   : "000")
             + (chequeNo != null ? chequeNo.trim() : "000000");
    }

    /**
     * Recount MICR errors remaining in a batch and persist the updated count.
     * Uses the string batchId to look up cheques — avoids touching any lazy
     * association on a detached entity.
     */
    private void updateBatchMicrCount(String batchId) {
        try {
            // Resolve string batchId → numeric PK via batchDao
            InwardBatch batch = batchDao.findByBatchId(batchId);
            if (batch == null) {
                LOG.warning("updateBatchMicrCount: batch not found, batchId=" + batchId);
                return;
            }
            List<InwardCheque> all = chequeDao.findByBatchId(batch.getId());
            if (all == null) return;

            long remaining = all.stream()
                    .filter(c -> c.isMicrError()
                              || STATUS_NEEDS_REPAIR.equalsIgnoreCase(c.getRepairStatus()))
                    .count();

            batchDao.updateBatchMicrErrorCount(batchId, (int) remaining);
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "updateBatchMicrCount failed, batchId=" + batchId, e);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}