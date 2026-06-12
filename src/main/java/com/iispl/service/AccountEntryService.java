package com.iispl.service;

import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import java.math.BigDecimal;
import java.util.List;

/**
 * File    : com/iispl/service/AccountEntryService.java
 * Purpose : Business logic for the Account and Amount Entry screen.
 *
 * Workflow:
 *   1. Maker opens screen with ?batchId OR selects from batch list
 *   2. For each PENDING cheque:
 *      - Pre-filled fields shown from XML parse data
 *      - Maker verifies account no (CBS mock), amount, date, payee
 *   3. saveEntry() saves and marks cheque ENTRY_DONE
 *   4. When all cheques done → submitBatch() sets batch to SUBMITTED
 */
public interface AccountEntryService {

    /** Load batch by batch_id string. */
    OutwardBatch getBatch(String batchId);

    /**
     * Returns batches with status = ENTRY_DONE for this maker.
     * These are ready for account entry (MICR repair is done).
     */
    List<OutwardBatch> getEntryBatches(Long makerId);

    /**
     * Returns all cheques in batch that are PENDING entry.
     * Excludes REJECTED and already ENTRY_DONE cheques.
     */
    List<OutwardCheque> getPendingCheques(Long batchDbId);

    /**
     * Bug-4 FIX: returns pending counts for a list of batches in ONE
     * DB round-trip. Used by the batch selection grid to avoid the
     * N+1 query that called getPendingCheques() per row.
     */
    java.util.Map<Long, Integer> getPendingCountsForBatches(java.util.List<Long> batchDbIds);

    /**
     * Saves account entry data for one cheque.
     * Sets cheque status = ENTRY_DONE.
     *
     * @param chequeId      DB id of the cheque
     * @param accountNo     account number entered by maker
     * @param accountHolder account holder name (CBS mock = "Validated")
     * @param amount        cheque amount (verified/edited by maker)
     * @param amountInWords amount in words (auto-filled or edited)
     * @param chequeDate    cheque date string "YYYY-MM-DD"
     * @param payeeName     payee name verified by maker
     * @param makerId       logged-in maker's user ID
     */
    boolean saveEntry(Long       chequeId,
                      String     accountNo,
                      String     accountHolder,
                      BigDecimal amount,
                      String     amountInWords,
                      String     chequeDate,
                      String     payeeName,
                      Long       makerId);

    /**
     * Rejects a cheque during account entry.
     * Same logic as MICR Repair reject.
     */
    boolean rejectCheque(Long   chequeId,
                          String reasonCode,
                          String remarks,
                          Long   makerId);

    /**
     * Returns true when no more cheques are pending entry in this batch.
     */
    boolean isAllEntriesDone(Long batchDbId);

    /**
     * Maker submits a batch to the Checker queue.
     * Sets status='SUBMITTED' AND records submitted_at + submitted_by
     * (was a lifecycle gap in the prior implementation).
     *
     * @param batchDbId outward_batch primary key
     * @param makerId   the user submitting the batch (for audit)
     */
    boolean submitBatch(Long batchDbId, Long makerId);
    
}