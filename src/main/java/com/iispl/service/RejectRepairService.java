package com.iispl.service;


import com.iispl.entity.inward.InwardBatch;
import java.util.List;

/**
 * RejectRepairService
 *
 * Service interface for the Reject & Repair (Step 1) page.
 * Fetches batches that are eligible for MICR repair.
 */
public interface RejectRepairService {

    /**
     * Returns all inward batches eligible for reject & repair.
     * Eligible = status RECEIVED or PARSED, and micrErrorCount > 0.
     *
     * @return non-null list; empty if none found
     */
    List<InwardBatch> getRepairEligibleBatches();

    /**
     * Returns a single batch by its batchId.
     *
     * @param batchId  the batch identifier
     * @return InwardBatch or null if not found
     */
    InwardBatch getBatchById(String batchId);
}