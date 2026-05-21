package com.iispl.dao;

import com.iispl.entity.OutwardBatch;

public interface OutwardBatchDao {

    // save a new batch to outward_batches table
    void saveBatch(OutwardBatch batch);

    // find batch by batch_id string
    OutwardBatch findByBatchId(String batchId);

    // get the count of batches created today for a branch
    // used for generating batch sequence number
    int countBatchesTodayForBranch(String branchCode, String date);

}