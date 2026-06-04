package com.iispl.serviceImpl;

import com.iispl.dao.CheckerInwardReportsDao;
import com.iispl.daoImpl.CheckerInwardReportsDaoImpl;
import com.iispl.dto.InwardReportDTO;
import com.iispl.service.CheckerInwardReportsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * File    : com/iispl/serviceImpl/CheckerInwardReportsServiceImpl.java
 * Purpose : Concrete implementation of CheckerInwardReportsService.
 *
 *   - Validates date range before calling DAO
 *   - Normalises status filter ("null" → "ALL")
 *   - Builds CXF and BRF XML strings for file download
 *   - All DB access is delegated to CheckerInwardReportsDaoImpl
 */
public class CheckerInwardReportsServiceImpl implements CheckerInwardReportsService {

    private static final Logger log = LoggerFactory.getLogger(CheckerInwardReportsServiceImpl.class);

    private final CheckerInwardReportsDao dao = new CheckerInwardReportsDaoImpl();

    // ─────────────────────────────────────────────────────────────────────────
    //  Query methods
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

        log.info("getReports called — batchId='{}', from={}, to={}, status='{}'",
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
    //  XML builders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a minimal CXF XML (Cheque Transaction File) for the given batch.
     * In production this would aggregate per-cheque records from the DB;
     * for the report module we build a batch-level summary envelope.
     */
    @Override
    public String buildAckXml(InwardReportDTO dto) {
        if (dto == null) return "";

        String generatedOn = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<AcknowledgementFile\n");
        xml.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("    version=\"2.0\">\n");
        xml.append("\n");
        xml.append("  <BatchHeader>\n");
        xml.append("    <BatchId>").append(escapeXml(dto.getBatchId())).append("</BatchId>\n");
        xml.append("    <BatchDate>").append(escapeXml(dto.getBatchDate())).append("</BatchDate>\n");
        xml.append("    <TotalCheques>").append(dto.getTotalCheques()).append("</TotalCheques>\n");
        xml.append("    <MicrErrors>").append(dto.getMicrErrors()).append("</MicrErrors>\n");
        xml.append("    <IqaFails>").append(dto.getIqaFails()).append("</IqaFails>\n");
        xml.append("    <PassedCount>").append(dto.getPassedCount()).append("</PassedCount>\n");
        xml.append("    <RejectedCount>").append(dto.getRejectedCount()).append("</RejectedCount>\n");
        xml.append("    <ReferredCount>").append(dto.getReferredCount()).append("</ReferredCount>\n");
        xml.append("    <Status>").append(escapeXml(dto.getStatus())).append("</Status>\n");
        xml.append("    <GeneratedOn>").append(generatedOn).append("</GeneratedOn>\n");
        xml.append("  </BatchHeader>\n");
        xml.append("\n");
        xml.append("  <!-- Cheque-level records would be populated here in production -->\n");
        xml.append("  <Cheques/>\n");
        xml.append("\n");
        xml.append("</ChequeTransactionFile>\n");

        return xml.toString();
    }

    /**
     * Builds a minimal BRF XML (Bank Response File) for the given batch.
     */
    @Override
    public String buildRrfXml(InwardReportDTO dto) {
        if (dto == null) return "";

        String generatedOn = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<ReturnResponseFile\n");
        xml.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("    version=\"1.0\">\n");
        xml.append("\n");
        xml.append("  <ResponseHeader>\n");
        xml.append("    <BatchId>").append(escapeXml(dto.getBatchId())).append("</BatchId>\n");
        xml.append("    <BatchDate>").append(escapeXml(dto.getBatchDate())).append("</BatchDate>\n");
        xml.append("    <TotalCheques>").append(dto.getTotalCheques()).append("</TotalCheques>\n");
        xml.append("    <AcceptedCount>").append(dto.getPassedCount()).append("</AcceptedCount>\n");
        xml.append("    <RejectedCount>").append(dto.getRejectedCount()).append("</RejectedCount>\n");
        xml.append("    <ReferredCount>").append(dto.getReferredCount()).append("</ReferredCount>\n");
        xml.append("    <BatchStatus>").append(escapeXml(dto.getStatus())).append("</BatchStatus>\n");
        xml.append("    <GeneratedOn>").append(generatedOn).append("</GeneratedOn>\n");
        xml.append("  </ResponseHeader>\n");
        xml.append("\n");
        xml.append("  <!-- Bank response records would be populated here in production -->\n");
        xml.append("  <Responses/>\n");
        xml.append("\n");
        xml.append("</BankResponseFile>\n");

        return xml.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void validateDateRange(Date fromDate, Date toDate) {
        if (fromDate != null && toDate != null && fromDate.after(toDate)) {
            throw new IllegalArgumentException(
                    "From Date cannot be after To Date.");
        }
    }

    /** Return "ALL" when status is null/blank/unrecognised. */
    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) return "ALL";
        return status.trim().toUpperCase();
    }

    private String trimOrNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return s.trim();
    }

    /** Minimal XML character escaping for element text content. */
    private String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&apos;");
    }
}
