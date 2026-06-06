package com.iispl.service;

import com.iispl.dto.InwardReportDTO;

import java.util.Date;
import java.util.List;

/**
 * File    : com/iispl/service/CheckerInwardReportsService.java
 * Purpose : Service contract for Checker Inward Reports.
 *           Responsibilities:
 *             - Input validation (date range, null guards)
 *             - Delegating to DAO for report queries
 *             - Orchestrating the "Generate to Debit" workflow
 */
public interface CheckerInwardReportsService {

    /**
     * Fetch a page of inward batch report rows matching the supplied filters.
     *
     * @param batchIdSearch partial batch_id search string (null → no filter)
     * @param fromDate      lower bound on batch_date (null → no lower bound)
     * @param toDate        upper bound on batch_date (null → no upper bound)
     * @param status        "ALL" | "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED"
     * @param pageNo        1-based page number
     * @param pageSize      rows per page
     * @return list of matching rows (never null, may be empty)
     * @throws IllegalArgumentException if fromDate is after toDate
     */
    List<InwardReportDTO> getReports(String batchIdSearch,
                                     Date   fromDate,
                                     Date   toDate,
                                     String status,
                                     int    pageNo,
                                     int    pageSize);

    /**
     * Total number of rows matching the same filters — used for pagination.
     *
     * @param batchIdSearch partial batch_id search string (null → no filter)
     * @param fromDate      lower bound on batch_date (null → no lower bound)
     * @param toDate        upper bound on batch_date (null → no upper bound)
     * @param status        "ALL" | "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED"
     * @return total matching row count
     */
    int getTotalCount(String batchIdSearch,
                      Date   fromDate,
                      Date   toDate,
                      String status);

    /**
     * Execute the "Generate to Debit" workflow for the given batch.
     * <p>
     * Steps performed:
     *   1. Validate batchId is not null/blank.
     *   2. Re-fetch the batch and confirm its status is PENDING (guard against duplicates).
     *   3. Transition status to PROCESSING.
     *   4. Invoke debit generation logic (account debit entries, downstream posting).
     *   5. Transition status to COMPLETED on success, FAILED on exception.
     *
     * @param batchId the batch to process
     * @throws IllegalArgumentException if batchId is blank or batch is not in PENDING state
     * @throws RuntimeException         if the debit generation itself fails
     */
    void generateToDebit(String batchId);
}