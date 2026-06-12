package com.iispl.dao;

import com.iispl.entity.inward.InwardBatch;
import java.util.List;

public interface InwardBatchDao {

    void save(InwardBatch batch);

    List<InwardBatch> findAll();

    List<InwardBatch> findInwardBatchesByStatus(String status);

    /** All batches pending checker action. */
    List<InwardBatch> findPendingCheckerBatches();

    /**
     * Batches eligible for MICR repair (status RECEIVED or PARSED,
     * micrErrorCount > 0).
     */
    List<InwardBatch> findRepairEligibleBatches();

    /** Find a single batch by its business batch_id string. */
    InwardBatch findByBatchId(String batchId);

    /**
     * Update batch status and repairStatus in one shot.
     * Used by RejectRepairServiceImpl when advancing workflow steps.
     */
    void updateBatchStatus(String batchId, String status, String repairStatus);

    /**
     * Persist the updated MICR error count after Step 1 repair.
     */
    void updateBatchMicrErrorCount(String batchId, int count);

    /** Count of all inward batches (any status). */
    int countAllBatches();

    /** Count of inward batches cleared/accepted by checker. */
    int countClearedBatches();
    
    int countSendBackCheques(String batchId);
    
    List<InwardBatch> findBatchesByStatuses(List<String> statuses);
}