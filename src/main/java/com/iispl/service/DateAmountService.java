package com.iispl.service;

import com.iispl.entity.inward.InwardCheque;
import java.util.List;

/**
 * DateAmountService
 *
 * Business-layer contract for:
 *   Step 2 — Date & Amount Repair  (save / reject / referBack)
 *   Step 3 — Payee Name & Account Entry
 *   Batch submission to Checker
 */
public interface DateAmountService {

    // ── Queries ───────────────────────────────────────────────────────────

    /** Fetch all cheques for a batch by numeric batch PK. */
    List<InwardCheque> getChequesByBatchId(Long batchId);

    /** Fetch cheques filtered by repairStatus within a batch. */
    List<InwardCheque> getChequesByBatchIdAndStatus(Long batchId, String repairStatus);

    /** Fetch a single cheque by its numeric PK. */
    InwardCheque getChequeById(Long chequeId);

    /** Count cheques still pending repair (DB query, no list load). */
    long countPendingRepairs(Long batchId);

    /** Count Step-3 cheques still awaiting payee/account entry (stream over loaded list). */
    long countPendingStep3(List<InwardCheque> cheques);

    // ── Step 2: Date & Amount Repair ─────────────────────────────────────

    /** Persist corrected OCR date and amount. Returns true if 1 row updated. */
    boolean saveRepairDateAndAmount(InwardCheque cheque);

    /** Reject a cheque. Sets repairStatus = REJECTED, status = RETURNED. */
    boolean rejectCheque(Long chequeId, String rejectReason, String remarks);

    /** Refer a cheque back. Sets repairStatus = REFERRED_BACK. */
    boolean referChequeBack(Long chequeId, String referReason, String remarks);

    // ── Step 3: Payee Name & Account Entry ───────────────────────────────

    /** Persist payee name and account number. Sets repairStatus = ENTRY_DONE. */
    boolean savePayeeAndAccount(InwardCheque cheque);

    // ── Batch Submission ──────────────────────────────────────────────────

    /**
     * Mark all cheques in a batch as submitted to checker.
     * Takes the string batchId (not numeric PK) — the DAO handles the
     * WHERE clause directly on the batch association.
     *
     * @return count of cheque rows updated
     */
    int submitBatchToChecker(String batchId);
}