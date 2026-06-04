package com.iispl.dao;

import com.iispl.entity.outward.OutwardBatch;
import java.util.List;

/**
 * File    : com/iispl/dao/OutwardBatchDao.java
 * Purpose : Database operations for outward_batch table.
 */
public interface OutwardBatchDao {

    OutwardBatch save(OutwardBatch batch);

    OutwardBatch findByBatchId(String batchId);

    /**
     * Find all batches created by a specific maker.
     * Used in: Batch Upload screen, View Batches screen (MAKER role).
     */
    List<OutwardBatch> findByCreatedBy(Long makerId);

    boolean updateStatus(Long batchDbId, String newStatus);

    boolean existsByBatchId(String batchId);

    /**
     * Check if a batch with the same file path was already uploaded
     * by the same maker and is not REJECTED.
     */
    boolean existsByFilePathAndMaker(String filePath, Long makerId);

    int countBatchesToday(String datePrefix);

    List<OutwardBatch> findNeedsRepairByMaker(Long makerId);

    List<OutwardBatch> findEntryReadyByMaker(Long makerId);

    /**
     * Find all batches across all makers.
     * Used on View Batches screen for ADMIN role.
     */
    List<OutwardBatch> findAll();
}