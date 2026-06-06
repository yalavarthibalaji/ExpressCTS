package com.iispl.dao;

import com.iispl.entity.inward.InwardCheque;

import java.util.List;

/**
 * ChequeRepairDao
 *
 * DAO interface for Step 2 (Date & Amount) and Step 3 (Payee & Account)
 * cheque-level repair operations.
 */
public interface ChequeRepairDao {

    /**
     * Fetch all cheques belonging to a batch that require date/amount repair.
     * Matches repair_status = 'NEEDS_REPAIR' (or NOT_REQUIRED for review).
     *
     * @param batchId the parent batch ID
     * @return list of InwardCheque rows, ordered by seq_no
     */
    List<InwardCheque> findChequesByBatchId(String batchId);

    /**
     * Find a single cheque by its primary key (id).
     *
     * @param chequeId the PK of inward_cheque
     * @return InwardCheque or null
     */
    InwardCheque findChequeById(Long chequeId);

    /**
     * Persist corrected date/amount values for a cheque (Step 2 save).
     * Updates: cheque_date_ocr, amount_ocr, repair_status, updated_at.
     *
     * @param cheque the cheque entity carrying corrected values
     * @return true if one row was updated
     */
    boolean updateDateAmount(InwardCheque cheque);

    /**
     * Persist corrected payee name and account number for a cheque (Step 3 save).
     * Updates: payee_name, drawee_account_number, repair_status, updated_at.
     *
     * @param cheque the cheque entity carrying payee/account values
     * @return true if one row was updated
     */
    boolean updatePayeeAccount(InwardCheque cheque);

    /**
     * Count cheques in a batch that still have repair_status = 'NEEDS_REPAIR'.
     *
     * @param batchId the parent batch ID
     * @return count of pending cheques
     */
    int countPendingRepairs(String batchId);
}