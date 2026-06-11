package com.iispl.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheckerAction;
import com.iispl.entity.outward.OutwardCheque;

/**
 * File    : com/iispl/dao/ReportsDao.java
 * Purpose : DAO interface for all report queries — Maker and Checker.
 */
public interface ReportsDao {

    // ════════════════════════════════════════════════════════════════
    //  MAKER REPORTS  (existing — do not modify)
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns all batches created by a specific maker
     * between the given from and to dates (inclusive).
     */
    List<OutwardBatch> getMyBatchesReport(Long makerId,
                                           LocalDate fromDate,
                                           LocalDate toDate);

    /**
     * Returns all cheques belonging to a specific batch,
     * ordered by seq_no ASC. Used for per-batch cheque detail PDF.
     *
     * @param batchDbId  DB primary key of the outward_batch row
     * @return list of OutwardCheque ordered by seq_no
     */
    List<OutwardCheque> getChequesByBatch(Long batchDbId);

    // ════════════════════════════════════════════════════════════════
    //  CHECKER REPORTS  (new)
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns all batches verified by a specific checker.
     *
     * Fetches batches where:
     *   verified_by = checkerId
     *   AND status IN ('CHECKER_APPROVED', 'EXPORTED')
     *
     * Ordered by verified_at DESC (most recently verified first).
     *
     * Used by: CheckerReportsComposer → Tab 1 (Verified Batches table).
     *
     * @param checkerId  DB primary key of the logged-in checker user
     * @return list of OutwardBatch, never null (empty list if none)
     */
    List<OutwardBatch> getVerifiedBatches(Long checkerId);

    /**
     * Returns all REJECTED and REFERRED checker actions taken by a
     * specific checker, across all batches.
     *
     * Fetches rows from outward_checker_actions where:
     *   checker_id = checkerId
     *   AND action IN ('REJECTED', 'REFERRED')
     *
     * Ordered by actioned_at DESC (most recent first).
     *
     * Used by: CheckerReportsComposer → Tab 2 (Cheque Action Log table).
     *
     * @param checkerId  DB primary key of the logged-in checker user
     * @return list of OutwardCheckerAction, never null (empty list if none)
     */
    List<OutwardCheckerAction> getCheckerActionLog(Long checkerId);
    
    /**
     * Returns DB IDs of all batches (created by this maker)
     * that contain at least one maker-rejected cheque.
     * Used by Reports screen to make the "Rejected" status filter work.
     */
    Set<Long> findBatchIdsWithRejections(Long makerId);
    
    /**
     * Returns all cheques rejected by this maker across all their batches.
     * Used to populate Tab 2 (Rejected Cheques) in the Maker Reports screen.
     * Ordered by rejected_at DESC — most recently rejected shown first.
     */
    List<OutwardCheque> getMakerRejectedCheques(Long makerId);
}