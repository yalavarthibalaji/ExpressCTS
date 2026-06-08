package com.iispl.dao;

import java.math.BigDecimal;
import java.util.List;

import com.iispl.entity.outward.OutwardCheque;

/**
 * File    : com/iispl/dao/OutwardChequeDao.java
 * Purpose : Database operations for outward_cheque table.
 */
public interface OutwardChequeDao {

    boolean saveAll(List<OutwardCheque> cheques);

    List<OutwardCheque> findByBatchId(Long batchId);

    int countMicrErrors(Long batchId);

    boolean rejectCheque(Long chequeId, Long userId);

    // ── MICR Repair ──────────────────────────────────────────────────────────

    /**
     * Returns all MICR-error cheques for a batch that are NOT yet
     * repaired and NOT rejected.
     * Used to populate the MICR Repair screen list.
     */
    List<OutwardCheque> findMicrErrorCheques(Long batchDbId);

    /**
     * Save corrected MICR sub-fields to the cheque.
     * Sets repair_status = REPAIRED and rebuilds micr_code_corrected.
     */
    boolean updateMicrRepaired(Long chequeId,
                                String cityCode,
                                String bankCode,
                                String branchCode,
                                String baseNumber,
                                String transactionCode,
                                String micrCodeCorrected);

    /**
     * Count cheques in a batch that still need MICR repair.
     * Returns 0 when all MICR errors have been repaired or rejected.
     */
    int countPendingMicrRepairs(Long batchDbId);

    /**
     * Reject a cheque with a specific reason code and remarks.
     * Used on the MICR Repair screen reject panel.
     */
    boolean rejectWithReason(Long chequeId,
                              String reasonCode,
                              String remarks,
                              Long userId);

    // ── Account Entry ─────────────────────────────────────────────────────────

    /**
     * Returns all cheques in a batch that are PENDING account entry.
     * Excludes REJECTED and already ENTRY_DONE cheques.
     * Ordered by seq_no.
     */
    List<OutwardCheque> findPendingEntries(Long batchDbId);

    /**
     * Count of cheques still pending account entry in a batch.
     */
    int countPendingEntries(Long batchDbId);

    /**
     * Saves account entry data for a cheque.
     * Sets status = ENTRY_DONE after successful save.
     */
    boolean saveAccountEntry(Long       chequeId,
                              String     accountNo,
                              String     accountHolder,
                              BigDecimal amount,
                              String     amountInWords,
                              String     chequeDate,
                              String     payeeName);

    // ── Checker Outward ───────────────────────────────────────────────────────

    /**
     * Returns ALL cheques for a batch, ordered by seq_no ASC.
     * No status filtering — Checker sees every cheque including
     * already-passed, rejected, and referred ones.
     *
     * Used in: CheckerQueueComposer to populate the split-screen navigator.
     */
    List<OutwardCheque> findAllByBatchDbId(Long batchDbId);

    /**
     * Updates the status column on a single cheque row.
     * Called after every Checker action — Pass, Reject, or Refer.
     *
     * Expected values for newStatus:
     *   CHECKER_PASSED   — Checker passed this cheque.
     *   CHECKER_REJECTED — Checker rejected this cheque.
     *   CHECKER_REFERRED — Checker referred this cheque back to Maker.
     */
    boolean updateCheckerStatus(Long chequeId, String newStatus);

    /**
     * Counts cheques in a batch that still have status = ENTRY_DONE,
     * meaning the Checker has not yet acted on them.
     *
     * Returns 0 when all cheques in the batch have been
     * passed, rejected, or referred by the Checker.
     *
     * Used in: CheckerServiceImpl.isAllChequesActioned()
     */
    int countPendingCheckerActions(Long batchDbId);
    
    
    /**
     * Marks cheque CHECKER_REFERRED AND saves the maker module that should fix it.
     * Called by Checker → Send to Maker action.
     *
     * @param chequeId       cheque primary key
     * @param referToModule  'MICR_REPAIR' or 'DATA_ENTRY'
     * @return true if 1 row updated
     */
    boolean markReferredWithModule(Long chequeId, String referToModule);

    /**
     * Clears the referral on a cheque after the Maker has fixed it.
     * Sets status back to the given recovery status and clears referred_to_module.
     *
     * @param chequeId        cheque primary key
     * @param recoveryStatus  the status to set after the fix (e.g. 'ENTRY_DONE')
     * @return true if 1 row updated
     */
    boolean clearReferral(Long chequeId, String recoveryStatus);

    /**
     * Count of cheques in a batch currently referred to a specific module.
     *
     * @param batchDbId  outward_batch primary key
     * @param module     'MICR_REPAIR' or 'DATA_ENTRY'
     * @return number of cheques with status='CHECKER_REFERRED' AND referred_to_module=module
     */
    int countReferredByModule(Long batchDbId, String module);

    /**
     * Returns the list of cheques in a batch currently referred to the given module.
     * Used by MICR Repair / Account Entry to show ONLY the cheques the maker
     * needs to fix from a REFER_BACK batch.
     */
    java.util.List<com.iispl.entity.outward.OutwardCheque>
            findReferredByModule(Long batchDbId, String module);
    
    /**
     * Returns the count of cheques in a batch whose referral has NOT
     * been cleared (referred_to_module IS NOT NULL).
     * Used to gate the "Re-submit to Checker" button.
     */
    int countActiveReferrals(Long batchDbId);
}