package com.iispl.dao;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;

import java.util.List;

public interface RejectRepairDao {

    List<InwardBatch> findRepairEligibleBatches();

    InwardBatch findBatchById(String batchId);

    List<InwardCheque> findChequesByBatchId(String batchId);

    void updateCheque(InwardCheque cheque);

    List<InwardCheque> findStep2ChequesByBatchId(String batchId);

    List<InwardCheque> findStep3ChequesByBatchId(String batchId);

    void updateBatchStatus(String batchId, String status, String repairStatus);
}