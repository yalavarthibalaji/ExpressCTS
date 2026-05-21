package com.iispl.serviceImpl;

import com.iispl.dao.CheckerDao;
import com.iispl.daoImpl.CheckerDaoImpl;
import com.iispl.entity.CheckerBatch;
import com.iispl.entity.CheckerCheque;
import com.iispl.service.CheckerService;

import java.util.List;

/**
 * CheckerServiceImpl.java
 * Service Implementation — Checker business logic.
 * Package : com.iispl.serviceImpl
 */
public class CheckerServiceImpl implements CheckerService {

    private final CheckerDao checkerDao = new CheckerDaoImpl();

    // ── Existing methods ─────────────────────────────────────────────

    @Override
    public List<CheckerBatch> getAllCheckerBatches() {
        return checkerDao.getAllCheckerBatches();
    }

    @Override
    public List<CheckerCheque> getChequesByBatchId(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be blank");
        }
        return checkerDao.getChequesByBatchId(batchId);
    }

    @Override
    public CheckerCheque getChequeById(String chequeId) {
        if (chequeId == null || chequeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Cheque ID cannot be blank");
        }
        return checkerDao.getChequeById(chequeId);
    }

    @Override
    public void approveCheque(String chequeId, String remarks) {
        if (chequeId == null || chequeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Cheque ID cannot be blank");
        }
        String safeRemarks = (remarks == null) ? "" : remarks.trim();
        checkerDao.updateChequeCheckerStatus(chequeId, "approved", safeRemarks);
    }

    @Override
    public void rejectCheque(String chequeId, String remarks) {
        if (chequeId == null || chequeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Cheque ID cannot be blank");
        }
        if (remarks == null || remarks.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Remarks are required when flagging a cheque as an exception.");
        }
        checkerDao.updateChequeCheckerStatus(chequeId, "rejected", remarks.trim());
    }

    @Override
    public void approveBatch(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be blank");
        }
        List<CheckerCheque> cheques = checkerDao.getChequesByBatchId(batchId);
        long approvedCount = 0;
        for (CheckerCheque c : cheques) {
            if ("approved".equals(c.getCheckerStatus())) approvedCount++;
        }
        if (approvedCount == 0) {
            throw new IllegalStateException(
                "Please spot-check at least one cheque before approving the batch.");
        }
        checkerDao.approveBatch(batchId);
    }

    @Override
    public void returnBatchToMaker(String batchId, String remarks) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be blank");
        }
        if (remarks == null || remarks.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Please enter a reason before returning the batch to Maker.");
        }
        checkerDao.returnBatchToMaker(batchId, remarks.trim());
    }

    // ── New MICR Repair methods ──────────────────────────────────────

    @Override
    public List<CheckerCheque> getMicrRepairCheques() {
        // Returns all cheques where iqa_status = 'FAIL'
        // These are the ones needing MICR repair
        return checkerDao.getMicrRepairCheques();
    }

    @Override
    public void submitMicrRepair(String chequeId, String micrCode, String chequeNum,
                                 String bankName, String ifscCode, String remarks) {
        if (chequeId == null || chequeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Cheque ID cannot be blank");
        }
        if (remarks == null || remarks.trim().isEmpty()) {
            throw new IllegalArgumentException("Repair remarks are required");
        }
        // Save corrected data and mark as 'repaired' for checker to verify
        checkerDao.submitMicrRepair(
            chequeId,
            micrCode   == null ? "" : micrCode.trim(),
            chequeNum  == null ? "" : chequeNum.trim(),
            bankName   == null ? "" : bankName.trim(),
            ifscCode   == null ? "" : ifscCode.trim(),
            remarks.trim()
        );
    }

    @Override
    public void resetMicrChequeStatus(String chequeId, String remarks) {
        if (chequeId == null || chequeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Cheque ID cannot be blank");
        }
        if (remarks == null || remarks.trim().isEmpty()) {
            throw new IllegalArgumentException("Remarks required for rejection");
        }
        // Reset to pending so maker must repair again
        checkerDao.updateChequeCheckerStatus(chequeId, "pending", remarks.trim());
    }
}