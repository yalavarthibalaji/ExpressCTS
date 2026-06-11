package com.iispl.serviceImpl;

import com.iispl.dao.InwardBatchDao;
import com.iispl.dao.InwardChequeDao;
import com.iispl.daoImpl.InwardBatchDaoImpl;
import com.iispl.daoImpl.InwardChequeDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.DateAmountService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DateAmountServiceImpl
 *
 * Business layer for Step 2 — Date & Amount Repair only.
 * Logic is copied from RejectRepairServiceImpl's Step 2 section
 * and adjusted:
 *   saveStep2Repair → STATUS_REPAIRED          (was DATE_AMT_REPAIRED)
 *   referStep2      → STATUS_REFERRED_DATEAMOUNT (was REFERRED_MICR)
 */
public class DateAmountServiceImpl implements DateAmountService {

    private static final Logger LOG =
            Logger.getLogger(DateAmountServiceImpl.class.getName());

    // ── Status constants ──────────────────────────────────────────────────
    private static final String STATUS_REPAIRED             = "REPAIRED";
    private static final String STATUS_REJECTED             = "REJECTED";
    private static final String STATUS_REFERRED_DATEAMOUNT  = "REFERRED_DATEAMOUNT";

    // ── DAOs ──────────────────────────────────────────────────────────────
    private final InwardChequeDao chequeDao;
    private final InwardBatchDao  batchDao;

    // ── Constructors ──────────────────────────────────────────────────────

    /** Default — creates its own DAOs (non-Spring). */
    public DateAmountServiceImpl() {
        this.chequeDao = new InwardChequeDaoImpl();
        this.batchDao  = new InwardBatchDaoImpl();
    }

    /** Injectable constructor for testing. */
    public DateAmountServiceImpl(InwardChequeDao chequeDao, InwardBatchDao batchDao) {
        this.chequeDao = chequeDao;
        this.batchDao  = batchDao;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BATCH RESOLUTION
    // ══════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════
    //  QUERIES
    // ══════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════
    //  STEP 2 — DATE & AMOUNT REPAIR
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Persist corrected date and amount.
     * Sets repairStatus = REPAIRED.
     * Same logic as RejectRepairServiceImpl.saveStep2Repair()
     * but uses REPAIRED instead of DATE_AMT_REPAIRED.
     */
    @Override
    public void saveStep2Repair(InwardCheque cheque) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_REPAIRED);
            chequeDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "saveStep2Repair failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Step 2 save failed", e);
        }
    }
    
    @Override
    public boolean saveRepairDateAndAmount(InwardCheque cheque) {
        if (cheque == null || cheque.getId() == null) {
            LOG.warning("saveRepairDateAndAmount: null cheque or id");
            return false;
        }
        try {
            cheque.setRepairStatus(STATUS_REPAIRED);
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
     * Reject a cheque at Step 2.
     * Sets repairStatus = REJECTED.
     * Exact same logic as RejectRepairServiceImpl.rejectStep2().
     */
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

    /**
     * Refer a cheque back from Step 2.
     * Sets repairStatus = REFERRED_DATEAMOUNT.
     * Same logic as RejectRepairServiceImpl.referStep2()
     * but uses REFERRED_DATEAMOUNT instead of REFERRED_MICR.
     */
    @Override
    public void referStep2(InwardCheque cheque, String referReason) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_REFERRED_DATEAMOUNT);
            if (!isBlank(referReason)) cheque.setRemarks(referReason.trim());
            chequeDao.updateCheque(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "referStep2 failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Step 2 refer failed", e);
        }
    }

    @Override
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
    // ─────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}