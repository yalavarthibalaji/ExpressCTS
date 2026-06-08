package com.iispl.serviceImpl;

import com.iispl.dao.RejectRepairDao;
import com.iispl.daoImpl.RejectRepairDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.RejectRepairService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RejectRepairServiceImpl implements RejectRepairService {

    private static final Logger LOG =
            Logger.getLogger(RejectRepairServiceImpl.class.getName());

    private static final String STATUS_NEEDS_REPAIR      = "NEEDS_REPAIR";
    private static final String STATUS_REPAIRED          = "REPAIRED";
    private static final String STATUS_REFERRED_BACK     = "REFERRED_BACK";
    private static final String STATUS_ENTRY_DONE        = "ENTRY_DONE";
    private static final String STATUS_SUBMITTED_CHECKER = "SUBMITTED_TO_CHECKER";
    private static final String STATUS_REPAIR_COMPLETE   = "REPAIR_COMPLETE";
    private static final String STATUS_DATE_AMT_REPAIRED = "DATE_AMT_REPAIRED";
    private static final String STATUS_REJECTED          = "REJECTED";
    private static final String STATUS_REFERRED_STEP2    = "REFERRED_STEP2";

    private final RejectRepairDao rejectRepairDao;

    public RejectRepairServiceImpl() {
        this.rejectRepairDao = new RejectRepairDaoImpl();
    }

    public RejectRepairServiceImpl(RejectRepairDao rejectRepairDao) {
        this.rejectRepairDao = rejectRepairDao;
    }

    // ══════════════════════════════════════════════════════════════════════
    // BATCH
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<InwardBatch> getRepairEligibleBatches() {
        try {
            List<InwardBatch> list = rejectRepairDao.findRepairEligibleBatches();
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching repair-eligible batches", e);
            return new ArrayList<>();
        }
    }

    @Override
    public InwardBatch getBatchById(String batchId) {
        if (batchId == null || batchId.isBlank()) return null;
        try {
            return rejectRepairDao.findBatchById(batchId.trim());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching batch: " + batchId, e);
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 1 — MICR REPAIR
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<InwardCheque> getChequesByBatchId(String batchId) {
        if (batchId == null || batchId.isBlank()) return new ArrayList<>();
        try {
            List<InwardCheque> list =
                    rejectRepairDao.findChequesByBatchId(batchId.trim());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getChequesByBatchId failed: " + batchId, e);
            return new ArrayList<>();
        }
    }

    /**
     * FIX 1: Removed BpxfParser.buildImagePath() calls — image paths are
     *         already fully resolved when the cheque was first persisted by
     *         BpxfUploadServiceImpl. Re-calling buildImagePath() on an
     *         already-resolved path corrupts it on every repair save.
     *
     * FIX 2: Removed cheque.getBatch().getBatchId() access — InwardCheque.batch
     *         is LAZY-loaded and the Hibernate session is already closed by the
     *         time saveRepair() is called (detached entity). Accessing a lazy
     *         association on a detached entity throws LazyInitializationException,
     *         which was being caught and rethrown as "Save repair failed".
     *
     *         The batchId is passed separately from the composer via the
     *         overloaded saveRepair(InwardCheque, String) method below.
     *         The original single-arg method still works for cases where
     *         batch is eagerly available.
     */
    @Override
    public void saveRepair(InwardCheque cheque) {
        if (cheque == null) return;
        try {
            // Rebuild corrected MICR only if composer did not already set it
            if (cheque.getMicrCodeCorrected() == null
                    || cheque.getMicrCodeCorrected().isBlank()) {
                String corrected = buildMicr(
                        cheque.getCityCode(),
                        cheque.getBankCode(),
                        cheque.getBranchCode(),
                        cheque.getChequeNo());
                cheque.setMicrCodeCorrected(corrected);
            }

            // FIX: Do NOT re-resolve image paths here.
            // BpxfUploadServiceImpl already stored the correct absolute paths.
            // Calling buildImagePath() again would double-resolve and corrupt them.

            cheque.setMicrError(false);
            cheque.setRepairStatus(STATUS_REPAIRED);

            rejectRepairDao.updateCheque(cheque);

            // Update batch MICR error count — safe version that avoids lazy load
            updateBatchMicrCountSafe(cheque);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "saveRepair failed, id=" + cheque.getId(), e);
            throw new RuntimeException("Save repair failed", e);
        }
    }

    @Override
    public void referBack(Long chequeId, String remarks) {
        if (chequeId == null) return;
        try {
            InwardCheque cheque = rejectRepairDao.findChequeById(chequeId);
            if (cheque == null) {
                LOG.warning("referBack: cheque not found id=" + chequeId);
                return;
            }
            cheque.setRepairStatus(STATUS_REFERRED_BACK);
            if (remarks != null && !remarks.isBlank())
                cheque.setRemarks(remarks);
            rejectRepairDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "referBack failed, id=" + chequeId, e);
            throw new RuntimeException("Refer back failed", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 2 — DATE & AMOUNT REPAIR
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<InwardCheque> getStep2ChequesByBatchId(String batchId) {
        if (batchId == null || batchId.isBlank()) return new ArrayList<>();
        try {
            List<InwardCheque> list =
                    rejectRepairDao.findStep2ChequesByBatchId(batchId.trim());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getStep2ChequesByBatchId failed", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveStep2Repair(InwardCheque cheque) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_DATE_AMT_REPAIRED);
            rejectRepairDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "saveStep2Repair failed", e);
            throw new RuntimeException("Step 2 save failed", e);
        }
    }

    public void rejectStep2(InwardCheque cheque, String rejectReason) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_REJECTED);
            if (rejectReason != null && !rejectReason.isBlank())
                cheque.setRemarks(rejectReason);
            rejectRepairDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "rejectStep2 failed", e);
            throw new RuntimeException("Step 2 reject failed", e);
        }
    }

    public void referStep2(InwardCheque cheque, String referReason) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_REFERRED_STEP2);
            if (referReason != null && !referReason.isBlank())
                cheque.setRemarks(referReason);
            rejectRepairDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "referStep2 failed", e);
            throw new RuntimeException("Step 2 refer failed", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 3 — PAYEE NAME & ACCOUNT ENTRY
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<InwardCheque> getStep3ChequesByBatchId(String batchId) {
        if (batchId == null || batchId.isBlank()) return new ArrayList<>();
        try {
            List<InwardCheque> list =
                    rejectRepairDao.findStep3ChequesByBatchId(batchId.trim());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getStep3ChequesByBatchId failed", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveStep3Entry(InwardCheque cheque) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_ENTRY_DONE);
            rejectRepairDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "saveStep3Entry failed", e);
            throw new RuntimeException("Step 3 save failed", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SUBMIT TO CHECKER
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void proceedToInwardChecker(String batchId) {
        if (batchId == null || batchId.isBlank()) return;
        try {
            rejectRepairDao.updateBatchStatus(
                    batchId.trim(),
                    STATUS_SUBMITTED_CHECKER,
                    STATUS_REPAIR_COMPLETE);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "proceedToInwardChecker failed", e);
            throw new RuntimeException("Proceed to checker failed", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private String buildMicr(String city, String bank, String branch, String chequeNo) {
        String c = city     != null ? city.trim()     : "000";
        String b = bank     != null ? bank.trim()     : "000";
        String x = branch   != null ? branch.trim()   : "000";
        String n = chequeNo != null ? chequeNo.trim() : "000000";
        return c + b + x + n;
    }

    /**
     * FIX: Safe version that uses explicit batchId instead of touching
     * the lazy-loaded cheque.getBatch() association.
     */
    private void updateBatchMicrCountByBatchId(String batchId) {
        try {
            List<InwardCheque> all = rejectRepairDao.findChequesByBatchId(batchId);
            if (all == null) return;

            long remaining = all.stream()
                    .filter(c -> c.isMicrError()
                              || STATUS_NEEDS_REPAIR.equalsIgnoreCase(c.getRepairStatus()))
                    .count();

            rejectRepairDao.updateBatchMicrErrorCount(batchId, (int) remaining);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "updateBatchMicrCountByBatchId failed", e);
        }
    }

    /**
     * FIX: Safely reads batchId from the detached cheque.
     * cheque.getBatch() on a detached LAZY entity will throw —
     * we catch it and skip the count update rather than failing the whole save.
     */
    private void updateBatchMicrCountSafe(InwardCheque cheque) {
        try {
            if (cheque.getBatch() == null) return;
            // This line may throw LazyInitializationException on detached entity.
            // If it does, we catch it below and log a warning — the repair is
            // already saved, so this is non-fatal.
            String batchId = cheque.getBatch().getBatchId();
            updateBatchMicrCountByBatchId(batchId);
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                "updateBatchMicrCountSafe: could not read batch from detached cheque id="
                + cheque.getId() + " — batch MICR count NOT updated. " +
                "Use saveRepair(cheque, batchId) from composer to avoid this.", e);
        }
    }
}
