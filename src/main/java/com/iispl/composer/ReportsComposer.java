package com.iispl.composer;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.service.ReportsService;
import com.iispl.serviceImpl.ReportsServiceImpl;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/ReportsComposer.java
 *
 * Two-tab Reports page for Maker Outward:
 *   Tab 1 — My Batches        : all batches created by this maker.
 *   Tab 2 — Rejected Cheques  : all cheques rejected by this maker
 *                               (from MICR Repair or Data Entry screens).
 */
public class ReportsComposer extends SelectorComposer<Component> {

    private static final int PAGE_SIZE = 10;

    // ── Active tab ────────────────────────────────────────────────────
    private String activeTab = "BATCHES";

    // ── Tabs ──────────────────────────────────────────────────────────
    @Wire private Div tabBatches;
    @Wire private Div tabRejected;

    // ── Panels ────────────────────────────────────────────────────────
    @Wire private Div batchPanel;
    @Wire private Div rejectedPanel;

    // ── Batch Tab — wired components ──────────────────────────────────
    @Wire private Textbox fromDateInput;
    @Wire private Textbox toDateInput;
    @Wire private Listbox statusFilter;
    @Wire private Textbox searchBox;
    @Wire private Label   errorLabel;
    @Wire private Label   rowCountLabel;
    @Wire private Label   totalAmountLabel;
    @Wire private Rows    batchRows;
    @Wire private Div     emptyState;
    @Wire private Div     paginationBar;
    @Wire private Button  prevBtn;
    @Wire private Button  nextBtn;
    @Wire private Label   pageInfoLabel;

    // ── Breadcrumb ────────────────────────────────────────────────────
    @Wire private Label breadSection;
    @Wire private Label breadSep2;
    @Wire private Label breadCurrent;

    // ── Rejected Cheques Tab — wired components ───────────────────────
    @Wire private Textbox rjFromDateInput;
    @Wire private Textbox rjToDateInput;
    @Wire private Textbox rjSearchBox;
    @Wire private Label   rjErrorLabel;
    @Wire private Label   rjRowCountLabel;
    @Wire private Rows    rjRows;
    @Wire private Div     rjEmptyState;
    @Wire private Div     rjPaginationBar;
    @Wire private Button  rjPrevBtn;
    @Wire private Button  rjNextBtn;
    @Wire private Label   rjPageInfoLabel;

    // ── Service ───────────────────────────────────────────────────────
    private final ReportsService reportService = new ReportsServiceImpl();

    // ── Batch Tab State ───────────────────────────────────────────────
    private LoginDTO           currentUser;
    private List<OutwardBatch> allBatches;
    private List<OutwardBatch> filteredBatches;
    private int                currentPage = 0;

    // ── Rejected Tab State ────────────────────────────────────────────
    private List<OutwardCheque> allRejectedCheques;
    private List<OutwardCheque> filteredRejectedCheques;
    private int                 rjCurrentPage = 0;

    private static final DateTimeFormatter FMT_DT   = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter FMT_PARSE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ═════════════════════════════════════════════════════════════════
    //  Page Init
    // ═════════════════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        currentUser = SessionUtil.requireLogin();
        if (currentUser == null) return;

        if (breadSection != null) { breadSection.setValue("Outward"); breadSection.setVisible(true); }
        if (breadSep2    != null)   breadSep2.setVisible(true);
        if (breadCurrent != null)   breadCurrent.setValue("Reports");

        allBatches              = reportService.getMyBatches(currentUser.getUserId());
        allRejectedCheques      = reportService.getMakerRejectedCheques(currentUser.getUserId());
        filteredBatches         = allBatches;
        filteredRejectedCheques = allRejectedCheques;

