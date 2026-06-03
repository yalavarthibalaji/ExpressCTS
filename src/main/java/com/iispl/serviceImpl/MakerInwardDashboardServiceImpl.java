package com.iispl.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iispl.dao.MakerInwardDashboardDao;
import com.iispl.daoImpl.MakerInwardDashboardDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.service.MakerInwardDashboardService;



/**
 * MakerInwardDashboardServiceImpl
 *
 * Business-logic layer between the ZK Composer and the DAO.
 * Validates inputs, handles exceptions, and applies any
 * Maker-role business rules before returning data.
 */
public class MakerInwardDashboardServiceImpl implements MakerInwardDashboardService {

    private static final Logger LOG =
            Logger.getLogger(MakerInwardDashboardServiceImpl.class.getName());

    // Inject via Spring @Autowired, or construct directly
    private final MakerInwardDashboardDao dashboardDao;

    // ── Constructors ──────────────────────────────────────────────────────

    /** Default constructor – creates its own DAO (non-Spring projects). */
    public MakerInwardDashboardServiceImpl() {
        this.dashboardDao = new MakerInwardDashboardDaoImpl();
    }

    /** Spring / test constructor – DAO injected externally. */
    public MakerInwardDashboardServiceImpl(MakerInwardDashboardDao dashboardDao) {
        this.dashboardDao = dashboardDao;
    }

    // ── Interface implementation ──────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * Delegates to DAO and guarantees a non-null list is returned.
     * Any DAO-level exception is caught here so the composer always
     * receives a safe result.
     */
    @Override
    public List<InwardBatch> getInwardBatches() {
        try {
            List<InwardBatch> batches = dashboardDao.findAllInwardBatches();
            return batches != null ? batches : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching inward batches", e);
            return new ArrayList<>();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Validates the status string before hitting the DAO.
     */
    @Override
    public List<InwardBatch> getInwardBatchesByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            LOG.warning("getInwardBatchesByStatus called with blank status – returning all");
            return getInwardBatches();
        }
        try {
            List<InwardBatch> batches =
                    dashboardDao.findInwardBatchesByStatus(status.trim().toUpperCase());
            return batches != null ? batches : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching inward batches by status: " + status, e);
            return new ArrayList<>();
        }
    }
}