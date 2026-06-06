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
 */
public interface InwardChequeDao {

    // ── Write ─────────────────────────────────────────────────────────────

    /** Persist a single new cheque record. */
    void save(InwardCheque cheque);

    /** Persist a list of new cheque records (batch insert). */
    void saveAll(List<InwardCheque> cheques);

    /** Merge an existing (detached) cheque record. */
    void update(InwardCheque cheque);

    // ── Read ──────────────────────────────────────────────────────────────

    /** Fetch a single cheque by its primary key. */
    InwardCheque findById(Long id);

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