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

    // ── Checker Outward ──────────────────────────────────────────────────────

    /**
     * Find all batches currently visible in the Checker's queue.
     *
     * Statuses included:
     *   SUBMITTED          — Maker submitted, waiting for Checker to start.
     *   CHECKER_IN_PROGRESS — Checker has opened the batch and is working on it.
     *   CHECKER_HOLD       — One or more cheques referred back to Maker;
     *                        batch returns here after Maker re-submits.
     *
     * Used in: CheckerQueue page (batch list table),
     *          CheckerServiceImpl.getCheckerQueueBatches().
     */
    List<OutwardBatch> findCheckerQueueBatches();

    /**
     * Find all batches that have been fully verified by the Checker.
     *
     * Status: CHECKER_APPROVED
     * These batches are ready for DEM Export (CXF/CIGF file generation).
     *
     * Used in: DEM Export page (future step).
     */
    List<OutwardBatch> findCheckerApprovedBatches();

    /**
     * Count batches that match a given status string.
     *
     * Used in: CheckerOutward Dashboard summary labels
     *          (Pending Queue, On Hold, Ready to Export counts).
     *
     * @param status  Exact status string, e.g. "SUBMITTED", "CHECKER_HOLD"
     * @return        Number of batches with that status
     */
    int countByStatus(String status);
}