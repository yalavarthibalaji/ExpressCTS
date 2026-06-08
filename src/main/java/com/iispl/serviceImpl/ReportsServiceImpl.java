package com.iispl.serviceImpl;

import com.iispl.dao.ReportsDao;
import com.iispl.daoImpl.ReportsDaoImpl;
import com.iispl.service.ReportsService;
import com.iispl.util.JasperPdfGenerator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File    : com/iispl/serviceImpl/ReportsServiceImpl.java
 * Purpose : Orchestrates report generation:
 *             1. Fetch data via ReportsDao
 *             2. Build JasperReports parameters (title, date range, generated-at)
 *             3. Delegate PDF rendering to JasperPdfGenerator
 */
public class ReportsServiceImpl implements ReportsService {

    private final ReportsDao reportsDao = new ReportsDaoImpl();

    private static final DateTimeFormatter D_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Map report type → template path on classpath */
    private static final Map<String, String> TEMPLATES = new HashMap<>();
    /** Map report type → human label */
    private static final Map<String, String> LABELS    = new HashMap<>();
    static {
        TEMPLATES.put("DAILY_SUMMARY",     "/reports/templates/dailySummary.jrxml");
        TEMPLATES.put("BATCH_DETAIL",      "/reports/templates/batchDetail.jrxml");
        TEMPLATES.put("CHECKER_ACTION",    "/reports/templates/checkerAction.jrxml");
        TEMPLATES.put("MAKER_PERFORMANCE", "/reports/templates/makerPerformance.jrxml");
        TEMPLATES.put("REJECTION",         "/reports/templates/rejection.jrxml");

        LABELS.put("DAILY_SUMMARY",     "Daily Outward Summary");
        LABELS.put("BATCH_DETAIL",      "Batch-wise Detail Report");
        LABELS.put("CHECKER_ACTION",    "Checker Action Report");
        LABELS.put("MAKER_PERFORMANCE", "Maker Performance Report");
        LABELS.put("REJECTION",         "Rejection Report");
    }

    @Override
    public String getReportLabel(String reportType) {
        return LABELS.getOrDefault(reportType, "Outward Report");
    }

    @Override
    public byte[] generatePdf(String reportType,
                               LocalDate fromDate,
                               LocalDate toDate) {

        if (reportType == null) {
            System.err.println("ReportsService → reportType is null");
            return new byte[0];
        }
        if (fromDate == null || toDate == null) {
            System.err.println("ReportsService → date range required");
            return new byte[0];
        }
        if (toDate.isBefore(fromDate)) {
            System.err.println("ReportsService → toDate is before fromDate; swapping");
            LocalDate tmp = fromDate; fromDate = toDate; toDate = tmp;
        }

        String template = TEMPLATES.get(reportType);
        if (template == null) {
            System.err.println("ReportsService → unknown reportType: " + reportType);
            return new byte[0];
        }

        // Common parameters every report shows in its header
        Map<String, Object> params = new HashMap<>();
        params.put("REPORT_TITLE", getReportLabel(reportType));
        params.put("FROM_DATE",    fromDate.format(D_FMT));
        params.put("TO_DATE",      toDate.format(D_FMT));
        params.put("GENERATED_AT", java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        try {
            List<?> data = fetchData(reportType, fromDate, toDate);
            if (data == null || data.isEmpty()) {
                System.out.println("ReportsService → " + reportType
                        + " has 0 records in range; generating empty-state PDF");
            }
            return JasperPdfGenerator.generatePdf(template, data, params);

        } catch (Exception e) {
            System.err.println("ReportsService → PDF generation failed for "
                    + reportType + ": " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Calls the DAO method matching the report type.
     */
    private List<?> fetchData(String reportType,
                               LocalDate fromDate,
                               LocalDate toDate) {
        switch (reportType) {
            case "DAILY_SUMMARY":     return reportsDao.getDailySummary(fromDate, toDate);
            case "BATCH_DETAIL":      return reportsDao.getBatchDetails(fromDate, toDate);
            case "CHECKER_ACTION":    return reportsDao.getCheckerActions(fromDate, toDate);
            case "MAKER_PERFORMANCE": return reportsDao.getMakerPerformance(fromDate, toDate);
            case "REJECTION":         return reportsDao.getRejections(fromDate, toDate);
            default:                  return new java.util.ArrayList<>();
        }
    }
}