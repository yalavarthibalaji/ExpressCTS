package com.iispl.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iispl.dao.InwardBatchDao;
import com.iispl.daoImpl.InwardBatchDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.service.MakerInwardDashboardService;

public class MakerInwardDashboardServiceImpl implements MakerInwardDashboardService {

    private static final Logger LOG =
            Logger.getLogger(MakerInwardDashboardServiceImpl.class.getName());

    private final InwardBatchDao inwardBatchDao;

    /** Default constructor – creates its own DAO (non-Spring projects). */
    public MakerInwardDashboardServiceImpl() {
        this.inwardBatchDao = new InwardBatchDaoImpl();
    }

    /** Spring / test constructor – DAO injected externally. */
    public MakerInwardDashboardServiceImpl(InwardBatchDao inwardBatchDao) {
        this.inwardBatchDao = inwardBatchDao;
    }

    @Override
    public List<InwardBatch> getInwardBatches() {
        try {
            List<InwardBatch> batches = inwardBatchDao.findAll();
            return batches != null ? batches : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching inward batches", e);
            return new ArrayList<>();
        }
    }
    
    

    @Override
    public List<InwardBatch> getInwardBatchesByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            LOG.warning("getInwardBatchesByStatus called with blank status – returning all");
            return getInwardBatches();
        }
        try {
            List<InwardBatch> batches =
                    inwardBatchDao.findInwardBatchesByStatus(status.trim().toUpperCase());
            return batches != null ? batches : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching inward batches by status: " + status, e);
            return new ArrayList<>();
        }
    }
}