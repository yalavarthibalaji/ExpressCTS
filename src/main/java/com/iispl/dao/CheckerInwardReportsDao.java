package com.iispl.dao;

import com.iispl.dto.InwardReportDTO;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardExport;

import java.util.Date;
import java.util.List;

/**
 * File    : com/iispl/dao/CheckerInwardReportsDao.java
 * Purpose : DAO contract for Checker Inward Reports.
 *           All methods query/update the inward_batch table using Hibernate/HQL.
 *           Results are returned as InwardReportDTO (no ORM entities exposed),
 *           except for findBatchWithChequesAndActions which is used internally
 *           by the service for XML generation.
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
     * @param newStatus the target status (e.g. "CBS_Processed")
     */
    void updateBatchStatus(String batchId, String newStatus);

    /**
     * Execute the core debit generation logic for the given batch.
     * <p>
     * Implementations should create account debit entries and perform any
     * downstream posting required by the CTS workflow.
     * This method is called by the service only after the batch has been
     * confirmed as Verified.
     *
     * @param batchId the batch to process
     * @throws RuntimeException if any DB operation fails
     */
    void executeDebitGeneration(String batchId);

    /**
     * Fetch a fully hydrated InwardBatch including its cheques and all
     * checker actions for each cheque.  Used by the XML generation workflow
     * to build ACK / RRF payloads.
     *
     * @param batchId the batch to load
     * @return the populated InwardBatch, or null if not found
     */
    InwardBatch findBatchWithChequesAndActions(String batchId);

    /**
     * Persist an InwardExport record to the inward_exports table.
     * <p>
     * If a record with the same batch_id and file_type already exists, it is
     * updated (file_name, file_path, status, generated_at) rather than
     * inserting a duplicate row.
     *
     * @param export fully populated InwardExport entity (batch and generatedBy
     *               must be managed or reference-proxied before calling this)
     * @throws RuntimeException if the DB persist/merge fails
     */
    void saveInwardExport(InwardExport export);

    /**
     * Check whether an export record already exists for a given batch and file type.
     *
     * @param batchId  the batch ID string (not the PK)
     * @param fileType e.g. "ACK" or "RRF"
     * @return existing InwardExport, or null if none found
     */
    InwardExport findExportByBatchAndType(String batchId, String fileType);
}