package com.iispl.service;

import com.iispl.entity.inward.InwardCheque;
import java.util.List;

/**
 * DateAmountService
 *
 * Business-layer contract for:
 *   - Step 2 : Date & Amount Repair
 *   - Step 3 : Payee Name & Account Entry
 *   - Batch submission to Checker
 */
public interface DateAmountService {

    // ── Step 2 & 3: Queries ───────────────────────────────────────────────

    /**
     * Fetch all cheques for the given batch (used in both Step 2 and Step 3 list views).
     * Returns empty list if batchId is blank or DB error occurs.
     */
    List<InwardCheque> getChequesByBatchId(String batchId);

    /**
     * Fetch a single cheque by its string ID (converts to Long internally).
     * Returns null if not found or invalid ID.
     */
    InwardCheque getChequeById(String chequeId);

    // ── Step 2: Date & Amount Repair ─────────────────────────────────────

    /**
     * Persist corrected OCR date and amount for a cheque.
     * Sets repairStatus to the value already set on the entity
     * (typically "REPAIRED" for accept, "REJECTED" for reject,
     *  "REFERRED_BACK" for refer).
     *
     * @return true if exactly one row was updated
     */
    boolean saveRepairDateAndAmount(InwardCheque cheque);

    /**
     * Reject a cheque with a reason code and optional remarks.
     * Sets repairStatus = 'REJECTED', status = 'RETURNED'.
     *
     * @return true if exactly one row was updated
     */
    boolean rejectCheque(Long chequeId, String rejectReason, String remarks);

    /**
     * Refer a cheque back with a reason and optional remarks.
     * Sets repairStatus = 'REFERRED_BACK'.
     *
     * @return true if exactly one row was updated
     */
    boolean referChequeBack(Long chequeId, String referReason, String remarks);

    // ── Step 3: Payee Name & Account Entry ───────────────────────────────

    /**
     * Persist payee name and account number for a cheque.
     * Sets repairStatus = 'ENTRY_DONE'.
     *
     * @return true if exactly one row was updated
     */
    boolean savePayeeAndAccount(InwardCheque cheque);

    // ── Batch submission ──────────────────────────────────────────────────

    /**
     * Submit all cheques in a batch to the Checker queue.
     * Sets repairStatus = 'SUBMITTED_TO_CHECKER', status = 'SUBMITTED'
     * on every cheque in the batch.
     *
     * @return count of rows updated (0 on error or blank batchId)
     */
    int submitBatchToChecker(String batchId);
}