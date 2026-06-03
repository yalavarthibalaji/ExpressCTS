package com.iispl.serviceImpl;


import com.iispl.dao.RejectRepairDao;
import com.iispl.daoImpl.RejectRepairDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.service.RejectRepairService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RejectRepairServiceImpl
 *
 * Business layer for Reject & Repair Step 1.
 * Delegates to DAO, guarantees non-null returns, handles exceptions.
 */
public class RejectRepairServiceImpl implements RejectRepairService {

    private static final Logger LOG =
            Logger.getLogger(RejectRepairServiceImpl.class.getName());

    private final RejectRepairDao rejectRepairDao;

    // ── Constructors ──────────────────────────────────────────────────────

    /** Default — creates its own DAO (non-Spring). */
    public RejectRepairServiceImpl() {
        this.rejectRepairDao = new RejectRepairDaoImpl();
    }

    /** Spring / test constructor. */
    public RejectRepairServiceImpl(RejectRepairDao rejectRepairDao) {
        this.rejectRepairDao = rejectRepairDao;
    }

    // ── Interface implementation ──────────────────────────────────────────

    @Override
    public List<InwardBatch> getRepairEligibleBatches() {
        try {
            List<InwardBatch> list = rejectRepairDao.findRepairEligibleBatches();
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching repair-eligible batches", e);
            return new ArrayList<>();   // safe empty — UI shows empty state
        }
    }

    @Override
    public InwardBatch getBatchById(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            LOG.warning("getBatchById called with blank batchId");
            return null;
        }
        try {
            return rejectRepairDao.findBatchById(batchId.trim());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching batch by id: " + batchId, e);
            return null;
        }
    }
}