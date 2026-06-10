package com.iispl.service;

import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheckerAction;
import java.time.LocalDate;
import java.util.List;

public interface ReportsService {

    // ════════════════════════════════════════════════════════════════
    //  MAKER REPORTS  (existing — do not modify)
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns all batches created by this maker (no date filter).
     * Used to populate the reports table on page load.
     */
    List<OutwardBatch> getMyBatches(Long makerId);

    /**
     * Generates a PDF for a list of batches already filtered by the Composer.
     * The Composer applies all UI filters (date, status, search) in memory
     * and passes the final filtered list here — no re-querying by date.
     *
     * @param batches      the filtered list of batches to include in the PDF
     * @param makerName    full name of the logged-in maker (for PDF header)
     * @param fromDate     display label — the "from" date shown in PDF header
     * @param toDate       display label — the "to" date shown in PDF header
     * @param jrxmlPath    real filesystem path to myBatchReport.jrxml
     * @return PDF bytes, or null on failure
     */
    byte[] generateMyBatchesReport(List<OutwardBatch> batches,
                                    String             makerName,
                                    LocalDate          fromDate,
                                    LocalDate          toDate,
                                    String             jrxmlPath);

    /**
     * Generates a cheque detail PDF for a single batch.
     * Includes all cheques under the batch, ordered by seq_no.
     *
     * @param batchDbId     DB primary key of the batch
     * @param batchId       batch ID string (e.g. B-2026-0606-001) for PDF header
     * @param batchStatus   formatted batch status string for PDF header
     * @param makerName     full name of the logged-in maker
     * @param jrxmlPath     real filesystem path to batchChequeReport.jrxml
     * @return PDF bytes, or null on failure
     */
    byte[] generateBatchChequeReport(Long   batchDbId,
                                      String batchId,
                                      String batchStatus,
                                      String makerName,
                                      String jrxmlPath);

    // ════════════════════════════════════════════════════════════════
    //  CHECKER REPORTS  (new)
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns all batches verified by the logged-in checker.
     *
     * No date filter applied here — the Composer loads all verified
     * batches on page load and applies date/status/search filters
     * in memory, exactly like the Maker Reports pattern.
     *
     * @param checkerId  DB primary key of the logged-in checker user
     * @return list of OutwardBatch, never null (empty list if none)
     */
    List<OutwardBatch> getVerifiedBatches(Long checkerId);

    /**
     * Generates a Verified Batches PDF for the Checker.
     *
     * Accepts the pre-filtered list from the Composer — no re-querying.
     * The Composer applies all UI filters (date, status, search) in memory
     * and passes the final filtered list here.
     *
     * @param batches       the filtered list of verified batches to include
     * @param checkerName   full name of the logged-in checker (for PDF header)
     * @param fromDate      display label — "from" date shown in PDF header
     * @param toDate        display label — "to" date shown in PDF header
     * @param jrxmlPath     real filesystem path to checkerBatchReport.jrxml
     * @return PDF bytes, or null on failure
     */
    byte[] generateCheckerBatchReport(List<OutwardBatch> batches,
                                       String             checkerName,
                                       LocalDate          fromDate,
                                       LocalDate          toDate,
                                       String             jrxmlPath);

    /**
     * Returns all REJECTED and REFERRED checker actions taken by
     * the logged-in checker, across all batches.
     *
     * No date filter applied here — the Composer loads all action log
     * rows on page load and applies filters in memory.
     *
     * @param checkerId  DB primary key of the logged-in checker user
     * @return list of OutwardCheckerAction, never null (empty list if none)
     */
    List<OutwardCheckerAction> getCheckerActionLog(Long checkerId);

    /**
     * Generates a Cheque Action Log PDF for the Checker.
     *
     * Accepts the pre-filtered list of OutwardCheckerAction objects
     * from the Composer. Builds CheckerChequeActionReportRow beans
     * internally and fills the jrxml template.
     *
     * @param actions       the filtered list of checker actions to include
     * @param checkerName   full name of the logged-in checker (for PDF header)
     * @param fromDate      display label — "from" date shown in PDF header
     * @param toDate        display label — "to" date shown in PDF header
     * @param jrxmlPath     real filesystem path to checkerChequeActionReport.jrxml
     * @return PDF bytes, or null on failure
     */
    byte[] generateCheckerActionLogReport(List<OutwardCheckerAction> actions,
                                           String                     checkerName,
                                           LocalDate                  fromDate,
                                           LocalDate                  toDate,
                                           String                     jrxmlPath);
}