package com.iispl.dao;

import com.iispl.entity.OutwardBatch;

public interface OutwardBatchDao {

    void saveBatch(OutwardBatch batch);

    OutwardBatch findByBatchId(String batchId);

    int countBatchesTodayForBranch(String branchCode, String date);

}