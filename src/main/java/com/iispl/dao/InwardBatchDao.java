package com.iispl.dao;

import com.iispl.entity.inward.InwardBatch;
import java.util.List;

public interface InwardBatchDao {
    void save(InwardBatch batch);
    List<InwardBatch> findAll();
    List<InwardBatch> findInwardBatchesByStatus(String status);
}