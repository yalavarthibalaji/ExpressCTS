package com.iispl.service;

/**
 * File    : com/iispl/service/MakerOutwardService.java
 * Purpose : Dashboard- and batch-level helpers for the Maker Outward role.
 *
 * Methods:
 *   - getReferralCounts          → Dashboard module-picker popup
 *   - countActiveReferrals       → View Batches re-submit button visibility
 *   - resubmitBatch              → View Batches "Re-submit to Checker" action
 */
public interface MakerOutwardService {

    /**
     * Returns an int[2] with counts of cheques currently referred
     * to each fixable module for the given batch.
     *   index [0] = MICR_REPAIR count
     *   index [1] = DATA_ENTRY  count
     *
     * Used by Maker Dashboard to choose between:
     *   - direct navigation (only one module has referrals)
     *   - module-picker popup (both modules have referrals)
     */
    int[] getReferralCounts(Long batchDbId);

    /**
     * Total number of cheques in this batch whose referral has NOT yet
     * been cleared by the maker. Used by View Batches to gate the
     * "Re-submit to Checker" button (disabled until count = 0).
     */
    int countActiveReferrals(Long batchDbId);

    /**
     * Moves a REFER_BACK batch back to the Checker queue.
     *
     * Preconditions enforced:
     *   - Batch must be in REFER_BACK status
     *   - countActiveReferrals(batchDbId) must equal 0
     *
     * @param batchDbId outward_batch primary key
     * @param makerId   currently logged-in maker user id (for audit)
     * @return true on success, false if any precondition fails
     */
    boolean resubmitBatch(Long batchDbId, Long makerId);
}