        showTab("BATCHES");
    }

    // ═════════════════════════════════════════════════════════════════
    //  Tab switching
    // ═════════════════════════════════════════════════════════════════

    @Listen("onClick = #tabBatches")
    public void onTabBatches() {
        showTab("BATCHES");
    }

    @Listen("onClick = #tabRejected")
    public void onTabRejected() {
        showTab("REJECTED");
    }

    private void showTab(String tab) {
        activeTab = tab;
        boolean isBatch = "BATCHES".equals(tab);
        tabBatches.setSclass(isBatch  ? "cr-tab cr-tab-active" : "cr-tab");
        tabRejected.setSclass(!isBatch ? "cr-tab cr-tab-active" : "cr-tab");
        batchPanel.setVisible(isBatch);
        rejectedPanel.setVisible(!isBatch);
        if (isBatch) {
            currentPage = 0;
            renderTable();
        } else {
            rjCurrentPage = 0;
            renderRejectedTable();
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  BATCH TAB — Filter
    // ═════════════════════════════════════════════════════════════════

    @Listen("onClick = #applyFilterBtn")
    public void onApplyFilter() {
        doFilter();
    }

    @Listen("onChange = #searchBox")
    public void onSearch() {
        doFilter();
    }

    private void doFilter() {
        errorLabel.setValue("");

        LocalDate fromDate = toLocalDate(fromDateInput.getValue());
        LocalDate toDate   = toLocalDate(toDateInput.getValue());

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            errorLabel.setValue("From Date cannot be after To Date.");
            return;
        }

        String statusVal = getSelectedStatus();
        String searchVal = searchBox.getValue() == null
                ? "" : searchBox.getValue().trim().toUpperCase();

        filteredBatches = allBatches.stream().filter(b -> {
            if (fromDate != null && b.getCreatedAt() != null
                    && b.getCreatedAt().toLocalDate().isBefore(fromDate)) return false;
            if (toDate != null && b.getCreatedAt() != null
                    && b.getCreatedAt().toLocalDate().isAfter(toDate))   return false;
            if (!"ALL".equals(statusVal) && !statusVal.equals(b.getStatus())) return false;
            if (!searchVal.isEmpty()
                    && !b.getBatchId().toUpperCase().contains(searchVal)) return false;
            return true;
        }).collect(Collectors.toList());

        currentPage = 0;
        renderTable();
    }

    @Listen("onClick = #clearFilterBtn")
    public void onClearFilter() {
        fromDateInput.setValue("");
        toDateInput.setValue("");
        statusFilter.setSelectedIndex(0);
        searchBox.setValue("");
        errorLabel.setValue("");
        filteredBatches = allBatches;
        currentPage     = 0;
        renderTable();
    }

    // ═════════════════════════════════════════════════════════════════
    //  BATCH TAB — Pagination
    // ═════════════════════════════════════════════════════════════════

    @Listen("onClick = #prevBtn")
    public void onPrev() {
        if (currentPage > 0) { currentPage--; renderTable(); }
    }

    @Listen("onClick = #nextBtn")
    public void onNext() {
        if (currentPage < getTotalPages() - 1) { currentPage++; renderTable(); }
    }

    // ═════════════════════════════════════════════════════════════════
    //  BATCH TAB — Download PDF
    // ═════════════════════════════════════════════════════════════════

    @Listen("onClick = #downloadBtn")
    public void onDownloadAll() {
        errorLabel.setValue("");

        if (filteredBatches == null || filteredBatches.isEmpty()) {
            errorLabel.setValue("No batches to download.");
            return;
        }

        LocalDate fromDate = toLocalDate(fromDateInput.getValue());
        LocalDate toDate   = toLocalDate(toDateInput.getValue());

        if (fromDate == null || toDate == null) {
            fromDate = filteredBatches.stream()
                    .filter(b -> b.getCreatedAt() != null)
                    .map(b -> b.getCreatedAt().toLocalDate())
                    .min(LocalDate::compareTo).orElse(LocalDate.now());
            toDate = filteredBatches.stream()
                    .filter(b -> b.getCreatedAt() != null)
                    .map(b -> b.getCreatedAt().toLocalDate())
                    .max(LocalDate::compareTo).orElse(LocalDate.now());
        }

        String fileName = "BatchReport_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                + ".pdf";

        pushBatchListPdf(filteredBatches, fromDate, toDate, fileName);
    }

    // ═════════════════════════════════════════════════════════════════
    //  BATCH TAB — Render table
    // ═════════════════════════════════════════════════════════════════

    private void renderTable() {
        batchRows.getChildren().clear();
        updateSummary();

        if (filteredBatches == null || filteredBatches.isEmpty()) {
            emptyState.setVisible(true);
            paginationBar.setVisible(false);
            return;
        }

        emptyState.setVisible(false);

        int totalItems = filteredBatches.size();
        int totalPages = getTotalPages();
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0)           currentPage = 0;

        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, totalItems);

        List<OutwardBatch> pageData = filteredBatches.subList(fromIndex, toIndex);
        int sno = fromIndex + 1;

        for (OutwardBatch b : pageData) {
            Row row = new Row();
            row.setSclass("rpt-row");

            Cell c1 = new Cell(); c1.setAlign("center");
            Label snoLbl = new Label(String.valueOf(sno++));
            snoLbl.setSclass("sno-cell");
            c1.appendChild(snoLbl); row.appendChild(c1);

            Cell c2 = new Cell();
            Label batchIdLbl = new Label(b.getBatchId());
            batchIdLbl.setSclass("mono");
            c2.appendChild(batchIdLbl); row.appendChild(c2);

            Cell c3 = new Cell(); c3.setAlign("center");
            c3.appendChild(new Label(String.valueOf(b.getChequeCount())));
            row.appendChild(c3);

            Cell c4 = new Cell(); c4.setAlign("right");
            c4.appendChild(new Label(formatAmount(
                    b.getExpectedAmount() != null ? b.getExpectedAmount() : BigDecimal.ZERO)));
            row.appendChild(c4);

            Cell c5 = new Cell(); c5.setAlign("right");
            c5.appendChild(new Label(formatAmount(
                    b.getActualAmount() != null ? b.getActualAmount() : BigDecimal.ZERO)));
            row.appendChild(c5);

            Cell c6 = new Cell();
            Label statusBadge = new Label(formatStatus(b.getStatus()));
            statusBadge.setSclass(badgeClass(b.getStatus()));
            c6.appendChild(statusBadge); row.appendChild(c6);

            Cell c7 = new Cell();
            Label createdLbl = new Label(b.getCreatedAt() != null
                    ? b.getCreatedAt().format(FMT_DT) : "—");
            createdLbl.setStyle("font-size:12px;");
            c7.appendChild(createdLbl); row.appendChild(c7);

            final Long   batchDbId  = b.getId();
            final String batchIdStr = b.getBatchId();
            final String batchStat  = formatStatus(b.getStatus());

            Cell c8 = new Cell(); c8.setAlign("center");
            Button dlBtn = new Button("⬇ PDF");
            dlBtn.setSclass("btn bo btn-sm rpt-dl-btn");
            dlBtn.setTooltiptext("Download cheque detail for " + batchIdStr);
            dlBtn.addEventListener("onClick", event ->
                pushChequePdf(batchDbId, batchIdStr, batchStat));
            c8.appendChild(dlBtn); row.appendChild(c8);

            batchRows.appendChild(row);
        }

        paginationBar.setVisible(true);
        pageInfoLabel.setValue("Page " + (currentPage + 1) + " of " + totalPages
                + "  (" + totalItems + " batches)");
        prevBtn.setDisabled(currentPage == 0);
        nextBtn.setDisabled(currentPage >= totalPages - 1);
    }

    // ═════════════════════════════════════════════════════════════════
    //  REJECTED CHEQUES TAB — Filter
    // ═════════════════════════════════════════════════════════════════

    @Listen("onClick = #rjApplyFilterBtn")
    public void onRjApplyFilter() {
        doRejectedFilter();
    }

    @Listen("onChange = #rjSearchBox")
    public void onRjSearch() {
        doRejectedFilter();
    }

    private void doRejectedFilter() {
        rjErrorLabel.setValue("");

        LocalDate fromDate = toLocalDate(rjFromDateInput.getValue());
        LocalDate toDate   = toLocalDate(rjToDateInput.getValue());

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            rjErrorLabel.setValue("From Date cannot be after To Date.");
            return;
        }

        String searchVal = rjSearchBox.getValue() == null
                ? "" : rjSearchBox.getValue().trim().toUpperCase();

        filteredRejectedCheques = allRejectedCheques.stream().filter(c -> {
            // Date filter on rejectedAt
            if (fromDate != null && c.getRejectedAt() != null
                    && c.getRejectedAt().toLocalDate().isBefore(fromDate)) return false;
            if (toDate != null && c.getRejectedAt() != null
                    && c.getRejectedAt().toLocalDate().isAfter(toDate))   return false;
            // Search by batch ID or cheque number
            if (!searchVal.isEmpty()) {
                boolean batchMatch  = c.getBatch() != null
                        && c.getBatch().getBatchId() != null
                        && c.getBatch().getBatchId().toUpperCase().contains(searchVal);
                boolean chequeMatch = c.getChequeNo() != null
                        && c.getChequeNo().toUpperCase().contains(searchVal);
                if (!batchMatch && !chequeMatch) return false;
            }
            return true;
        }).collect(Collectors.toList());

        rjCurrentPage = 0;
        renderRejectedTable();
    }

    @Listen("onClick = #rjClearFilterBtn")
    public void onRjClearFilter() {
        rjFromDateInput.setValue("");
        rjToDateInput.setValue("");
        rjSearchBox.setValue("");
        rjErrorLabel.setValue("");
        filteredRejectedCheques = allRejectedCheques;
        rjCurrentPage           = 0;
        renderRejectedTable();
    }

    // ═════════════════════════════════════════════════════════════════
    //  REJECTED CHEQUES TAB — Pagination
    // ═════════════════════════════════════════════════════════════════

    @Listen("onClick = #rjPrevBtn")
    public void onRjPrev() {
        if (rjCurrentPage > 0) { rjCurrentPage--; renderRejectedTable(); }
    }

    @Listen("onClick = #rjNextBtn")
    public void onRjNext() {
        if (rjCurrentPage < getRjTotalPages() - 1) { rjCurrentPage++; renderRejectedTable(); }
    }

    // ═════════════════════════════════════════════════════════════════
    //  REJECTED CHEQUES TAB — Render table
    // ═════════════════════════════════════════════════════════════════

    private void renderRejectedTable() {
        rjRows.getChildren().clear();
        rjRowCountLabel.setValue("0");

        if (filteredRejectedCheques == null || filteredRejectedCheques.isEmpty()) {
            rjEmptyState.setVisible(true);
            rjPaginationBar.setVisible(false);
            return;
        }

        rjEmptyState.setVisible(false);

        int totalItems = filteredRejectedCheques.size();
        int totalPages = getRjTotalPages();
        if (rjCurrentPage >= totalPages) rjCurrentPage = totalPages - 1;
        if (rjCurrentPage < 0)           rjCurrentPage = 0;

        int fromIndex = rjCurrentPage * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, totalItems);

        List<OutwardCheque> pageData = filteredRejectedCheques.subList(fromIndex, toIndex);
        int sno = fromIndex + 1;

        for (OutwardCheque c : pageData) {
            Row row = new Row();
            row.setSclass("rpt-row");

            // Col 1 — Sr No
            Cell c1 = new Cell(); c1.setAlign("center");
            Label snoLbl = new Label(String.valueOf(sno++));
            snoLbl.setSclass("sno-cell");
            c1.appendChild(snoLbl); row.appendChild(c1);

            // Col 2 — Batch ID
            Cell c2 = new Cell();
            String batchId = (c.getBatch() != null && c.getBatch().getBatchId() != null)
                    ? c.getBatch().getBatchId() : "—";
            Label batchLbl = new Label(batchId);
            batchLbl.setSclass("mono");
            c2.appendChild(batchLbl); row.appendChild(c2);

            // Col 3 — Cheque No
            Cell c3 = new Cell();
            Label chqLbl = new Label(nvl(c.getChequeNo()));
            chqLbl.setSclass("mono");
            c3.appendChild(chqLbl); row.appendChild(c3);

            // Col 4 — Payee Name
            Cell c4 = new Cell();
            Label payeeLbl = new Label(nvl(c.getPayeeName()));
            payeeLbl.setStyle("font-size:12px;");
            c4.appendChild(payeeLbl); row.appendChild(c4);

            // Col 5 — Amount
            Cell c5 = new Cell(); c5.setAlign("right");
            BigDecimal amt = c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO;
            Label amtLbl = new Label(formatAmount(amt));
            amtLbl.setSclass("amt-cell");
            c5.appendChild(amtLbl); row.appendChild(c5);

            // Col 6 — Reason Code
            Cell c6 = new Cell(); c6.setAlign("center");
            Label reasonLbl = new Label(nvl(c.getRejectedReasonCode()));
            reasonLbl.setSclass("chip");
            c6.appendChild(reasonLbl); row.appendChild(c6);

            // Col 7 — Remarks
            Cell c7 = new Cell();
            Label remarksLbl = new Label(nvl(c.getRemarks()));
            remarksLbl.setStyle("font-size:11px; color: var(--tm);");
            c7.appendChild(remarksLbl); row.appendChild(c7);

            // Col 8 — Rejected At
            Cell c8 = new Cell();
            String rejAt = c.getRejectedAt() != null
                    ? c.getRejectedAt().format(FMT_DT) : "—";
            Label rejLbl = new Label(rejAt);
            rejLbl.setStyle("font-size:12px;");
            c8.appendChild(rejLbl); row.appendChild(c8);

            rjRows.appendChild(row);
        }

        rjPaginationBar.setVisible(true);
        rjPageInfoLabel.setValue("Page " + (rjCurrentPage + 1) + " of " + totalPages
                + "  (" + totalItems + " cheques)");
        rjPrevBtn.setDisabled(rjCurrentPage == 0);
        rjNextBtn.setDisabled(rjCurrentPage >= totalPages - 1);
        rjRowCountLabel.setValue(String.valueOf(filteredRejectedCheques.size()));
    }

    // ═════════════════════════════════════════════════════════════════
    //  PDF — Batch list (header download button)
    // ═════════════════════════════════════════════════════════════════

    private void pushBatchListPdf(List<OutwardBatch> batches,
                                   LocalDate fromDate,
                                   LocalDate toDate,
                                   String fileName) {
        try {
            String jrxmlPath = resolveJrxmlPath("/reports/myBatchReport.jrxml");
            if (jrxmlPath == null) { errorLabel.setValue("Report template not found."); return; }

            byte[] pdfBytes = reportService.generateMyBatchesReport(
                    batches, currentUser.getFullName(), fromDate, toDate, jrxmlPath);

            if (pdfBytes == null || pdfBytes.length == 0) {
                errorLabel.setValue("Report generation failed. Please try again.");
                return;
            }
            Filedownload.save(new AMedia(fileName, "pdf", "application/pdf",
                    new ByteArrayInputStream(pdfBytes)));
            errorLabel.setValue("");

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setValue("Report generation failed: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  PDF — Per-row cheque detail
    // ═════════════════════════════════════════════════════════════════

    private void pushChequePdf(Long batchDbId, String batchId, String batchStatus) {
        try {
            String jrxmlPath = resolveJrxmlPath("/reports/batchChequeReport.jrxml");
            if (jrxmlPath == null) { errorLabel.setValue("Cheque report template not found."); return; }

            byte[] pdfBytes = reportService.generateBatchChequeReport(
                    batchDbId, batchId, batchStatus, currentUser.getFullName(), jrxmlPath);

            if (pdfBytes == null || pdfBytes.length == 0) {
                errorLabel.setValue("Cheque report generation failed for batch " + batchId);
                return;
            }
            String fileName = "Cheques_" + batchId.replace("-", "") + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                    + ".pdf";
            Filedownload.save(new AMedia(fileName, "pdf", "application/pdf",
                    new ByteArrayInputStream(pdfBytes)));
            errorLabel.setValue("");

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setValue("Cheque report failed: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════

    private String resolveJrxmlPath(String webPath) {
        try {
            jakarta.servlet.http.HttpServletRequest request =
                    (jakarta.servlet.http.HttpServletRequest)
                    org.zkoss.zk.ui.Executions.getCurrent().getNativeRequest();
            return request.getServletContext().getRealPath(webPath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateSummary() {
        if (filteredBatches == null || filteredBatches.isEmpty()) {
            rowCountLabel.setValue("0");
            totalAmountLabel.setValue("₹ 0.00");
            return;
        }
        BigDecimal grandTotal = filteredBatches.stream().map(b -> {
            BigDecimal act = b.getActualAmount();
            BigDecimal exp = b.getExpectedAmount();
            if (act != null && act.compareTo(BigDecimal.ZERO) > 0) return act;
            if (exp != null) return exp;
            return BigDecimal.ZERO;
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
        rowCountLabel.setValue(String.valueOf(filteredBatches.size()));
        totalAmountLabel.setValue("₹ " + formatAmount(grandTotal));
    }

    private int getTotalPages() {
        if (filteredBatches == null || filteredBatches.isEmpty()) return 1;
        return (int) Math.ceil((double) filteredBatches.size() / PAGE_SIZE);
    }

    private int getRjTotalPages() {
        if (filteredRejectedCheques == null || filteredRejectedCheques.isEmpty()) return 1;
        return (int) Math.ceil((double) filteredRejectedCheques.size() / PAGE_SIZE);
    }

    private LocalDate toLocalDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return LocalDate.parse(s.trim(), FMT_PARSE); }
        catch (Exception e) { return null; }
    }

    private String getSelectedStatus() {
        Listitem sel = statusFilter.getSelectedItem();
        return (sel != null) ? (String) sel.getValue() : "ALL";
    }

    private String formatAmount(BigDecimal val) {
        if (val == null) return "0.00";
        return String.format("%,.2f", val);
    }

    private String formatStatus(String status) {
        if (status == null) return "—";
        switch (status) {
            case "UPLOADED":            return "Uploaded";
            case "NEEDS_REPAIR":        return "Needs Repair";
            case "ENTRY_DONE":          return "Entry Done";
            case "SUBMITTED":           return "Submitted";
            case "CHECKER_IN_PROGRESS": return "Checker In Progress";
            case "CHECKER_APPROVED":    return "Checker Approved";
            case "CHECKER_HOLD":        return "Checker Hold";
            case "REJECTED":            return "Rejected";
            case "REFERRED_BACK":       return "Referred Back";
            default:                    return status;
        }
    }

    private String badgeClass(String status) {
        if (status == null) return "badge b-grey";
        switch (status) {
            case "UPLOADED":            return "badge b-info";
            case "NEEDS_REPAIR":        return "badge b-pend";
            case "ENTRY_DONE":          return "badge b-info";
            case "SUBMITTED":           return "badge b-info";
            case "CHECKER_IN_PROGRESS": return "badge b-pend";
            case "CHECKER_APPROVED":    return "badge b-pass";
            case "CHECKER_HOLD":        return "badge b-ref";
            case "REJECTED":            return "badge b-fail";
            case "REFERRED_BACK":       return "badge b-grey";
            default:                    return "badge b-grey";
        }
    }

    private String nvl(String s) {
        return (s != null && !s.trim().isEmpty()) ? s.trim() : "—";
    }
}