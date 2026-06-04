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

    // ── NEW METHODS FOR MICR REPAIR ──

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
    boolean saveAccountEntry(Long      chequeId,
                              String    accountNo,
                              String    accountHolder,
                              BigDecimal amount,
                              String    amountInWords,
                              String    chequeDate,
                              String    payeeName);
}