package com.iispl.dao;

import com.iispl.entity.CheckerBatch;
import com.iispl.entity.CheckerCheque;
import java.util.List;

/**
 * CheckerDao.java
 * DAO Interface — Checker module database operations.
 * Package : com.iispl.dao
 */
public interface CheckerDao {

    /** All batches in CHECKER_PENDING or APPROVED state. */
    List<CheckerBatch> getAllCheckerBatches();

    /** All cheques for a batch. */
    List<CheckerCheque> getChequesByBatchId(String batchId);

    /** Single cheque by ID. */
    CheckerCheque getChequeById(String chequeId);

    /** Save checker spot-check decision (approved / rejected). */
    void updateChequeCheckerStatus(String chequeId, String status, String remarks);

    /** Approve entire batch — sets status = APPROVED. */
    void approveBatch(String batchId);

    /** Return batch to maker — resets status and all cheque statuses. */
    void returnBatchToMaker(String batchId, String remarks);

    // ── MICR Repair ──────────────────────────────────────────────────

    /**
     * Returns all cheques where iqa_status = 'FAIL'.
     * These appear in the MICR Repair queue.
     */
    List<CheckerCheque> getMicrRepairCheques();

    /**
     * Saves corrected MICR data and marks checker_status = 'repaired'
     * so the checker can verify the correction.
     */
    void submitMicrRepair(String chequeId, String micrCode, String chequeNum,
                          String bankName, String ifscCode, String remarks);

    /**
     * Resets a cheque checker_status back to 'pending'
     * when checker rejects a MICR repair — maker must fix again.
     */
    void resetMicrChequeStatus(String chequeId, String remarks);
}