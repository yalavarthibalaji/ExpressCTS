package com.iispl.serviceImpl;

import com.iispl.dao.InwardBatchDao;
import com.iispl.daoImpl.InwardBatchDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.service.InwardBatchService;

import java.util.List;

public class InwardBatchServiceImpl implements InwardBatchService {

    private final InwardBatchDao inwardBatchDao = new InwardBatchDaoImpl();

    @Override
    public List<InwardBatch> getAllBatches() {
        return inwardBatchDao.findAll();
    }
}