package com.iispl.service;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import java.util.List;

public interface RejectRepairService {

    // ── Batch ──────────────────────────────────────────────────────────────
    List<InwardBatch> getRepairEligibleBatches();

    InwardBatch getBatchById(String batchId);

    // ── Step 1: MICR Repair ────────────────────────────────────────────────
    List<InwardCheque> getChequesByBatchId(Long batchId);

    void saveRepair(InwardCheque cheque, String batchId);

    void referBack(Long chequeId, String remarks);

    // ── Step 2: Date & Amount ──────────────────────────────────────────────
    List<InwardCheque> getStep2ChequesByBatchId(Long batchId);

    void saveStep2Repair(InwardCheque cheque);

    void rejectStep2(InwardCheque cheque, String rejectReason);

    void referStep2(InwardCheque cheque, String referReason);

    // ── Step 3: Payee & Account ────────────────────────────────────────────
    List<InwardCheque> getStep3ChequesByBatchId(Long batchId);

    void saveStep3Entry(InwardCheque cheque);

    // ── Submit ─────────────────────────────────────────────────────────────
    void proceedToInwardChecker(String batchId);
}