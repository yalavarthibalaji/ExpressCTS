package com.iispl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheckerAction;
import com.iispl.entity.outward.OutwardCheque;

public interface ReportsService {

    // ════════════════════════════════════════════════════════════════
    //  MAKER REPORTS
    // ════════════════════════════════════════════════════════════════

    List<OutwardBatch> getMyBatches(Long makerId);

    byte[] generateMyBatchesReport(List<OutwardBatch> batches,
                                    String             makerName,
                                    LocalDate          fromDate,
                                    LocalDate          toDate,
                                    String             jrxmlPath);

    byte[] generateBatchChequeReport(Long   batchDbId,
                                      String batchId,
                                      String batchStatus,
                                      String makerName,
                                      String jrxmlPath);

    Set<Long> getBatchIdsWithRejections(Long makerId);

    List<OutwardCheque> getMakerRejectedCheques(Long makerId);

    /**
     * Generates a Rejected Cheques PDF for the Maker.
     * Accepts the already-filtered list from the Composer.
     *
     * @param cheques      filtered list of rejected cheques to include
     * @param makerName    full name of the logged-in maker (for PDF header)
     * @param fromDate     display label — shown in PDF header
     * @param toDate       display label — shown in PDF header
     * @param jrxmlPath    real filesystem path to rejectedChequeReport.jrxml
     * @return PDF bytes, or null on failure
     */
    byte[] generateMakerRejectedReport(List<OutwardCheque> cheques,
                                        String              makerName,
                                        LocalDate           fromDate,
                                        LocalDate           toDate,
                                        String              jrxmlPath);

    // ════════════════════════════════════════════════════════════════
    //  CHECKER REPORTS
    // ════════════════════════════════════════════════════════════════

    List<OutwardBatch> getVerifiedBatches(Long checkerId);

    byte[] generateCheckerBatchReport(List<OutwardBatch> batches,
                                       String             checkerName,
                                       LocalDate          fromDate,
                                       LocalDate          toDate,
                                       String             jrxmlPath);

    List<OutwardCheckerAction> getCheckerActionLog(Long checkerId);

    byte[] generateCheckerActionLogReport(List<OutwardCheckerAction> actions,
                                           String                     checkerName,
                                           LocalDate                  fromDate,
                                           LocalDate                  toDate,
                                           String                     jrxmlPath);
}