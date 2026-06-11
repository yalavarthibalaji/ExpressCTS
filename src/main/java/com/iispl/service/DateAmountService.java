package com.iispl.service;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import java.util.List;

/**
 * DateAmountService
 *
 * Business-layer contract for Step 2 — Date & Amount Repair only.
 * Batch resolution methods are shared with RejectRepairService
 * but kept here so DateAmountComposer has no dependency on
 * RejectRepairService.
 */
public interface DateAmountService {

    // ── Batch Resolution ──────────────────────────────────────────────────

    /** Resolve a string batchId to its InwardBatch entity. */
    InwardBatch getBatchById(String batchId);

    /** Fetch all batches eligible for repair (used as last-resort fallback). */
    List<InwardBatch> getRepairEligibleBatches();

    // ── Queries ───────────────────────────────────────────────────────────

    /** Fetch all cheques for a batch by numeric batch PK. */
    List<InwardCheque> getStep2ChequesByBatchId(Long batchId);

    // ── Step 2: Date & Amount Repair ──────────────────────────────────────

    /** Persist corrected date and amount. Sets repairStatus = REPAIRED. */
    void saveStep2Repair(InwardCheque cheque);

    /** Reject a cheque. Sets repairStatus = REJECTED. */
    void rejectStep2(InwardCheque cheque, String rejectReason);
    
    InwardCheque getChequeById(Long chequeId);
    
    /** Persist corrected OCR date and amount. Returns true if 1 row updated. */
    boolean saveRepairDateAndAmount(InwardCheque cheque);
    /**
     * Refer a cheque back from Step 2.
     * Sets repairStatus = REFERRED_DATEAMOUNT.
     */
    void referStep2(InwardCheque cheque, String referReason);
}