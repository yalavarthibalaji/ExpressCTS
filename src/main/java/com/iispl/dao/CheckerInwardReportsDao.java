package com.iispl.dao;

import com.iispl.dto.InwardReportDTO;

import java.util.Date;
import java.util.List;

/**
 * File    : com/iispl/dao/CheckerInwardReportsDao.java
 * Purpose : DAO contract for Checker Inward Reports.
 *           All methods query/update the inward_batch table using Hibernate/HQL.
 *           Results are returned as InwardReportDTO (no ORM entities exposed).
 */
public interface CheckerInwardReportsDao {

    /**
     * Fetch inward batch report rows matching the supplied filters.
     *
     * @param batchIdSearch partial/exact batch_id search string (nullable)
     * @param fromDate      lower bound on batch_date (nullable)
     * @param toDate        upper bound on batch_date (nullable)
     * @param status        ALL | PENDING | PROCESSING | COMPLETED | FAILED (nullable → ALL)
     * @param pageNo        1-based page number
     * @param pageSize      number of rows per page
     * @return list of matching InwardReportDTO rows (never null)
     */
    List<InwardReportDTO> findReports(String batchIdSearch,
                                      Date   fromDate,
                                      Date   toDate,
                                      String status,
                                      int    pageNo,
                                      int    pageSize);

    /**
     * Count total rows matching the same filters (used for pagination display).
     *
     * @param batchIdSearch partial/exact batch_id search string (nullable)
     * @param fromDate      lower bound on batch_date (nullable)
     * @param toDate        upper bound on batch_date (nullable)
     * @param status        ALL | PENDING | PROCESSING | COMPLETED | FAILED
     * @return total row count
     */
    int countReports(String batchIdSearch,
                     Date   fromDate,
                     Date   toDate,
                     String status);

    /**
     * Read the current status of a single batch.
     *
     * @param batchId the batch ID to look up
     * @return the status string, or null if the batch is not found
     */
    String getBatchStatus(String batchId);

    /**
     * Update the status of a single batch row.
     *
     * @param batchId   the batch to update
     * @param newStatus the target status (PENDING | PROCESSING | COMPLETED | FAILED)
     */
    void updateBatchStatus(String batchId, String newStatus);

    /**
     * Execute the core debit generation logic for the given batch.
     * <p>
     * Implementations should create account debit entries and perform any
     * downstream posting required by the CTS workflow.
     * This method is called by the service only after the batch has been
     * transitioned to PROCESSING status.
     *
     * @param batchId the batch to process
     * @throws RuntimeException if any DB operation fails
     */
    void executeDebitGeneration(String batchId);
}