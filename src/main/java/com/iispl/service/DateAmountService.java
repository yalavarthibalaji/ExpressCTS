package com.iispl.service;

import com.iispl.entity.inward.InwardCheque;

import java.util.List;

/**
 * DateAmountRepairService
 *
 * Business service interface for Step 2 (Date & Amount Repair)
 * and Step 3 (Payee Name & Account Entry).
 */
public interface DateAmountService {

    /**
     * Return all cheques belonging to the given batch.
     * Never returns null — returns empty list on error.
     */
    List<InwardCheque> getChequesByBatchId(String batchId);

    /**
     * Return a single cheque by chequeId, or null if not found.
     */
    InwardCheque getChequeById(String chequeId);

    /**
     * Save corrected date and amount (Step 2).
     * Returns true if exactly one row was updated.
     */
    boolean saveRepairDateAndAmount(InwardCheque cheque);

    /**
     * Save payee name and account number (Step 3).
     * Returns true if exactly one row was updated.
     */
    boolean savePayeeAndAccount(InwardCheque cheque);

    /**
     * Submit all cheques in the batch to the Checker queue.
     * Returns number of rows updated.
     */
    int submitBatchToChecker(String batchId);
}