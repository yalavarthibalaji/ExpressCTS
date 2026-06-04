package com.iispl.serviceImpl;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.iispl.dao.OutwardBatchDao;
import com.iispl.dao.OutwardChequeDao;
import com.iispl.daoImpl.OutwardBatchDaoImpl;
import com.iispl.daoImpl.OutwardChequeDaoImpl;
import com.iispl.dto.BatchUploadResult;
import com.iispl.dto.xml.BatchXml;
import com.iispl.dto.xml.ChequeXml;
import com.iispl.entity.User;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.service.BatchUploadService;
import com.iispl.util.CxfParser;
import com.iispl.util.MicrValidator;

/**
 * File    : com/iispl/serviceImpl/BatchUploadServiceImpl.java
 * Purpose : Implements batch upload logic for outward cheque processing.
 *
 * Flow:
 *   1. validateInputs    → ensure zip file, count, amount, maker are valid
 *   2. generateBatchId   → e.g. B-2026-0603-001
 *   3. CxfParser.parse   → unzip + JAXB parse XML
 *   4. detectMismatch    → compare expected vs parsed totals
 *   5. buildChequeEntity → read isMicrError flag from XML for each cheque
 *   6. saveBatch+cheques → persist to DB
 *   7. return result     → composer renders table / mismatch modal
 */
public class BatchUploadServiceImpl implements BatchUploadService {

    private final OutwardBatchDao  batchDao  = new OutwardBatchDaoImpl();
    private final OutwardChequeDao chequeDao = new OutwardChequeDaoImpl();

    // ════════════════════════════════════════════════════
    //  Batch ID Generation
    //  Format: B-YYYY-MMDD-NNN  (e.g. B-2026-0603-001)
    // ════════════════════════════════════════════════════

    @Override
    public String generateBatchId() {
        String date   = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MMdd"));
        String prefix = "B-" + date;
        int    count  = batchDao.countBatchesToday(prefix);
        int    next   = count + 1;
        return String.format("%s-%03d", prefix, next);
    }

    // ════════════════════════════════════════════════════
    //  Main Upload Workflow
    // ════════════════════════════════════════════════════

