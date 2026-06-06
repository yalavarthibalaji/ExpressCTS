package com.iispl.dao;

import com.iispl.entity.inward.InwardBatch;
import java.util.List;

public interface InwardBatchDao {

    /** All batches pending checker action (status RECEIVED or PENDING_CHECKER). */
    List<InwardBatch> findPendingCheckerBatches();

    /** Find a single batch by its business batch_id string. */
    InwardBatch findByBatchId(String batchId);

    /** Count of all inward batches (any status). */
    int countAllBatches();

    /** Count of inward batches that have been cleared/accepted by checker. */
    int countClearedBatches();
}