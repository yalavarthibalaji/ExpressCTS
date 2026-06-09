package com.iispl.util;

import java.io.File;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import com.iispl.dto.xml.cxf.CxfBatchInfoXml;
import com.iispl.dto.xml.cxf.CxfChequeListXml;
import com.iispl.dto.xml.cxf.CxfChequeXml;
import com.iispl.dto.xml.cxf.CxfRootXml;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;

/**
 * File    : com/iispl/util/CxfFileGenerator.java
 * Purpose : Generates the NPCI CTS CXF (Cheque eXchange File) XML
 *           for one outward batch and writes it to disk.
 *
 * Workflow:
 *   1. Caller builds an OutwardBatch with cheques attached.
 *   2. Caller invokes generate(batch, cheques, outputDir, generatedBy).
 *   3. This class builds the JAXB DTO tree, marshals to XML,
 *      and writes the file to outputDir/{batchId}.cxf.
 *   4. Returns the absolute path of the generated file.
 *
 * Cheque inclusion rule:
 *   Only cheques with status = CHECKER_PASSED are included.
 *   CHECKER_REJECTED and CHECKER_REFERRED cheques are excluded
 *   (per NPCI spec — rejected cheques are not transmitted).
 *
 * No external libraries needed — uses Jakarta JAXB.
 */
public class CxfFileGenerator {

    /** Fixed presenting-bank metadata (training project — would come from config in prod). */
    private static final String PRESENTING_BANK_CODE = "IISPL";
    private static final String PRESENTING_BANK_NAME = "IISPL Bank";
    private static final String CLEARING_HOUSE_CODE  = "MUMBAI_GRID";

    private static final DateTimeFormatter DATE_FMT      = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Generates one CXF file for the given batch and writes it to outputDir.
     *
     * @param batch        the outward batch entity (must have batchId, totals)
     * @param cheques      all cheques in the batch (only CHECKER_PASSED will be exported)
     * @param outputDir    target directory (will be created if missing)
     * @param generatedBy  user id of the checker who triggered export
     * @return absolute path of the generated .cxf file
     * @throws Exception if marshalling or file write fails
     */
    public static String generate(OutwardBatch         batch,
                                   List<OutwardCheque> cheques,
                                   File                outputDir,
                                   Long                generatedBy) throws Exception {

        if (batch == null) throw new IllegalArgumentException("batch is null");
        if (cheques == null || cheques.isEmpty()) {
            throw new IllegalArgumentException("cheques list is empty");
        }
        if (outputDir == null) throw new IllegalArgumentException("outputDir is null");

        // Step 1: ensure output directory exists
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new RuntimeException("Failed to create output dir: " + outputDir);
        }

        // Step 2: filter to only CHECKER_PASSED cheques
        List<OutwardCheque> passedCheques = new ArrayList<>();
        BigDecimal          passedTotal   = BigDecimal.ZERO;
        for (OutwardCheque c : cheques) {
            if ("CHECKER_PASSED".equalsIgnoreCase(c.getStatus())) {
                passedCheques.add(c);
                if (c.getAmount() != null) {
                    passedTotal = passedTotal.add(c.getAmount());
                }
            }
        }

        if (passedCheques.isEmpty()) {
            throw new RuntimeException(
                "Batch " + batch.getBatchId()
                + " has no CHECKER_PASSED cheques to export.");
        }

        System.out.println("CxfFileGenerator → Building CXF for batch=" + batch.getBatchId()
                + " | passedCheques=" + passedCheques.size()
                + " | totalAmount="   + passedTotal);

        // Step 3: build JAXB DTO tree
        CxfRootXml root = new CxfRootXml();
        root.setVersion("1.0");
        root.setBatchInfo(buildBatchInfo(batch, passedCheques.size(), passedTotal, generatedBy));
        root.setChequeList(buildChequeList(passedCheques));

        // Step 4: marshal to file
        File outputFile = new File(outputDir, batch.getBatchId() + ".cxf");

