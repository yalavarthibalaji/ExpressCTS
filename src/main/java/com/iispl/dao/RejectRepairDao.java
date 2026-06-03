package com.iispl.dao;

import com.iispl.entity.inward.InwardBatch;
import java.util.List;

/**
 * RejectRepairDao
 *
 * DAO interface for Reject & Repair data access.
 */
public interface RejectRepairDao {

    /**
     * Fetches all inward batches eligible for repair:
     *   status IN ('RECEIVED','PARSED') AND micr_error_count > 0
     *
     * @return list; may be null (ServiceImpl handles null-safety)
     */
    List<InwardBatch> findRepairEligibleBatches();

    /**
     * Fetches a single batch by its batch_id.
     *
     * @param batchId  batch identifier
     * @return InwardBatch or null if not found
     */
    InwardBatch findBatchById(String batchId);
}