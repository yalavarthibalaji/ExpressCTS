package com.iispl.service;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;

import java.util.List;

public interface RejectRepairService {

    List<InwardBatch> getRepairEligibleBatches();

    InwardBatch getBatchById(String batchId);

    // ── Step 1 ──
    List<InwardCheque> getChequesByBatchId(String batchId);

    void saveRepair(InwardCheque cheque,String batchId);

    void referBack(Long chequeId, String remarks);

    // ── Step 2 ──
    List<InwardCheque> getStep2ChequesByBatchId(String batchId);

    void saveStep2Repair(InwardCheque cheque);

    // ── Step 3 ──
    List<InwardCheque> getStep3ChequesByBatchId(String batchId);

    void saveStep3Entry(InwardCheque cheque);

    void proceedToInwardChecker(String batchId);
}