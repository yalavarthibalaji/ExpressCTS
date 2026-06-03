package com.iispl.service;

import com.iispl.entity.inward.InwardBatch;
import java.util.List;

public interface InwardCheckerService {

    /** Returns all inward batches pending checker verification. */
    List<InwardBatch> getPendingBatches();

    /** Returns a single batch by its batch_id string. */
    InwardBatch getBatchById(String batchId);
}