    @Override
    public BatchUploadResult processBatchUpload(File   zipFile,
                                                 int    expectedChequeCount,
                                                 double expectedTotalAmount,
                                                 Long   makerId) {

        // ── Step 1: Validate inputs ──
        String inputError = validateInputs(zipFile, expectedChequeCount,
                                            expectedTotalAmount, makerId);
        if (inputError != null) {
            return BatchUploadResult.failure(inputError);
        }

        // ── Step 2: Generate batch ID ──
        String batchId = generateBatchId();
        System.out.println("BatchUploadService → Generated batch ID: " + batchId);

        // ── Step 3: Extract ZIP and parse XML ──
        CxfParser.ParseResult parseResult;
        try {
            parseResult = CxfParser.parse(zipFile, batchId);
        } catch (Exception e) {
            System.err.println("BatchUploadService → Parse failed: " + e.getMessage());
            return BatchUploadResult.failure(
                "Failed to read the uploaded file. "
                + "Please check the ZIP format and try again. "
                + "Error: " + e.getMessage());
        }

        BatchXml batchXml = parseResult.getBatchXml();

        // Safety check — XML must contain at least one cheque
        if (batchXml.getCheques() == null
                || batchXml.getCheques().getChequeList() == null
                || batchXml.getCheques().getChequeList().isEmpty()) {
            return BatchUploadResult.failure(
                "No cheques found in the uploaded XML file. "
                + "Please check the file and try again.");
        }

        List<ChequeXml> parsedCheques = batchXml.getCheques().getChequeList();
        int    parsedCount  = parsedCheques.size();
        double parsedAmount = calculateTotalAmount(parsedCheques);

        System.out.println("BatchUploadService → Parsed cheques: " + parsedCount
                + " | Total amount: " + parsedAmount);

        // ── Step 4: Detect mismatch ──
        boolean hasMismatch = detectMismatch(parsedCount, parsedAmount,
                                              expectedChequeCount, expectedTotalAmount);
        if (hasMismatch) {
            System.out.println("BatchUploadService → Mismatch detected. "
                    + "Expected: " + expectedChequeCount + " / " + expectedTotalAmount
                    + " | Parsed: " + parsedCount + " / " + parsedAmount);
        }

        // ── Step 5: Build cheque entities and count MICR errors ──
        boolean             hasMicrErrors  = false;
        List<OutwardCheque> chequeEntities = new ArrayList<>();

        for (int i = 0; i < parsedCheques.size(); i++) {
            ChequeXml xml = parsedCheques.get(i);

            String frontPath = CxfParser.buildImagePath(batchId, xml.getFrontImage());
            String backPath  = CxfParser.buildImagePath(batchId, xml.getBackImage());

            OutwardCheque cheque = buildChequeEntity(xml, i + 1, frontPath, backPath);

            if (cheque.isMicrError()) {
                hasMicrErrors = true;
            }

            chequeEntities.add(cheque);
        }

        // ── Step 6: Save OutwardBatch to DB ──
        String batchStatus       = hasMicrErrors ? "NEEDS_REPAIR" : "ENTRY_DONE";
        String batchRepairStatus = hasMicrErrors ? "NEEDS_REPAIR" : "NOT_REQUIRED";

        OutwardBatch batch = buildBatchEntity(batchId, parsedCount, parsedAmount,
                                               batchStatus, batchRepairStatus, makerId);

        OutwardBatch savedBatch = batchDao.save(batch);
        if (savedBatch == null) {
            return BatchUploadResult.failure(
                "Failed to save batch to database. Please try again.");
        }

        // ── Step 7: Link cheques to saved batch and save all ──
        for (OutwardCheque cheque : chequeEntities) {
            cheque.setBatch(savedBatch);
        }

        boolean chequesSaved = chequeDao.saveAll(chequeEntities);
        if (!chequesSaved) {
            return BatchUploadResult.failure(
                "Batch saved but failed to save cheques. Please contact support.");
        }

        System.out.println("BatchUploadService → Upload complete."
                + " BatchID: " + batchId
                + " | Status: " + batchStatus
                + " | MICR errors: " + hasMicrErrors);

        // ── Step 8: Return result to composer ──
        return BatchUploadResult.success(
            batchId,
            savedBatch.getId(),
            parsedCount,
            parsedAmount,
            expectedChequeCount,
            expectedTotalAmount,
            hasMismatch,
            hasMicrErrors,
            chequeEntities
        );
    }

    // ════════════════════════════════════════════════════
    //  Cheque Rejection
    // ════════════════════════════════════════════════════

    @Override
    public boolean rejectCheque(Long chequeDbId, Long makerId) {
        if (chequeDbId == null || makerId == null) {
            System.err.println("BatchUploadService → rejectCheque: "
                    + "missing chequeId or makerId");
            return false;
        }
        System.out.println("BatchUploadService → Rejecting cheque ID: "
                + chequeDbId + " by maker ID: " + makerId);
        return chequeDao.rejectCheque(chequeDbId, makerId);
    }

    // ════════════════════════════════════════════════════
    //  Private Helpers
    // ════════════════════════════════════════════════════

    /**
     * Validates all inputs before processing starts.
     * Returns an error message, or null if everything is valid.
     */
    private String validateInputs(File   zipFile,
                                   int    expectedChequeCount,
                                   double expectedTotalAmount,
                                   Long   makerId) {
        if (zipFile == null || !zipFile.exists()) {
            return "Please select a ZIP file to upload.";
        }
        if (!zipFile.getName().toLowerCase().endsWith(".zip")) {
            return "Invalid file type. Only ZIP files are accepted.";
        }
        if (expectedChequeCount <= 0) {
            return "Expected cheque count must be greater than 0.";
        }
        if (expectedTotalAmount <= 0) {
            return "Expected total amount must be greater than 0.";
        }
        if (makerId == null) {
            return "Session expired. Please log in again.";
        }
        return null;
    }

