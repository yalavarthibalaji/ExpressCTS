package com.iispl.dao;

import java.util.List;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;

public interface CheckerBatchProcessDao {

    InwardBatch findBatchWithCheques(String batchId);

    void saveCheckerActions(List<InwardCheckerAction> actions);

    void updateBatchStatus(InwardBatch batch);

    void saveReturnAction(InwardCheckerAction action);

    void updateBatchStatusTo(InwardBatch batch, String statusValue);

    void saveReferBackAction(InwardCheckerAction action);

    // Decrements total_cheques by 1 in DB after a refer-back
    void decrementBatchChequeCount(InwardBatch batch);
}