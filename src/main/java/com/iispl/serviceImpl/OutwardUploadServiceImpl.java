package com.iispl.serviceImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.iispl.dao.OutwardBatchDao;
import com.iispl.dao.OutwardChequeDao;
import com.iispl.daoImpl.OutwardBatchDaoImpl;
import com.iispl.daoImpl.OutwardChequeDaoImpl;
import com.iispl.entity.OutwardBatch;
import com.iispl.entity.OutwardCheque;
import com.iispl.service.OutwardUploadService;
import com.iispl.xml.BatchHeaderXml;
import com.iispl.xml.ChequeXml;
import com.iispl.xml.OutwardBatchXml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

public class OutwardUploadServiceImpl implements OutwardUploadService {

    private OutwardBatchDao batchDao = new OutwardBatchDaoImpl();
    private OutwardChequeDao chequeDao = new OutwardChequeDaoImpl();

    private static final String EXTRACT_BASE_PATH = "uploads/outward/";

    @Override
    public void processUpload(File zipFile, String uploadedBy) {
        System.out.println("Upload started by: " + uploadedBy);
        System.out.println("ZIP file: " + zipFile.getName());

        // Step 1 - extract ZIP
        String extractedFolderPath = extractZip(zipFile);
        System.out.println("ZIP extracted to: " + extractedFolderPath);

        // Step 2 - find XML file inside extracted folder
        File xmlFile = findXmlFile(extractedFolderPath);
        if (xmlFile == null) {
            throw new RuntimeException("No XML file found inside the ZIP.");
        }
        System.out.println("XML file found: " + xmlFile.getName());

        // Step 3 - parse XML using JAXB
        OutwardBatchXml batchXml = parseXml(xmlFile);
        System.out.println("XML parsed successfully.");

        // Step 4 - read batch header
        BatchHeaderXml header = batchXml.getBatchHeader();

        // Step 5 - validate batch header fields
        validateBatchHeader(header);

        // Step 6 - get cheque list
        List<ChequeXml> chequeList = batchXml.getCheques().getChequeList();
        if (chequeList == null || chequeList.isEmpty()) {
            throw new RuntimeException("No cheques found in XML.");
        }

        // Step 7 - validate cheque count matches header
        if (chequeList.size() != header.getTotalCheques()) {
            throw new RuntimeException(
                "Cheque count mismatch. Header says: " + header.getTotalCheques()
                + " but found: " + chequeList.size());
        }

        // Step 8 - generate batch id
        String batchId = generateBatchId(header.getBranchCode(), header.getClearingDate());
        System.out.println("Generated BatchId: " + batchId);

        // Step 9 - count iqa pass and fail
        int iqaPass = countIqaPass(chequeList);
        int iqaFail = chequeList.size() - iqaPass;

        // Step 10 - check if any cheque needs micr repair
        boolean hasMicrRepair = checkMicrRepair(chequeList);

        // Step 11 - build and save batch to outward_batches
        OutwardBatch batch = buildBatchEntity(
                batchId, header, zipFile.getName(),
                xmlFile.getName(), uploadedBy,
                iqaPass, iqaFail, hasMicrRepair);
        batchDao.saveBatch(batch);
        System.out.println("Batch saved to DB.");

        // Step 12 - loop through each cheque
        // validate and save directly to outward_cheques master table
        long totalAmountFromXml = 0;
        for (ChequeXml chequeXml : chequeList) {

            // validate each cheque fields
            validateCheque(chequeXml);

            // build master table entity
            OutwardCheque cheque = buildChequeEntity(
                    chequeXml, batch, extractedFolderPath);

            // save to master table
            chequeDao.saveCheque(cheque);

            totalAmountFromXml += chequeXml.getAmountInFigures();
        }

        // Step 13 - validate total amount matches header
        if (totalAmountFromXml != header.getTotalAmount()) {
            System.out.println("Warning: Total amount mismatch."
                + " Header: " + header.getTotalAmount()
                + " Actual: " + totalAmountFromXml);
        }

        System.out.println("All cheques saved to master table. Total: " + chequeList.size());
        System.out.println("Upload completed for batch: " + batchId);
    }


