package com.iispl.dao;

import com.iispl.entity.inward.InwardCheque;
import java.util.List;

/**
 * DateAmountDao
 *
 * DAO interface for Step 2 (Date & Amount Repair) and
 * Step 3 (Payee Name & Account Entry) operations.
 */
public interface DateAmountDao {

    // ── Step 2 & 3: Find cheques ─────────────────────────────────────────

    /** Find all cheques for a given batch, ordered by chequeNo ASC. */
    List<InwardCheque> findChequesByBatchId(String batchId);

    /** Find a single cheque by its PK. */
    InwardCheque findChequeById(Long chequeId);

    /**
     * Find cheques by batch and optional repairStatus filter.
     * Pass null or empty string for repairStatus to get all.
     */
    List<InwardCheque> findChequesByBatchAndStatus(String batchId, String repairStatus);

    // ── Step 2: Date & Amount updates ────────────────────────────────────

    /**
     * Update chequeDateOcr, amountOcr, repairStatus → 'DATE_AMT_REPAIRED'
     * and updatedAt for a single cheque.
     *
     * @return rows affected (expect 1 on success)
     */
    int updateDateAndAmount(InwardCheque cheque);

    /**
     * Reject a cheque: sets repairStatus = 'REJECTED', status = 'RETURNED',
     * remarks = rejectReason + " | " + remarks.
     *
     * @return rows affected
     */
    int rejectCheque(Long chequeId, String rejectReason, String remarks);

    /**
     * Refer a cheque back: sets repairStatus = 'REFERRED_BACK',
     * remarks = referReason + " | " + remarks.
     *
     * @return rows affected
     */
    int referChequeBack(Long chequeId, String referReason, String remarks);

    // ── Step 3: Payee & Account updates ──────────────────────────────────

    /**
     * Update payeeName, draweeAccountNumber, repairStatus → 'ENTRY_DONE'
     * and updatedAt for a single cheque.
     *
     * @return rows affected
     */
    int updatePayeeAndAccount(InwardCheque cheque);

    // ── Submit batch ──────────────────────────────────────────────────────

    /**
     * Mark all cheques in the batch as submitted to checker.
     * Sets repairStatus = 'SUBMITTED_TO_CHECKER', status = 'SUBMITTED'.
     *
     * @return rows affected
     */
    int submitBatchToChecker(String batchId);
}