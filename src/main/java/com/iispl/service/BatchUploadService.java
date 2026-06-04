package com.iispl.service;

import java.io.File;
import java.util.List;

import com.iispl.dto.BatchUploadResult;
import com.iispl.entity.outward.OutwardBatch;

/**
 * File    : com/iispl/service/BatchUploadService.java
 * Purpose : Service interface for outward batch upload operations.
 */
public interface BatchUploadService {

    /**
     * Generates the next batch ID for today.
     * Format: B-YYYY-MMDD-NNN
     * Example: B-2026-0603-001
     *
     * @return generated batch ID string
     */
    String generateBatchId();

    /**
     * Main upload processing method.
     *
     * What it does:
     *   1. Validates inputs (file not null, expected values > 0)
     *   2. Generates batch ID
     *   3. Extracts ZIP and parses XML via CxfParser
     *   4. Validates each cheque's MICR via MicrValidator
     *   5. Detects count/amount mismatch
     *   6. Saves batch + cheques to DB
     *   7. Returns BatchUploadResult to composer
     *
     * @param zipFile             The uploaded ZIP file
     * @param expectedChequeCount Count entered by maker before upload
     * @param expectedTotalAmount Amount entered by maker before upload
     * @param currentMakerId             Logged-in maker's user ID (from session)
     * @return BatchUploadResult  with all data the composer needs
     */
    BatchUploadResult processBatchUpload(File zipFile,
            int expectedChequeCount,
            double expectedTotalAmount,
            Long makerId);
    
    
    /**
     * Rejects a single cheque from the batch.
     * The cheque is marked REJECTED in DB (kept for audit).
     * The batch totals (cheque_count, actual_amount) are decremented.
     *
     * @param chequeDbId DB id of the cheque to reject
     * @param currentMakerId     id of the maker performing the rejection
     * @return true if rejection succeeded
     */
    boolean rejectCheque(Long chequeDbId, Long makerId);
    
    /** Loads all batches created by this maker (for table display). */
    List<OutwardBatch> getMyBatches(Long makerId);

    /**
     * Rejects entire batch — called when maker clicks Reject in mismatch modal.
     * Marks batch + all its cheques as REJECTED in DB.
     */
    boolean rejectBatch(Long batchDbId);
}