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

    private final RejectRepairDao rejectRepairDao;

    public RejectRepairServiceImpl() {
        this.rejectRepairDao = new RejectRepairDaoImpl();
    }

    public RejectRepairServiceImpl(RejectRepairDao rejectRepairDao) {
        this.rejectRepairDao = rejectRepairDao;
    }

    // ── Batch ─────────────────────────────────────────────────────────────

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
        if (batchId == null || batchId.trim().isEmpty()) return null;
        try {
            return rejectRepairDao.findBatchById(batchId.trim());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching batch: " + batchId, e);
            return null;
        }
    }

    // ── Step 1 ────────────────────────────────────────────────────────────

    @Override
    public List<InwardCheque> getChequesByBatchId(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) return new ArrayList<>();
        try {
            List<InwardCheque> list =
                    rejectRepairDao.findChequesByBatchId(batchId.trim());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getChequesByBatchId failed: " + batchId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveRepair(InwardCheque cheque) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus("REPAIRED");
            cheque.setMicrError(false);
            rejectRepairDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "saveRepair failed, id=" + cheque.getId(), e);
            throw new RuntimeException("Save repair failed", e);
        }
    }

    @Override
    public void referBack(Long chequeId, String remarks) {
        // Load cheque, mark REFERRED_BACK, persist
        // Full implementation depends on your session scope
        LOG.info("referBack called for chequeId=" + chequeId);
    }

    // ── Step 2 ────────────────────────────────────────────────────────────

    @Override
    public List<InwardCheque> getStep2ChequesByBatchId(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) return new ArrayList<>();
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
            rejectRepairDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "saveStep2Repair failed", e);
            throw new RuntimeException("Step2 save failed", e);
        }
    }

    // ── Step 3 ────────────────────────────────────────────────────────────

    @Override
    public List<InwardCheque> getStep3ChequesByBatchId(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) return new ArrayList<>();
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
            cheque.setRepairStatus("ENTRY_DONE");
            rejectRepairDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "saveStep3Entry failed", e);
            throw new RuntimeException("Step3 save failed", e);
        }
    }

    @Override
    public void proceedToInwardChecker(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) return;
        try {
            rejectRepairDao.updateBatchStatus(
                    batchId.trim(), "SUBMITTED_TO_CHECKER", "REPAIR_COMPLETE");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "proceedToInwardChecker failed", e);
            throw new RuntimeException("Proceed to checker failed", e);
        }
    }
}