        try {
            JAXBContext  ctx       = JAXBContext.newInstance(CxfRootXml.class);
            Marshaller   marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING,         "UTF-8");

            marshaller.marshal(root, outputFile);
        } catch (JAXBException e) {
            throw new RuntimeException("CXF marshalling failed: " + e.getMessage(), e);
        }

        System.out.println("CxfFileGenerator → Wrote CXF file: " + outputFile.getAbsolutePath()
                + " (" + outputFile.length() + " bytes)");
        return outputFile.getAbsolutePath();
    }

    // ════════════════════════════════════════════════════
    //  Private builders
    // ════════════════════════════════════════════════════

    private static CxfBatchInfoXml buildBatchInfo(OutwardBatch batch,
                                                   int    chequeCount,
                                                   BigDecimal totalAmount,
                                                   Long   generatedBy) {

        CxfBatchInfoXml info = new CxfBatchInfoXml();
        info.setBatchId(batch.getBatchId());
        info.setPresentingBankCode(PRESENTING_BANK_CODE);
        info.setPresentingBankName(PRESENTING_BANK_NAME);
        info.setClearingHouseCode(CLEARING_HOUSE_CODE);
        info.setSessionCode(resolveSessionCode());
        info.setSessionDate(LocalDate.now().format(DATE_FMT));
        info.setChequeCount(chequeCount);
        info.setTotalAmount(totalAmount);
        info.setGeneratedAt(LocalDateTime.now().format(TIMESTAMP_FMT));
        info.setGeneratedBy(generatedBy);
        return info;
    }

    private static CxfChequeListXml buildChequeList(List<OutwardCheque> cheques) {
        CxfChequeListXml list   = new CxfChequeListXml();
        List<CxfChequeXml> items = new ArrayList<>();

        for (OutwardCheque c : cheques) {
            items.add(buildCheque(c));
        }
        list.setCheques(items);
        return list;
    }

    private static CxfChequeXml buildCheque(OutwardCheque c) {
        CxfChequeXml xml = new CxfChequeXml();
        xml.setSeqNo(c.getSeqNo());
        xml.setChequeNo(safe(c.getChequeNo()));
        // Prefer corrected MICR if it exists, else the original
        xml.setMicrCode(c.getMicrCodeCorrected() != null
                && !c.getMicrCodeCorrected().trim().isEmpty()
                ? c.getMicrCodeCorrected()
                : safe(c.getMicrCode()));
        xml.setCityCode(safe(c.getCityCode()));
        xml.setBankCode(safe(c.getBankCode()));
        xml.setBranchCode(safe(c.getBranchCode()));
        xml.setBaseNumber(safe(c.getBaseNumber()));
        xml.setTransactionCode(safe(c.getTransactionCode()));
        xml.setAccountNo(safe(c.getAccountNo()));
        xml.setAccountHolder(safe(c.getAccountHolder()));
        xml.setAmount(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO);
        xml.setChequeDate(c.getChequeDate() != null
                ? c.getChequeDate().toString() : "");
        xml.setPayeeName(safe(c.getPayeeName()));
        xml.setFrontImageRef(extractFileName(c.getFrontImagePath()));
        xml.setBackImageRef(extractFileName(c.getBackImagePath()));
        xml.setIqaStatus(safe(c.getIqaStatus()));
        return xml;
    }

    /**
     * Returns "MORNING" if current local time is before 12:00 noon, otherwise "AFTERNOON".
     */
    private static String resolveSessionCode() {
        return LocalDateTime.now().getHour() < 12 ? "MORNING" : "AFTERNOON";
    }

    /** Returns just the file name from a full path (or empty if null). */
    private static String extractFileName(String fullPath) {
        if (fullPath == null || fullPath.trim().isEmpty()) return "";
        String p = fullPath.replace('\\', '/').trim();
        int idx = p.lastIndexOf('/');
        return idx < 0 ? p : p.substring(idx + 1);
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}