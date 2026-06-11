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
 *             - Orchestrating the "Generate to Debit" workflow including
 *               inward_exports persistence after XML file generation
 *
 * CHANGE — generateToDebit now accepts a userId parameter so the
 *          inward_exports.generated_by FK can be populated correctly.
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
     *   1. Validates batchId and userId.
     *   2. Re-fetches the batch and confirms status is "Verified" (duplicate guard).
     *   3. Fetches batch + cheques + checker actions from DB.
     *   4. Generates ACK.xml and saves an inward_exports record (fileType = "ACK").
     *   5. Generates RRF.xml and saves an inward_exports record (fileType = "RRF").
     *   6. Executes legacy debit entry logic.
     *   7. Updates batch status to "CBS_Processed".
     *   8. On any failure, status remains "Verified" so the operator can retry.
     *
     * @param batchId the batch to process
     * @param userId  ID of the currently logged-in Checker user; used to populate
     *                the generated_by column in inward_exports
     * @throws IllegalArgumentException if batchId is blank or batch is not in Verified state
     * @throws RuntimeException         if file generation or DB operations fail
     */
    void generateToDebit(String batchId, Long userId);
}