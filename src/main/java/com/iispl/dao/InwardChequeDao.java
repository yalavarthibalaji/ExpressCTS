package com.iispl.dao;

import com.iispl.entity.inward.InwardCheque;

import java.util.List;

/**
 * DAO interface for InwardCheque entity.
 *
 * All batch-related lookups use the numeric batch PK (Long batchId)
 * which maps to InwardBatch.id in the database.
 *
 * The string batchId used at the service/UI layer is translated
 * to the numeric PK inside the service before calling the DAO.
 *
 * Feature-specific DAOs (RejectRepairDao, DateAmountDao) have been
 * removed — all InwardCheque reads and writes live here.
 */
public interface InwardChequeDao {

    // ── Write: Persist ────────────────────────────────────────────────────

    /** Persist a single new cheque record. */
    void save(InwardCheque cheque);

    /** Persist a list of new cheque records (batch insert). */
    void saveAll(List<InwardCheque> cheques);

    /** Merge an existing (detached) cheque record. */
    void update(InwardCheque cheque);

    // ── Write: Step 1 — MICR Repair ───────────────────────────────────────

    /**
     * Save corrected MICR fields on a cheque.
     * The caller sets the relevant fields and repairStatus on the entity
     * before passing it in; this method calls session.merge().
     */
    void updateCheque(InwardCheque cheque);

    // ── Write: Step 2 — Date & Amount Repair ──────────────────────────────

    /**
     * Persist corrected chequeDateOcr, amountOcr, repairStatus, and remarks.
     *
     * @return number of rows updated (expected: 1)
     */
    int updateDateAndAmount(InwardCheque cheque);

    /**
     * Mark a cheque as rejected.
     * Sets repairStatus = 'REJECTED', status = 'RETURNED'.
     *
     * @param chequeId     PK of the cheque
     * @param rejectReason primary reject reason code / label
     * @param remarks      optional free-text remarks (may be null)
     * @return number of rows updated (expected: 1)
     */
    int rejectCheque(Long chequeId, String rejectReason, String remarks);

    /**
     * Refer a cheque back for clarification.
     * Sets repairStatus = 'REFERRED_BACK'.
     *
     * @param chequeId    PK of the cheque
     * @param referReason primary refer-back reason code / label
     * @param remarks     optional free-text remarks (may be null)
     * @return number of rows updated (expected: 1)
     */
    int referChequeBack(Long chequeId, String referReason, String remarks);

    // ── Write: Step 3 — Payee Name & Account Entry ────────────────────────

    /**
     * Persist payeeName and draweeAccountNumber.
     * Sets repairStatus = 'ENTRY_DONE'.
     *
     * @return number of rows updated (expected: 1)
     */
    int updatePayeeAndAccount(InwardCheque cheque);

    // ── Write: Batch Submission ────────────────────────────────────────────

    /**
     * Mark all cheques in a batch as submitted to the checker.
     * Sets repairStatus = 'SUBMITTED_TO_CHECKER', status = 'SUBMITTED'.
     *
     * @param batchId  string batchId on InwardBatch (e.g. "BATCH-20250601-001")
     * @return number of rows updated
     */
    int submitBatchToChecker(String batchId);

    // ── Read ──────────────────────────────────────────────────────────────

    /** Fetch a single cheque by its primary key. */
    InwardCheque findById(Long id);
    
    int resetSendBackCheques(String batchId);

    /**
     * Fetch all cheques belonging to a batch, ordered by seqNo ASC.
     *
     * @param batchId  numeric PK of the parent InwardBatch row
     */
    List<InwardCheque> findByBatchId(Long batchId);

    /**
     * Fetch cheques filtered by repair status within a batch.
     *
     * @param batchId      numeric PK of the parent InwardBatch row
     * @param repairStatus e.g. "NEEDS_REPAIR", "REPAIRED", "REFERRED_BACK"
     */
    List<InwardCheque> findByBatchIdAndRepairStatus(Long batchId, String repairStatus);

    /**
     * Count cheques in a batch that still need repair.
     * Used by the service to decide whether step 2 can be unlocked.
     *
     * @param batchId  numeric PK of the parent InwardBatch row
     */
    long countPendingRepairsByBatchId(Long batchId);
}