    // ── Extract ZIP ───────────────────────────────────────────────────

    private String extractZip(File zipFile) {
        String folderName = zipFile.getName().replace(".zip", "");
        String extractPath = EXTRACT_BASE_PATH + folderName + "/";

        File extractDir = new File(extractPath);
        if (!extractDir.exists()) {
            extractDir.mkdirs();
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String filePath = extractPath + entry.getName();

                if (entry.isDirectory()) {
                    new File(filePath).mkdirs();
                } else {
                    new File(filePath).getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipIn.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zipIn.closeEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to extract ZIP: " + e.getMessage());
        }

        return extractPath;
    }


    // ── Find XML file recursively ─────────────────────────────────────

    private File findXmlFile(String folderPath) {
        File folder = new File(folderPath);
        return searchXmlRecursively(folder);
    }

    private File searchXmlRecursively(File folder) {
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".xml")) {
                    return file;
                } else if (file.isDirectory()) {
                    File found = searchXmlRecursively(file);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }


    // ── Parse XML using JAXB ──────────────────────────────────────────

    private OutwardBatchXml parseXml(File xmlFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(OutwardBatchXml.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (OutwardBatchXml) unmarshaller.unmarshal(xmlFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse XML: " + e.getMessage());
        }
    }


    // ── Validate Batch Header ─────────────────────────────────────────

    private void validateBatchHeader(BatchHeaderXml header) {
        if (header.getBranchCode() == null || header.getBranchCode().isEmpty()) {
            throw new RuntimeException("Validation failed: BranchCode is missing in XML.");
        }
        if (header.getClearingDate() == null || header.getClearingDate().isEmpty()) {
            throw new RuntimeException("Validation failed: ClearingDate is missing in XML.");
        }
        if (header.getTotalCheques() <= 0) {
            throw new RuntimeException("Validation failed: TotalCheques must be greater than 0.");
        }
        if (header.getTotalAmount() <= 0) {
            throw new RuntimeException("Validation failed: TotalAmount must be greater than 0.");
        }
    }


    // ── Validate Each Cheque ──────────────────────────────────────────

    private void validateCheque(ChequeXml cheque) {
        if (cheque.getChequeNumber() == null || cheque.getChequeNumber().isEmpty()) {
            throw new RuntimeException("Validation failed: ChequeNumber is missing.");
        }
        if (cheque.getAmountInFigures() <= 0) {
            throw new RuntimeException(
                "Validation failed: AmountInFigures must be greater than 0"
                + " for cheque: " + cheque.getChequeNumber());
        }
        if (cheque.getMicrCode() == null || cheque.getMicrCode().isEmpty()) {
            throw new RuntimeException(
                "Validation failed: MicrCode is missing"
                + " for cheque: " + cheque.getChequeNumber());
        }
        if (cheque.getIfscCode() == null || cheque.getIfscCode().isEmpty()) {
            throw new RuntimeException(
                "Validation failed: IfscCode is missing"
                + " for cheque: " + cheque.getChequeNumber());
        }
    }


    // ── Generate Batch ID ─────────────────────────────────────────────

    private String generateBatchId(String branchCode, String clearingDate) {
        String dateFormatted = clearingDate.replace("-", "");
        int existingCount = batchDao.countBatchesTodayForBranch(branchCode, clearingDate);
        String sequence = String.format("%03d", existingCount + 1);
        return "BATCH-" + branchCode + "-" + dateFormatted + "-" + sequence;
    }


    // ── Count IQA Pass ────────────────────────────────────────────────

    private int countIqaPass(List<ChequeXml> chequeList) {
        int count = 0;
        for (ChequeXml cheque : chequeList) {
            if ("PASS".equalsIgnoreCase(cheque.getIqaStatus())) {
                count++;
            }
        }
        return count;
    }


    // ── Check MICR Repair ─────────────────────────────────────────────

    private boolean checkMicrRepair(List<ChequeXml> chequeList) {
        for (ChequeXml cheque : chequeList) {
            if ("REPAIR_NEEDED".equalsIgnoreCase(cheque.getMicrStatus())) {
                return true;
            }
        }
        return false;
    }


    // ── Build OutwardBatch Entity ─────────────────────────────────────

    private OutwardBatch buildBatchEntity(
            String batchId,
            BatchHeaderXml header,
            String zipFileName,
            String xmlFileName,
            String uploadedBy,
            int iqaPass,
            int iqaFail,
            boolean hasMicrRepair) {

        OutwardBatch batch = new OutwardBatch();
        batch.setBatchId(batchId);
        batch.setBranchCode(header.getBranchCode());
        batch.setClearingDate(LocalDate.parse(header.getClearingDate()));
        batch.setClearingSessionRef(header.getClearingSessionRef());
        batch.setRoute(header.getRoute());
        batch.setStatus("READY");
        batch.setIsMicrRepairBatch(hasMicrRepair);
        batch.setTotalCheques(header.getTotalCheques());
        batch.setTotalAmount(header.getTotalAmount());
        batch.setIqaPass(iqaPass);
        batch.setIqaFail(iqaFail);
        batch.setXmlFileName(xmlFileName);
        batch.setCbxFile(zipFileName);
        batch.setMakerDone(false);
        batch.setCheckerDone(false);
        batch.setCxfGenerated(false);
        batch.setDemSent(false);
        batch.setSupervisorVerified(false);
        batch.setCreatedBy(uploadedBy);
        batch.setScannedAt(LocalDateTime.now());
        return batch;
    }


    // ── Build OutwardCheque Entity ────────────────────────────────────

    private OutwardCheque buildChequeEntity(
            ChequeXml chequeXml,
            OutwardBatch batch,
            String extractedFolderPath) {

        OutwardCheque cheque = new OutwardCheque();

        // system generated ids
        cheque.setChequeId(generateChequeId());
        cheque.setTransactionId(generateTransactionId());

        // link to batch using HAS-A relationship
        cheque.setBatch(batch);

        cheque.setChequeNumber(chequeXml.getChequeNumber());
        cheque.setBankName(chequeXml.getBankName());
        cheque.setBranchName(chequeXml.getBranchName());
        cheque.setIfscCode(chequeXml.getIfscCode());
        cheque.setMicrCode(chequeXml.getMicrCode());
        cheque.setMicrStatus(chequeXml.getMicrStatus());
        cheque.setChequeDate(LocalDate.parse(chequeXml.getChequeDate()));
        cheque.setPresentationDate(LocalDate.parse(chequeXml.getPresentationDate()));
        cheque.setDrawerName(chequeXml.getDrawerName());
        cheque.setDrawerAccountNumber(chequeXml.getDrawerAccountNumber());
        cheque.setPayeeName(chequeXml.getPayeeName());
        cheque.setAmountInWords(chequeXml.getAmountInWords());
        cheque.setAmountInFigures(chequeXml.getAmountInFigures());
        cheque.setDepositorAccountNumber(chequeXml.getDepositorAccountNumber());

        // full image paths
        cheque.setImageFrontPath(extractedFolderPath + chequeXml.getImageFrontPath());
        cheque.setImageBackPath(extractedFolderPath + chequeXml.getImageBackPath());

        cheque.setIqaStatus(chequeXml.getIqaStatus());

        // system calculated
        cheque.setHvCategory(calculateHvCategory(chequeXml.getAmountInFigures()));

        // default workflow statuses
        cheque.setMakerStatus("PENDING");
        cheque.setCheckerStatus("PENDING");
        cheque.setReviewed(false);

        return cheque;
    }


    // ── Generate Cheque ID ────────────────────────────────────────────

    private String generateChequeId() {
        // CHQ + first 13 chars of UUID without dashes
        return "CHQ" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 13).toUpperCase();
    }


    // ── Generate Transaction ID ───────────────────────────────────────

    private String generateTransactionId() {
        // TXN + first 17 chars of UUID without dashes
        return "TXN" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 17).toUpperCase();
    }


    // ── Calculate HV Category ─────────────────────────────────────────

    private String calculateHvCategory(long amountInFigures) {
        // HV = High Value (1 lakh and above)
        // NV = Normal Value (below 1 lakh)
        if (amountInFigures >= 100000) {
            return "HV";
        }
        return "NV";
    }
}