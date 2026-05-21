package com.iispl.service;

import com.iispl.entity.CheckerBatch;
import com.iispl.entity.CheckerCheque;
import java.util.List;

/**
 * CheckerService.java
 * Service Interface — Checker module.
 * Package : com.iispl.service
 */
public interface CheckerService {

    /** All batches in checker queue. */
    List<CheckerBatch> getAllCheckerBatches();

    /** All cheques for a batch. */
    List<CheckerCheque> getChequesByBatchId(String batchId);

    /** Single cheque by ID. */
    CheckerCheque getChequeById(String chequeId);

    /** Spot-check pass — mark cheque approved. */
    void approveCheque(String chequeId, String remarks);

    /** Flag cheque as exception. Remarks required. */
    void rejectCheque(String chequeId, String remarks);

    /** Approve entire batch. At least 1 cheque must be spot-checked. */
    void approveBatch(String batchId);

    /** Return batch to maker. Remarks required. */
    void returnBatchToMaker(String batchId, String remarks);

    // ── MICR Repair methods ──────────────────────────────────────────

    /**
     * Returns all IQA-failed cheques for the MICR Repair queue.
     * These are cheques where iqa_status = 'FAIL'.
     */
    List<CheckerCheque> getMicrRepairCheques();

    /**
     * Maker submits corrected MICR data for a cheque.
     * Sets checker_status = 'repaired' so checker can verify it.
     *
     * @param chequeId   the cheque being repaired
     * @param micrCode   corrected MICR code
     * @param chequeNum  corrected cheque number
     * @param bankName   corrected bank name
     * @param ifscCode   corrected IFSC code
     * @param remarks    reason for correction (required)
     */
    void submitMicrRepair(String chequeId, String micrCode, String chequeNum,
                          String bankName, String ifscCode, String remarks);

    /**
     * Checker rejects a MICR repair — resets status back to 'pending'
     * so maker must repair again.
     *
     * @param chequeId the cheque to reset
     * @param remarks  reason for rejection (required)
     */
    void resetMicrChequeStatus(String chequeId, String remarks);
}