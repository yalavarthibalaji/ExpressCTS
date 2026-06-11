package com.iispl.serviceImpl;

import com.iispl.dao.InwardBatchDao;
import com.iispl.daoImpl.InwardBatchDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.service.InwardCheckerService;

import java.util.List;

public class InwardCheckerServiceImpl implements InwardCheckerService {

    private final InwardBatchDao inwardBatchDao = new InwardBatchDaoImpl();

    @Override
    public List<InwardBatch> getPendingBatches() {
        return inwardBatchDao.findPendingCheckerBatches();
    }

    @Override
    public InwardBatch getBatchById(String batchId) {
        return inwardBatchDao.findByBatchId(batchId);
    }

    @Override
    public int getTotalBatchCount() {
        return inwardBatchDao.countAllBatches();
    }

    @Override
    public int getClearedBatchCount() {
        return inwardBatchDao.countClearedBatches();
    }
    
 // In InwardCheckerServiceImpl
    public List<InwardBatch> getAllBatchesForChecker() {
        // Returns all batches with status in MakerVerified, Verified, CBS_Processed
        return inwardBatchDao.findBatchesByStatuses(
        		List.of("MakerVerified", "CheckerReferred", "Verified", "CBS_Processed")
        );
    }
}