package com.iispl.dao;

import com.iispl.entity.outward.OutwardCheque;
import java.util.List;

/**
 * File    : com/iispl/dao/OutwardChequeDao.java
 * Purpose : Database operations for outward_cheque table.
 */
public interface OutwardChequeDao {

    /**
     * Save a list of cheques for a batch in one transaction.
     * All cheques saved together — if one fails, all roll back.
     * @return true on success, false on failure
     */
    boolean saveAll(List<OutwardCheque> cheques);

    /**
     * Find all cheques belonging to a specific batch.
     * @return list of cheques ordered by seq_no
     */
    List<OutwardCheque> findByBatchId(Long batchId);

    /**
     * Count how many cheques in a batch have MICR errors.
     * Used after upload to decide batch routing
     * (NEEDS_REPAIR vs ENTRY_DONE).
     */
    int countMicrErrors(Long batchId);
    
    
    /**
     * Reject a single cheque from a batch.
     * Atomically updates:
     *   - cheque  → status = REJECTED, rejected_by, rejected_at
     *   - batch   → cheque_count - 1, actual_amount - cheque.amount
     *
     * @param chequeId DB id of the cheque
     * @param userId   maker id (sets rejected_by)
     * @return true on success
     */
    boolean rejectCheque(Long chequeId, Long userId);
}