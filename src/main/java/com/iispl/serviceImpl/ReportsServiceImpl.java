package com.iispl.serviceImpl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iispl.dao.ReportsDao;
import com.iispl.daoImpl.ReportsDaoImpl;
import com.iispl.dto.reports.BatchReportRow;
import com.iispl.dto.reports.CheckerBatchReportRow;
import com.iispl.dto.reports.CheckerChequeActionReportRow;
import com.iispl.dto.reports.ChequeReportRow;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheckerAction;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.service.BatchUploadService;
import com.iispl.service.ReportsService;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * File    : com/iispl/serviceImpl/ReportsServiceImpl.java
 *
 * Implements five reports:
 *
 *   MAKER
 *   1. generateMyBatchesReport       — Maker's batch list PDF
 *   2. generateBatchChequeReport     — Maker's per-batch cheque detail PDF
 *
 *   CHECKER
 *   3. generateCheckerBatchReport    — Checker's verified batches PDF
 *   4. generateCheckerActionLogReport — Checker's exception audit PDF
 *
 * All PDF generation follows the same pattern:
 *   Composer filters data in memory → passes filtered list here →
 *   service builds DTO rows → fills jrxml → returns PDF bytes.
 */
public class ReportsServiceImpl implements ReportsService {

    private final ReportsDao         reportDao    = new ReportsDaoImpl();
    private final BatchUploadService batchService = new BatchUploadServiceImpl();

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DT_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ════════════════════════════════════════════════════════════════
    //  MAKER REPORTS  (existing — not modified)
    // ════════════════════════════════════════════════════════════════

    @Override
    public List<OutwardBatch> getMyBatches(Long makerId) {
        return batchService.getMyBatches(makerId);
    }

