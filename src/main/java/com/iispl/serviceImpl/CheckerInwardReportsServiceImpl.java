package com.iispl.serviceImpl;

import com.iispl.dao.CheckerInwardReportsDao;
import com.iispl.daoImpl.CheckerInwardReportsDaoImpl;
import com.iispl.dto.InwardReportDTO;
import com.iispl.dto.xml.AckBatchDto;
import com.iispl.dto.xml.AckChequeDto;
import com.iispl.dto.xml.RrfBatchDto;
import com.iispl.dto.xml.RrfChequeDto;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.CheckerInwardReportsService;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * File    : com/iispl/serviceImpl/CheckerInwardReportsServiceImpl.java
 *
 * Status flow (existing enum values only — no new enum values added):
 *   Verified      → eligible for Generate to Debit
 *   CBS_Processed → debit already generated (Completed)
 *
 * generateToDebit():
 *   1.  Validates batchId.
 *   2.  Confirms status = "Verified" — guard against duplicates.
 *   3.  Fetches batch + cheques + checker actions from DB.
 *   4.  Generates ACK.xml  → /xml/ack/ACK_<batchId>.xml   (accepted cheques only)
 *   5.  Generates RRF.xml  → /xml/rrf/RRF_<batchId>.xml   (all cheques + rejection detail)
 *   6.  Updates batch status to "CBS_Processed" via DAO.
 *   7.  On any failure, status stays "Verified" so the operator can retry.
 */
public class CheckerInwardReportsServiceImpl implements CheckerInwardReportsService {

    private static final Logger log =
            LoggerFactory.getLogger(CheckerInwardReportsServiceImpl.class);

    /** DB status that makes a batch eligible for debit generation. */
    private static final String ELIGIBLE_STATUS  = "Verified";

    /** DB status set after successful debit + XML generation. */
    private static final String COMPLETED_STATUS = "CBS_Processed";

    /** Output folder for ACK XML files. Created at runtime if absent. */
    private static final String ACK_FOLDER = "C:/ExpressCTS/xml/ack";

    /** Output folder for RRF XML files. Created at runtime if absent. */
    private static final String RRF_FOLDER = "C:/ExpressCTS/xml/ack";

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final CheckerInwardReportsDao dao = new CheckerInwardReportsDaoImpl();

