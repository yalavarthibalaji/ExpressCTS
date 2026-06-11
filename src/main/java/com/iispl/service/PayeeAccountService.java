package com.iispl.service;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;

import java.util.List;

/**
 * PayeeAccountService — Step 3 Payee Name & Account Entry.
 * Deliberately separate from RejectRepairService so Step 3 logic,
 * status constants and DAO calls are fully isolated.
 */
public interface PayeeAccountService {

    // ── Batch ──────────────────────────────────────────────────────────────

    /** Fetch the batch header (used for breadcrumb / badge display). */
    InwardBatch getBatchById(String batchId);

    // ── Cheques ────────────────────────────────────────────────────────────

    /**
     * Return ALL cheques in the batch ordered by seqNo.
     * Step 3 must show every cheque (not only those with errors) so the
     * maker can enter payee + account for each one.
     */
    List<InwardCheque> getChequesByBatchId(String batchId);

    // ── Save / Refer ───────────────────────────────────────────────────────

    /**
     * Persist payee name, drawee account number and optional remarks.
     * Sets repairStatus → ENTRY_DONE.
     */
    void saveEntry(InwardCheque cheque);

    /**
     * Mark the cheque as referred back for re-investigation.
     * Sets repairStatus → REFERRED_BACK.
     */
    void referBack(InwardCheque cheque, String remarks);

    // ── Workflow ───────────────────────────────────────────────────────────

    /**
     * Validate that all cheques in the batch have been processed
     * (ENTRY_DONE or REFERRED_BACK) and advance batch status so the
     * Inward Checker can pick it up.
     *
     * @return true if the batch was advanced successfully.
     */
    boolean proceedToInwardChecker(String batchId);
    
    boolean savePayeeAndAccount(InwardCheque cheque);
    /**
     * Count how many cheques in the given list still need an entry
     * (payeeName or draweeAccountNumber is null / blank AND status
     * is not REFERRED_BACK).
     */
    long countPending(List<InwardCheque> cheques);
}