    @Override
    public byte[] generateMyBatchesReport(List<OutwardBatch> batches,
                                           String             makerName,
                                           LocalDate          fromDate,
                                           LocalDate          toDate,
                                           String             jrxmlPath) {
        try {
            System.out.println("ReportsServiceImpl → generateMyBatchesReport → "
                    + batches.size() + " batches for maker=" + makerName);

            List<BatchReportRow> dataList = new ArrayList<>();
            int serialNo = 1;
            for (OutwardBatch batch : batches) {
                dataList.add(new BatchReportRow(
                    serialNo++,
                    nvl(batch.getBatchId(), "-"),
                    batch.getChequeCount(),
                    batch.getExpectedAmount() != null ? batch.getExpectedAmount() : BigDecimal.ZERO,
                    batch.getActualAmount()   != null ? batch.getActualAmount()   : BigDecimal.ZERO,
                    formatStatus(batch.getStatus()),
                    batch.getCreatedAt() != null ? batch.getCreatedAt().format(DT_FORMAT) : "-"
                ));
            }

            String fromLabel = fromDate != null ? fromDate.format(DISPLAY_FORMAT) : "All";
            String toLabel   = toDate   != null ? toDate.format(DISPLAY_FORMAT)   : "All";

            Map<String, Object> params = new HashMap<>();
            params.put("reportTitle",  "My Batches Report");
            params.put("makerName",    makerName != null ? makerName : "-");
            params.put("fromDate",     fromLabel);
            params.put("toDate",       toLabel);
            params.put("totalBatches", batches.size());

            return renderPdf(jrxmlPath, params, dataList);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] generateBatchChequeReport(Long   batchDbId,
                                              String batchId,
                                              String batchStatus,
                                              String makerName,
                                              String jrxmlPath) {
        try {
            List<OutwardCheque> cheques = reportDao.getChequesByBatch(batchDbId);

            System.out.println("ReportsServiceImpl → generateBatchChequeReport → "
                    + "batchId=" + batchId
                    + " cheques=" + cheques.size());

            BigDecimal totalAmount = cheques.stream()
                    .filter(c -> c.getAmount() != null)
                    .map(OutwardCheque::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<ChequeReportRow> dataList = new ArrayList<>();
            int serialNo = 1;
            for (OutwardCheque cheque : cheques) {
                String chequeDate = cheque.getChequeDate() != null
                        ? cheque.getChequeDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                        : "—";

                String micrDisplay = cheque.getMicrCodeCorrected() != null
                        && !cheque.getMicrCodeCorrected().trim().isEmpty()
                        ? cheque.getMicrCodeCorrected()
                        : nvl(cheque.getMicrCode(), "—");

                dataList.add(new ChequeReportRow(
                    serialNo++,
                    nvl(cheque.getChequeNo(),      "—"),
                    nvl(cheque.getAccountNo(),     "—"),
                    nvl(cheque.getAccountHolder(), "—"),
                    nvl(cheque.getPayeeName(),     "—"),
                    cheque.getAmount() != null ? cheque.getAmount() : BigDecimal.ZERO,
                    chequeDate,
                    micrDisplay,
                    formatStatus(cheque.getStatus()),
                    formatRepairStatus(cheque.getRepairStatus())
                ));
            }

            String generatedAt = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));

            Map<String, Object> params = new HashMap<>();
            params.put("batchId",      batchId);
            params.put("makerName",    makerName != null ? makerName : "-");
            params.put("batchStatus",  batchStatus != null ? batchStatus : "-");
            params.put("totalCheques", cheques.size());
            params.put("totalAmount",  totalAmount);
            params.put("generatedAt",  generatedAt);

            return renderPdf(jrxmlPath, params, dataList);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CHECKER REPORTS  (new)
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns all batches verified by the logged-in checker.
     * Delegates directly to DAO — no business logic needed here.
     */
    @Override
    public List<OutwardBatch> getVerifiedBatches(Long checkerId) {
        return reportDao.getVerifiedBatches(checkerId);
    }

    /**
     * Generates the Verified Batches PDF for the Checker (Tab 1).
     *
     * Receives the already-filtered list from CheckerReportsComposer.
     * Builds CheckerBatchReportRow beans and fills checkerBatchReport.jrxml.
     *
     * Key difference from Maker's generateMyBatchesReport:
     *   - Uses makerName  (who created the batch)  instead of expectedAmount
     *   - Uses verifiedAt (when checker approved)   instead of createdAt
     *   - PDF header says "Checker : <name>"        instead of "Maker : <name>"
     */
    @Override
    public byte[] generateCheckerBatchReport(List<OutwardBatch> batches,
                                              String             checkerName,
                                              LocalDate          fromDate,
                                              LocalDate          toDate,
                                              String             jrxmlPath) {
        try {
            System.out.println("ReportsServiceImpl → generateCheckerBatchReport → "
                    + batches.size() + " batches for checker=" + checkerName);

            List<CheckerBatchReportRow> dataList = new ArrayList<>();
            int serialNo = 1;

            for (OutwardBatch batch : batches) {

                // Get maker's full name from the createdBy relationship.
                // createdBy is @ManyToOne on OutwardBatch — loaded via Hibernate.
                String makerName = "-";
                if (batch.getCreatedBy() != null
                        && batch.getCreatedBy().getFullName() != null) {
                    makerName = batch.getCreatedBy().getFullName();
                }

                // Format verifiedAt timestamp
                String verifiedAt = "-";
                if (batch.getVerifiedAt() != null) {
                    verifiedAt = batch.getVerifiedAt().format(DT_FORMAT);
                }

                dataList.add(new CheckerBatchReportRow(
                    serialNo++,
                    nvl(batch.getBatchId(), "-"),
                    batch.getChequeCount(),
                    makerName,
                    batch.getActualAmount() != null ? batch.getActualAmount() : BigDecimal.ZERO,
                    verifiedAt,
                    formatStatus(batch.getStatus())
                ));
            }

            // Compute grand total of actual amounts across all filtered batches
            BigDecimal grandTotal = batches.stream()
                    .filter(b -> b.getActualAmount() != null)
                    .map(OutwardBatch::getActualAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String fromLabel = fromDate != null ? fromDate.format(DISPLAY_FORMAT) : "All";
            String toLabel   = toDate   != null ? toDate.format(DISPLAY_FORMAT)   : "All";

            Map<String, Object> params = new HashMap<>();
            params.put("reportTitle",  "Verified Batches Report");
            params.put("checkerName",  checkerName != null ? checkerName : "-");
            params.put("fromDate",     fromLabel);
            params.put("toDate",       toLabel);
            params.put("totalBatches", batches.size());
            params.put("grandTotal",   grandTotal);

            return renderPdf(jrxmlPath, params, dataList);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns all REJECTED and REFERRED actions by the logged-in checker.
     * Delegates directly to DAO — no business logic needed here.
     */
    @Override
    public List<OutwardCheckerAction> getCheckerActionLog(Long checkerId) {
        return reportDao.getCheckerActionLog(checkerId);
    }

    /**
     * Generates the Cheque Action Log PDF for the Checker (Tab 2).
     *
     * Receives the already-filtered list of OutwardCheckerAction objects
     * from CheckerReportsComposer.
     *
     * For each action row, we access:
     *   action.getOutwardBatch()  → to get the batch ID string
     *   action.getOutwardCheque() → to get cheque_no, payee_name, amount
     *
     * These are lazy-loaded @ManyToOne relationships on OutwardCheckerAction.
     * They are safe to access here because the DAO returns fully-loaded
     * objects within the same Hibernate session lifecycle.
     */
    @Override
    public byte[] generateCheckerActionLogReport(List<OutwardCheckerAction> actions,
                                                  String                     checkerName,
                                                  LocalDate                  fromDate,
                                                  LocalDate                  toDate,
                                                  String                     jrxmlPath) {
        try {
            System.out.println("ReportsServiceImpl → generateCheckerActionLogReport → "
                    + actions.size() + " actions for checker=" + checkerName);

            List<CheckerChequeActionReportRow> dataList = new ArrayList<>();
            int serialNo = 1;

            for (OutwardCheckerAction action : actions) {

                // Safely read batch ID string
                String batchId = "-";
                if (action.getOutwardBatch() != null
                        && action.getOutwardBatch().getBatchId() != null) {
                    batchId = action.getOutwardBatch().getBatchId();
                }

                // Safely read cheque fields
                String     chequeNo  = "-";
                String     payeeName = "-";
                BigDecimal amount    = BigDecimal.ZERO;

                if (action.getOutwardCheque() != null) {
                    chequeNo  = nvl(action.getOutwardCheque().getChequeNo(),  "-");
                    payeeName = nvl(action.getOutwardCheque().getPayeeName(), "-");
                    if (action.getOutwardCheque().getAmount() != null) {
                        amount = action.getOutwardCheque().getAmount();
                    }
                }

                // Format actioned_at timestamp
                String actionedAt = "-";
                if (action.getActionedAt() != null) {
                    actionedAt = action.getActionedAt().format(DT_FORMAT);
                }

                dataList.add(new CheckerChequeActionReportRow(
                    serialNo++,
                    batchId,
                    chequeNo,
                    payeeName,
                    amount,
                    formatAction(action.getAction()),
                    nvl(action.getReasonCode(), "-"),
                    nvl(action.getRemarks(),    "-"),
                    actionedAt
                ));
            }

            String fromLabel = fromDate != null ? fromDate.format(DISPLAY_FORMAT) : "All";
            String toLabel   = toDate   != null ? toDate.format(DISPLAY_FORMAT)   : "All";

            Map<String, Object> params = new HashMap<>();
            params.put("reportTitle",  "Cheque Action Log");
            params.put("checkerName",  checkerName != null ? checkerName : "-");
            params.put("fromDate",     fromLabel);
            params.put("toDate",       toLabel);
            params.put("totalActions", actions.size());

            return renderPdf(jrxmlPath, params, dataList);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  SHARED HELPERS
    // ════════════════════════════════════════════════════════════════

    /**
     * Compiles the jrxml template, fills it with params + data rows,
     * and exports to PDF bytes.
     * Shared by all five report methods above.
     */
    private byte[] renderPdf(String              jrxmlPath,
                              Map<String, Object> params,
                              List<?>             dataList) throws Exception {
        System.out.println("ReportsServiceImpl → loading jrxml: " + jrxmlPath);

        try (InputStream jrxmlStream = new FileInputStream(jrxmlPath)) {
            JasperReport report   = JasperCompileManager.compileReport(jrxmlStream);
            JasperPrint  print    = JasperFillManager.fillReport(
                                        report, params,
                                        new JRBeanCollectionDataSource(dataList));
            byte[]       pdfBytes = JasperExportManager.exportReportToPdf(print);

            System.out.println("ReportsServiceImpl → PDF generated, size=" + pdfBytes.length);
            return pdfBytes;
        }
    }

    private String formatStatus(String status) {
        if (status == null) return "-";
        switch (status) {
            case "UPLOADED":            return "Uploaded";
            case "NEEDS_REPAIR":        return "Needs Repair";
            case "ENTRY_DONE":          return "Entry Done";
            case "SUBMITTED":           return "Submitted";
            case "CHECKER_IN_PROGRESS": return "Checker In Progress";
            case "CHECKER_APPROVED":    return "Checker Approved";
            case "CHECKER_HOLD":        return "Checker Hold";
            case "REJECTED":            return "Rejected";
            case "EXPORTED":            return "Exported";
            case "CHECKER_PASSED":      return "Passed";
            case "CHECKER_REJECTED":    return "Rejected";
            case "CHECKER_REFERRED":    return "Referred";
            case "REFERRED_BACK":       return "Referred Back";
            case "PENDING":             return "Pending";
            default:                    return status;
        }
    }

    private String formatRepairStatus(String repairStatus) {
        if (repairStatus == null) return "-";
        switch (repairStatus) {
            case "NOT_REQUIRED": return "Not Required";
            case "NEEDS_REPAIR": return "Needs Repair";
            case "REPAIRED":     return "Repaired";
            case "REJECTED":     return "Rejected";
            default:             return repairStatus;
        }
    }

    private String formatAction(String action) {
        if (action == null) return "-";
        switch (action) {
            case "REJECTED": return "Rejected";
            case "REFERRED": return "Referred";
            case "PASSED":   return "Passed";
            default:         return action;
        }
    }

    private String nvl(String val, String fallback) {
        return (val != null && !val.trim().isEmpty()) ? val : fallback;
    }
}