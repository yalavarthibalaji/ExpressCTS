package com.iispl.service;

import com.iispl.entity.inward.InwardCheque;

import java.util.List;

/**
 * ChequeRepairService
 *
 * Business-layer interface for Step 2 (Date & Amount) and
 * Step 3 (Payee & Account) repair operations.
 */
public interface ChequeRepairService {

    /**
     * Return all cheques belonging to the given batch.
     *
     * @param batchId the parent batch ID
     * @return non-null list of InwardCheque (may be empty)
     */
    List<InwardCheque> getChequesByBatchId(String batchId);

    /**
     * Return a single cheque by PK.
     *
     * @param chequeId PK of inward_cheque
     * @return InwardCheque or null if not found
     */
    InwardCheque getChequeById(Long chequeId);

    /**
     * Save corrected date and amount values for a cheque (Step 2).
     *
     * @param cheque entity with updated chequeDateOcr, amountOcr, repairStatus
     * @return true on success
     */
    boolean saveDateAmountRepair(InwardCheque cheque);

    /**
     * Save corrected payee name and account number for a cheque (Step 3).
     *
     * @param cheque entity with updated payeeName, draweeAccountNumber, repairStatus
     * @return true on success
     */
    boolean savePayeeAccountRepair(InwardCheque cheque);

    /**
     * Count how many cheques in a batch still need repair.
     *
     * @param batchId the parent batch ID
     * @return pending count
     */
    int getPendingRepairCount(String batchId);
}