    // ─────────────────────────────────────────────────────────────────────────
    //  Query methods  (unchanged)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<InwardReportDTO> getReports(String batchIdSearch,
                                             Date   fromDate,
                                             Date   toDate,
                                             String status,
                                             int    pageNo,
                                             int    pageSize) {

        validateDateRange(fromDate, toDate);
        String normalizedStatus = normalizeStatus(status);

        log.info("getReports — batchId='{}', from={}, to={}, status='{}'",
                 batchIdSearch, fromDate, toDate, normalizedStatus);

        return dao.findReports(
                trimOrNull(batchIdSearch),
                fromDate,
                toDate,
                normalizedStatus,
                pageNo,
                pageSize
        );
    }

    @Override
    public int getTotalCount(String batchIdSearch,
                              Date   fromDate,
                              Date   toDate,
                              String status) {

        validateDateRange(fromDate, toDate);
        return dao.countReports(
                trimOrNull(batchIdSearch),
                fromDate,
                toDate,
                normalizeStatus(status)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Generate to Debit  (enhanced with JAXB XML generation)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void generateToDebit(String batchId) {

        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID must not be blank.");
        }

        String trimmedId = batchId.trim();
        log.info("generateToDebit — starting for batch '{}'", trimmedId);

        // ── Step 1: Guard — batch must be in Verified state ──────────────────
        String currentStatus = dao.getBatchStatus(trimmedId);
        if (currentStatus == null) {
            throw new IllegalArgumentException(
                    "Batch '" + trimmedId + "' not found.");
        }
        if (!ELIGIBLE_STATUS.equals(currentStatus)) {
            throw new IllegalArgumentException(
                    "Batch '" + trimmedId + "' is not eligible for debit generation. " +
                    "Current status: " + currentStatus + ".");
        }

        try {
            // ── Step 2: Fetch batch with all cheques and checker actions ──────
            InwardBatch batch = dao.findBatchWithChequesAndActions(trimmedId);
            if (batch == null) {
                throw new IllegalArgumentException(
                        "Batch '" + trimmedId + "' could not be loaded for XML generation.");
            }

            List<InwardCheque> cheques = batch.getCheques();
            if (cheques == null) cheques = new ArrayList<>();

            String generatedAt = LocalDateTime.now().format(TS_FMT);
            String batchDate   = batch.getBatchDate() != null
                                 ? batch.getBatchDate().toString() : "";

            // ── Step 3: Generate ACK.xml ──────────────────────────────────────
            String ackPath = generateAckXml(batch, cheques, trimmedId, batchDate, generatedAt);
            log.info("generateToDebit — ACK.xml written to '{}'", ackPath);

            // ── Step 4: Generate RRF.xml ──────────────────────────────────────
            String rrfPath = generateRrfXml(batch, cheques, trimmedId, batchDate, generatedAt);
            log.info("generateToDebit — RRF.xml written to '{}'", rrfPath);

            // ── Step 5: Execute legacy debit entries (existing DAO logic) ─────
            dao.executeDebitGeneration(trimmedId);

            // ── Step 6: Transition to CBS_Processed ───────────────────────────
            dao.updateBatchStatus(trimmedId, COMPLETED_STATUS);
            log.info("generateToDebit — batch '{}' set to {}", trimmedId, COMPLETED_STATUS);

        } catch (IllegalArgumentException e) {
            // Re-throw validation errors as-is; status stays Verified → retryable
            throw e;
        } catch (Exception e) {
            // Status stays as Verified so the operator can retry
            log.error("generateToDebit — batch '{}' FAILED: {}", trimmedId, e.getMessage(), e);
            throw new RuntimeException(
                    "Debit generation failed for batch '" + trimmedId + "': " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ACK XML generation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds an AckBatchDto containing only ACCEPTED cheques, marshals it to
     * /xml/ack/ACK_<batchId>.xml, and returns the absolute file path.
     */
    private String generateAckXml(InwardBatch batch,
                                   List<InwardCheque> cheques,
                                   String batchId,
                                   String batchDate,
                                   String generatedAt) throws Exception {

        List<AckChequeDto> ackCheques = new ArrayList<>();

        for (InwardCheque cheque : cheques) {
            if (!"ACCEPTED".equalsIgnoreCase(cheque.getStatus())) continue;

            AckChequeDto dto = new AckChequeDto();
            dto.setSeqNo(cheque.getSeqNo());
            dto.setChequeNo(safe(cheque.getChequeNo()));
            dto.setChequeDate(cheque.getChequeDate() != null
                              ? cheque.getChequeDate().toString() : "");
            dto.setAmount(cheque.getAmount() != null
                          ? cheque.getAmount().toPlainString() : "0.00");
            dto.setMicrCode(resolveMicrCode(cheque));
            dto.setCityCode(safe(cheque.getCityCode()));
            dto.setBankCode(safe(cheque.getBankCode()));
            dto.setBranchCode(safe(cheque.getBranchCode()));
            dto.setPresentingBankCode(safe(cheque.getPresentingBankCode()));
            dto.setPresentingBankName(safe(cheque.getPresentingBankName()));
            dto.setDraweeAccountNumber(safe(cheque.getDraweeAccountNumber()));
            dto.setDraweeAccountHolder(safe(cheque.getDraweeAccountHolder()));
            dto.setPayeeName(safe(cheque.getPayeeName()));
            dto.setStatus(cheque.getStatus());

            ackCheques.add(dto);
        }

        AckBatchDto ackBatch = new AckBatchDto();
        ackBatch.setBatchId(batchId);
        ackBatch.setBatchDate(batchDate);
        ackBatch.setTotalAccepted(ackCheques.size());
        ackBatch.setGeneratedAt(generatedAt);
        ackBatch.setCheques(ackCheques);

        File outFile = prepareOutputFile(ACK_FOLDER, "ACK_" + batchId + ".xml");
        marshalToFile(AckBatchDto.class, ackBatch, outFile);
        return outFile.getAbsolutePath();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RRF XML generation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds an RrfBatchDto containing ALL cheques (accepted + returned),
     * marshals it to /xml/rrf/RRF_<batchId>.xml, and returns the absolute path.
     * Rejection reason details are sourced from InwardCheckerAction.
     */
    private String generateRrfXml(InwardBatch batch,
                                   List<InwardCheque> cheques,
                                   String batchId,
                                   String batchDate,
                                   String generatedAt) throws Exception {

        List<RrfChequeDto> rrfCheques = new ArrayList<>();
        int acceptedCount = 0;
        int returnedCount = 0;

        for (InwardCheque cheque : cheques) {

            RrfChequeDto dto = new RrfChequeDto();
            dto.setSeqNo(cheque.getSeqNo());
            dto.setChequeNo(safe(cheque.getChequeNo()));
            dto.setChequeDate(cheque.getChequeDate() != null
                              ? cheque.getChequeDate().toString() : "");
            dto.setAmount(cheque.getAmount() != null
                          ? cheque.getAmount().toPlainString() : "0.00");
            dto.setMicrCode(resolveMicrCode(cheque));
            dto.setCityCode(safe(cheque.getCityCode()));
            dto.setBankCode(safe(cheque.getBankCode()));
            dto.setBranchCode(safe(cheque.getBranchCode()));
            dto.setPresentingBankCode(safe(cheque.getPresentingBankCode()));
            dto.setPresentingBankName(safe(cheque.getPresentingBankName()));
            dto.setDraweeAccountNumber(safe(cheque.getDraweeAccountNumber()));
            dto.setDraweeAccountHolder(safe(cheque.getDraweeAccountHolder()));
            dto.setPayeeName(safe(cheque.getPayeeName()));

            // Resolve checker action for this cheque
            InwardCheckerAction lastAction = resolveLastCheckerAction(cheque);

            String actionValue = (lastAction != null) ? lastAction.getAction()
                                                      : cheque.getStatus();
            dto.setCheckerAction(safe(actionValue));

            // Populate rejection details only for RETURNED cheques
            if ("RETURNED".equalsIgnoreCase(actionValue)) {
                returnedCount++;
                if (lastAction != null) {
                    dto.setReturnReasonCode(safe(lastAction.getReasonCode()));
                    dto.setReturnReasonText(safe(lastAction.getReasonText()));
                    dto.setRemarks(safe(lastAction.getRemarks()));
                }
            } else {
                acceptedCount++;
            }

            rrfCheques.add(dto);
        }

        RrfBatchDto rrfBatch = new RrfBatchDto();
        rrfBatch.setBatchId(batchId);
        rrfBatch.setBatchDate(batchDate);
        rrfBatch.setTotalCheques(cheques.size());
        rrfBatch.setTotalAccepted(acceptedCount);
        rrfBatch.setTotalReturned(returnedCount);
        rrfBatch.setGeneratedAt(generatedAt);
        rrfBatch.setCheques(rrfCheques);

        File outFile = prepareOutputFile(RRF_FOLDER, "RRF_" + batchId + ".xml");
        marshalToFile(RrfBatchDto.class, rrfBatch, outFile);
        return outFile.getAbsolutePath();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  JAXB marshalling
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Marshals a JAXB-annotated DTO to an XML file with formatted (indented) output.
     *
     * @param clazz   the root element class (needed to create JAXBContext)
     * @param payload the populated DTO instance
     * @param file    the target output file (parent dirs must already exist)
     */
    private <T> void marshalToFile(Class<T> clazz, T payload, File file) throws Exception {
        JAXBContext context    = JAXBContext.newInstance(clazz);
        Marshaller  marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.marshal(payload, file);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ensures the target directory exists, then returns a File pointing to the
     * desired output path. The folder path is relative to the JVM working
     * directory (i.e. the web application root on Tomcat).
     */
    private File prepareOutputFile(String folder, String filename) {
        File dir = new File(folder);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                log.warn("prepareOutputFile — could not create directory '{}'", dir.getAbsolutePath());
            }
        }
        return new File(dir, filename);
    }

    /**
     * Returns the most recent InwardCheckerAction on a cheque, or null if none.
     * The actions list is ordered by insertion (natural persistence order), so
     * the last element is the most recent.
     */
    private InwardCheckerAction resolveLastCheckerAction(InwardCheque cheque) {
        List<InwardCheckerAction> actions = cheque.getCheckerActions();
        if (actions == null || actions.isEmpty()) return null;
        return actions.get(actions.size() - 1);
    }

    /**
     * Returns micrCodeCorrected if available, otherwise micrCodeRaw.
     * Always returns a non-null string.
     */
    private String resolveMicrCode(InwardCheque cheque) {
        if (cheque.getMicrCodeCorrected() != null
                && !cheque.getMicrCodeCorrected().trim().isEmpty()) {
            return cheque.getMicrCodeCorrected();
        }
        return safe(cheque.getMicrCodeRaw());
    }

    /**
     * Returns the value if non-null, otherwise an empty string.
     * Prevents null elements inside JAXB XML nodes.
     */
    private String safe(String value) {
        return value != null ? value : "";
    }

    private void validateDateRange(Date fromDate, Date toDate) {
        if (fromDate != null && toDate != null && fromDate.after(toDate)) {
            throw new IllegalArgumentException("From Date cannot be after To Date.");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) return "ALL";
        return status.trim();
    }

    private String trimOrNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return s.trim();
    }
}