    /** Sums total amount from all parsed cheques. */
    private double calculateTotalAmount(List<ChequeXml> cheques) {
        double total = 0;
        for (ChequeXml c : cheques) {
            total += c.getAmount();
        }
        return total;
    }

    /**
     * Returns true if parsed values differ from user-entered expected values.
     * Triggers the mismatch modal in the composer.
     */
    private boolean detectMismatch(int    parsedCount,
                                    double parsedAmount,
                                    int    expectedCount,
                                    double expectedAmount) {
        boolean countMismatch  = parsedCount != expectedCount;
        boolean amountMismatch = Math.abs(parsedAmount - expectedAmount) > 0.01;
        return countMismatch || amountMismatch;
    }

    /** Builds an OutwardBatch entity ready to save. */
    private OutwardBatch buildBatchEntity(String batchId,
                                           int    chequeCount,
                                           double totalAmount,
                                           String status,
                                           String repairStatus,
                                           Long   makerId) {
        OutwardBatch batch = new OutwardBatch();
        batch.setBatchId(batchId);
        batch.setChequeCount(chequeCount);
        batch.setExpectedAmount(BigDecimal.valueOf(totalAmount));
        batch.setActualAmount(BigDecimal.valueOf(totalAmount));
        batch.setStatus(status);
        batch.setRepairStatus(repairStatus);
        batch.setCreatedAt(LocalDateTime.now());

        User maker = new User();
        maker.setId(makerId);
        batch.setCreatedBy(maker);

        return batch;
    }

    /**
     * Builds a single OutwardCheque entity from a parsed ChequeXml.
     * Batch FK is NOT set here — set after batch is saved in Step 7.
     */
    private OutwardCheque buildChequeEntity(ChequeXml cxf,
                                             int       seqNo,
                                             String    frontPath,
                                             String    backPath) {
        OutwardCheque cheque = new OutwardCheque();

        cheque.setSeqNo(seqNo);
        cheque.setChequeNo(cxf.getChequeNo());

        // MICR fields from XML (2 may be wrong if isMicrError = FAIL)
        cheque.setMicrCode(cxf.getMicrCode());
        cheque.setCityCode(cxf.getCityCode());
        cheque.setBankCode(cxf.getBankCode());
        cheque.setBranchCode(cxf.getBranchCode());
        cheque.setBaseNumber(cxf.getBaseNumber());
        cheque.setTransactionCode(cxf.getTransactionCode());

        // corrected starts same as raw — updated during MICR Repair
        cheque.setMicrCodeCorrected(cxf.getMicrCode());

        // MICR error flag directly from XML
        boolean micrError = MicrValidator.hasMicrError(cxf.getIsMicrError());
        cheque.setMicrError(micrError);
        cheque.setRepairStatus(micrError ? "NEEDS_REPAIR" : "NOT_REQUIRED");

        cheque.setAmount(BigDecimal.valueOf(cxf.getAmount()));

        if (cxf.getChequeDate() != null && !cxf.getChequeDate().trim().isEmpty()) {
            cheque.setChequeDate(LocalDate.parse(cxf.getChequeDate().trim()));
        }

        cheque.setAccountNo(cxf.getAccountNo());
        cheque.setPayeeName(cxf.getPayeeName());

        cheque.setIqaStatus(cxf.getIqaStatus() != null
                ? cxf.getIqaStatus().trim().toUpperCase()
                : "PASS");

        cheque.setFrontImagePath(frontPath);
        cheque.setBackImagePath(backPath);

        cheque.setStatus("PENDING");

        return cheque;
    }
    @Override
    public List<OutwardBatch> getMyBatches(Long makerId) {
        if (makerId == null) return new ArrayList<>();
        try {
            return batchDao.findByCreatedBy(makerId);
        } catch (Exception e) {
            System.err.println("BatchUploadService → getMyBatches failed: "
                    + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public boolean rejectBatch(Long batchDbId) {
        if (batchDbId == null) return false;
        System.out.println("BatchUploadService → Rejecting entire batch: "
                + batchDbId);
        return batchDao.updateStatus(batchDbId, "REJECTED");
    }
}