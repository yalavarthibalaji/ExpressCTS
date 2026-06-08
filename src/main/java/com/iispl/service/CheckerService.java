package com.iispl.service;

import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;

import java.util.List;
import java.util.Map;

/**
 * File    : com/iispl/service/CheckerService.java
 * Purpose : Service interface for all Checker Outward business operations.
 *
 * This interface defines the contract between the UI layer (CheckerQueueComposer,
 * CheckerOutwardDashboardComposer) and the business logic layer (CheckerServiceImpl).
 *
 * Workflow this service covers:
 *
 *   1. Checker opens the Verification Queue → sees all SUBMITTED / IN_PROGRESS / HOLD batches
 *   2. Checker opens a batch → batch status moves to CHECKER_IN_PROGRESS
 *   3. For each cheque in the batch, Checker takes one of three actions:
 *        Pass   → cheque status = CHECKER_PASSED
 *        Reject → cheque status = CHECKER_REJECTED  (with reason code + remarks)
 *        Refer  → cheque status = CHECKER_REFERRED  (with reason code + remarks)
 *                 batch  status = CHECKER_HOLD
 *   4. When all cheques are actioned (no more ENTRY_DONE) →
 *        batch status = CHECKER_APPROVED → moves to DEM Export queue
 *
 * Refer loop (handled by AccountEntryComposer + AccountEntryService):
 *   Maker re-opens referred cheques → corrects data → re-saves → cheque back to ENTRY_DONE
 *   When all referred cheques are re-entered → Maker re-submits → batch back to SUBMITTED
 *   Checker sees the batch again in queue → resumes from where they left off
 *
 * Every action is logged to the outward_checker_actions audit table.
 */
public interface CheckerService {

    // ════════════════════════════════════════════════════════════════════════
    //  Queue Loading
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns all batches currently visible in the Checker's verification queue.
     *
     * Includes batches with the following statuses:
     *   SUBMITTED           — Submitted by Maker, not yet opened by Checker.
     *   CHECKER_IN_PROGRESS — Checker has opened the batch and is working on it.
     *   CHECKER_HOLD        — One or more cheques referred back to Maker;
     *                         batch returns here after Maker re-submits.
     *
     * Ordered by created_at ASC (oldest first — FIFO processing).
     *
     * Used by: CheckerQueueComposer (batch queue table),
     *          CheckerOutwardDashboardComposer (pending batch preview).
     *
     * @return list of OutwardBatch objects, never null (empty list if none)
     */
    List<OutwardBatch> getCheckerQueueBatches();

    /**
     * Returns summary counts for the Checker Outward dashboard.
     *
     * Map keys and their meaning:
     *   "pending"     → count of batches with status = SUBMITTED
     *   "inProgress"  → count of batches with status = CHECKER_IN_PROGRESS
     *   "hold"        → count of batches with status = CHECKER_HOLD
     *   "approved"    → count of batches with status = CHECKER_APPROVED
     *                   (ready for DEM Export)
     *   "exported"    → count of batches with status = EXPORTED
     *                   (fully exported via DEM Export module)
     *
     * Used by: CheckerOutwardDashboardComposer (summary counts section).
     *
     * @return Map<String, Integer> with the five keys above, never null
     */
    Map<String, Integer> getDashboardCounts();
    // ════════════════════════════════════════════════════════════════════════
    //  Batch Operations
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Marks a batch as being actively processed by this Checker.
     *
     * Business rule:
     *   - If batch status is SUBMITTED   → update to CHECKER_IN_PROGRESS
     *   - If batch status is CHECKER_IN_PROGRESS or CHECKER_HOLD →
     *     no status change (Checker is resuming; batch stays as-is)
     *   - Any other status → no-op, return true (already processed)
     *
     * Called when the Checker clicks "Process" on a batch in the queue table,
     * before the split-screen view is shown.
     *
     * @param batchDbId  DB primary key of the outward_batch row
     * @param checkerId  DB user id of the logged-in Checker
     * @return true if successful or no-op, false if DB update failed
     */
    boolean openBatch(Long batchDbId, Long checkerId);

    /**
     * Marks a batch as fully verified by the Checker.
     *
     * Business rule:
     *   - Sets batch status = CHECKER_APPROVED
     *   - Sets verified_by = checkerId
     *   - Sets verified_at = current timestamp
     *
     * Called automatically inside passCheque() and rejectCheque()
     * after confirming that isAllActioned() returns true.
     * Should NOT be called directly by the composer.
     *
     * @param batchDbId  DB primary key of the outward_batch row
     * @param checkerId  DB user id of the logged-in Checker
     * @return true if update succeeded, false otherwise
     */
    boolean approveBatch(Long batchDbId, Long checkerId);

