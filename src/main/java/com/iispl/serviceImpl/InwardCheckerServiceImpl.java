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
}