package com.iispl.service;

import java.time.LocalDate;

/**
 * File    : com/iispl/service/ReportsService.java
 * Purpose : Service interface for the 5 Outward Reports.
 *
 * Report type codes (passed to generatePdf):
 *   "DAILY_SUMMARY"     → Daily Outward Summary
 *   "BATCH_DETAIL"      → Batch-wise Detail Report
 *   "CHECKER_ACTION"    → Checker Action Report
 *   "MAKER_PERFORMANCE" → Maker Performance Report
 *   "REJECTION"         → Rejection Report
 */
public interface ReportsService {

    /**
     * Generate a PDF report.
     *
     * @param reportType  one of the codes listed in the class JavaDoc
     * @param fromDate    inclusive lower bound
     * @param toDate      inclusive upper bound
     * @return PDF bytes — never null; empty array if generation failed
     */
    byte[] generatePdf(String reportType, LocalDate fromDate, LocalDate toDate);

    /**
     * Returns the human label for a report type code (for UI display).
     */
    String getReportLabel(String reportType);
}