    // ════════════════════════════════════════════════════════════════════════
    //  Cheque Loading
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns ALL cheques for a batch, ordered by seq_no ASC.
     *
     * No status filtering is applied — the Checker sees every cheque,
     * including already-passed, rejected, and referred ones.
     * This allows the Checker to review prior decisions when navigating
     * back to a cheque they already actioned.
     *
     * Used by: CheckerQueueComposer to populate the split-screen navigator.
     *
     * @param batchDbId  DB primary key of the outward_batch row
     * @return ordered list of OutwardCheque, never null (empty list if none)
     */
    List<OutwardCheque> getChequesForBatch(Long batchDbId);

    // ════════════════════════════════════════════════════════════════════════
    //  Cheque Actions
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Passes a single cheque after Checker verification.
     *
     * What this does:
     *   1. Updates outward_cheque.status = CHECKER_PASSED
     *   2. Saves an OutwardCheckerAction audit record (action = PASSED)
     *   3. Calls isAllActioned(batchDbId) — if true, calls approveBatch()
     *
     * Note: Does NOT decrement batch cheque_count or actual_amount.
     * That decrement only happens during Maker rejection (rejectWithReason).
     *
     * @param chequeId   DB primary key of the outward_cheque row
     * @param checkerId  DB user id of the logged-in Checker
     * @param batchDbId  DB primary key of the parent outward_batch row
     * @return true if all operations succeeded, false on any failure
     */
    boolean passCheque(Long chequeId, Long checkerId, Long batchDbId);

    /**
     * Rejects a single cheque at the Checker stage.
     *
     * What this does:
     *   1. Updates outward_cheque.status = CHECKER_REJECTED
     *   2. Saves an OutwardCheckerAction audit record
     *        (action = REJECTED, reasonCode, remarks)
     *   3. Calls isAllActioned(batchDbId) — if true, calls approveBatch()
     *
     * Important distinction from Maker rejection:
     *   Maker's rejectWithReason() decrements batch totals (cheque_count, actual_amount).
     *   Checker rejection does NOT decrement totals — the cheque is flagged but
     *   the batch financial record remains unchanged for audit purposes.
     *
     * @param chequeId   DB primary key of the outward_cheque row
     * @param reasonCode 2-digit reason code string (e.g. "08", "11")
     * @param remarks    Optional additional remarks from Checker (may be null/empty)
     * @param checkerId  DB user id of the logged-in Checker
     * @param batchDbId  DB primary key of the parent outward_batch row
     * @return true if all operations succeeded, false on any failure
     */
    boolean rejectCheque(Long chequeId,
                         String reasonCode,
                         String remarks,
                         Long checkerId,
                         Long batchDbId);

    /**
     * Refers a single cheque back to the Maker for correction.
     *
     * What this does:
     *   1. Updates outward_cheque.status = CHECKER_REFERRED
     *   2. Saves an OutwardCheckerAction audit record
     *        (action = REFERRED, reasonCode, remarks)
     *   3. Updates outward_batch.status = CHECKER_HOLD
     *      (batch is now on hold until Maker corrects and re-submits)
     *
     * Note: Unlike Pass and Reject, Refer does NOT check isAllActioned()
     * because a referred cheque is not a final decision — it is returned
     * to the Maker. The batch cannot be approved while any cheque is REFERRED.
     *
     * The Maker will see this batch in their Account Entry screen
     * (status = CHECKER_HOLD) and the referred cheque (status = CHECKER_REFERRED)
     * will be available for re-entry. After re-entry, the cheque returns
     * to ENTRY_DONE and the Maker re-submits the batch to SUBMITTED.
     *
     * @param chequeId   DB primary key of the outward_cheque row
     * @param reasonCode 2-digit reason code string (e.g. "08", "11")
     * @param remarks    Optional additional remarks from Checker (may be null/empty)
     * @param checkerId  DB user id of the logged-in Checker
     * @param batchDbId  DB primary key of the parent outward_batch row
     * @return true if all operations succeeded, false on any failure
     */
    boolean referCheque(Long chequeId,
                        String reasonCode,
                        String remarks,
                        Long checkerId,
                        Long batchDbId);

    // ════════════════════════════════════════════════════════════════════════
    //  Completion Check
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns true when all cheques in a batch have been actioned by the Checker.
     *
     * A cheque is considered "actioned" when its status is anything other than
     * ENTRY_DONE. Specifically, the underlying DAO query checks:
     *   SELECT COUNT(*) FROM outward_cheque
     *   WHERE batch_id = :batchDbId AND status = 'ENTRY_DONE'
     *
     * Returns true (all done) when that count is 0.
     *
     * CHECKER_REFERRED cheques are NOT counted as ENTRY_DONE, so they do not
     * block batch approval — however, approveBatch() should only be called
     * after Pass or Reject (not after Refer), since Refer leaves the batch
     * in CHECKER_HOLD state.
     *
     * Called internally by: passCheque(), rejectCheque()
     *
     * @param batchDbId  DB primary key of the outward_batch row
     * @return true if count of ENTRY_DONE cheques in the batch is zero
     */
    boolean isAllActioned(Long batchDbId);
}