package com.iispl.dao;

import com.iispl.dto.reports.*;

import java.time.LocalDate;
import java.util.List;

/**
 * File    : com/iispl/dao/ReportsDao.java
 * Purpose : Read-only DAO for the 5 Outward Reports.
 *           Uses native SQL with date-range filters; returns DTOs (not entities).
 */
public interface ReportsDao {

    /** Aggregated batch stats per day in the given date range (inclusive). */
    List<DailySummaryDto> getDailySummary(LocalDate fromDate, LocalDate toDate);

    /** Every batch created in the date range, with maker + checker names joined in. */
    List<BatchDetailDto> getBatchDetails(LocalDate fromDate, LocalDate toDate);

    /** Per-checker Pass/Reject/Refer counts from outward_checker_actions audit. */
    List<CheckerActionDto> getCheckerActions(LocalDate fromDate, LocalDate toDate);

    /** Per-maker batch and cheque upload counts. */
    List<MakerPerformanceDto> getMakerPerformance(LocalDate fromDate, LocalDate toDate);

    /** Every rejected cheque in the date range (both MAKER + CHECKER rejections). */
    List<RejectionDto> getRejections(LocalDate fromDate, LocalDate toDate);
}