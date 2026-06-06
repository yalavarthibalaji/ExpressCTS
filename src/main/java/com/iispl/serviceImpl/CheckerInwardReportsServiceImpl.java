package com.iispl.serviceImpl;

import com.iispl.dao.CheckerInwardReportsDao;
import com.iispl.daoImpl.CheckerInwardReportsDaoImpl;
import com.iispl.dto.InwardReportDTO;
import com.iispl.service.CheckerInwardReportsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *   1. Confirms batch status is exactly "Verified" — prevents duplicate processing.
 *   2. Executes debit generation via DAO.
 *   3. Transitions batch status to "CBS_Processed" on success.
 *   4. On failure, status stays "Verified" so the operator can retry.
 */
public class CheckerInwardReportsServiceImpl implements CheckerInwardReportsService {

    private static final Logger log =
            LoggerFactory.getLogger(CheckerInwardReportsServiceImpl.class);

    /** DB status that makes a batch eligible for debit generation. */
    private static final String ELIGIBLE_STATUS   = "Verified";

    /** DB status set after successful debit generation (existing enum value). */
    private static final String COMPLETED_STATUS  = "CBS_Processed";

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
    //  Generate to Debit
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void generateToDebit(String batchId) {

        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID must not be blank.");
        }

        String trimmedId = batchId.trim();
        log.info("generateToDebit — starting for batch '{}'", trimmedId);

        // Guard: batch must be in Verified state
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
            // Execute debit entries
            dao.executeDebitGeneration(trimmedId);

            // Transition to CBS_Processed (Completed)
            dao.updateBatchStatus(trimmedId, COMPLETED_STATUS);
            log.info("generateToDebit — batch '{}' set to {}", trimmedId, COMPLETED_STATUS);

        } catch (IllegalArgumentException e) {
            throw e; // re-throw validation errors as-is
        } catch (Exception e) {
            // Status stays as Verified so the operator can retry
            log.error("generateToDebit — batch '{}' FAILED: {}", trimmedId, e.getMessage(), e);
            throw new RuntimeException(
                    "Debit generation failed for batch '" + trimmedId + "': " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

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