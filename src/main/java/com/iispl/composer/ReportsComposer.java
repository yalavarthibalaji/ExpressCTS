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
import com.iispl.service.ReportsService;
import com.iispl.serviceImpl.ReportsServiceImpl;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/ReportComposer.java
 *
 * Header "Download PDF" button:
 *   Downloads a PDF of ALL currently filtered batches shown in the table.
 *   No date restriction — applies whatever filters the user has set
 *   (date range, status, batch ID search). If no filters → all batches.
 *
 * Per-row "⬇ PDF" button:
 *   Downloads a Cheque Detail PDF listing every cheque under that batch.
 *   Fetches directly from DB by batchDbId.
 *
 * Table header fix:
 *   CSS class rpt-col-hdr is applied to <columns> in ZUL.
 *   Additional CSS override added in reports.css.
 */
public class ReportsComposer extends SelectorComposer<Component> {

    // ── Pagination ─────────────────────────────────────────────────────
    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;

    // ── Wired components ───────────────────────────────────────────────
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

    // ── Breadcrumb labels (inside included breadcrumb.zul) ─────────────
    @Wire private Label   breadSection;
    @Wire private Label   breadSep2;
    @Wire private Label   breadCurrent;

    // ── Service ────────────────────────────────────────────────────────
    private final ReportsService reportService = new ReportsServiceImpl();

    // ── State ──────────────────────────────────────────────────────────
    private LoginDTO           currentUser;
    private List<OutwardBatch> allBatches;
    private List<OutwardBatch> filteredBatches;

    private static final DateTimeFormatter FMT_DT   = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter FMT_FILE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ──────────────────────────────────────────────────────────────────
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        currentUser = SessionUtil.requireLogin();
        if (currentUser == null) return;

        // ── Breadcrumb: Home › Outward › Reports ─────────────────────
        if (breadSection != null) { breadSection.setValue("Outward"); breadSection.setVisible(true); }
        if (breadSep2    != null)   breadSep2.setVisible(true);
        if (breadCurrent != null)   breadCurrent.setValue("Reports");

        allBatches      = reportService.getMyBatches(currentUser.getUserId());
        filteredBatches = allBatches;
        currentPage     = 0;
        renderTable();
    }

    // ──────────────────────────────────────────────────────────────────
    // Apply Filter
    // ──────────────────────────────────────────────────────────────────
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
        String searchVal = searchBox.getValue() == null ? "" : searchBox.getValue().trim().toUpperCase();

        filteredBatches = allBatches.stream().filter(b -> {
            if (fromDate != null && b.getCreatedAt() != null
                    && b.getCreatedAt().toLocalDate().isBefore(fromDate)) return false;
            if (toDate != null && b.getCreatedAt() != null
                    && b.getCreatedAt().toLocalDate().isAfter(toDate))   return false;
            if (!"ALL".equals(statusVal) && !statusVal.equals(b.getStatus())) return false;
            if (!searchVal.isEmpty() && !b.getBatchId().toUpperCase().contains(searchVal)) return false;
            return true;
        }).collect(Collectors.toList());

        currentPage = 0;
        renderTable();
    }

    // ──────────────────────────────────────────────────────────────────
    // Clear Filter
    // ──────────────────────────────────────────────────────────────────
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

    // ──────────────────────────────────────────────────────────────────
    // Pagination
    // ──────────────────────────────────────────────────────────────────
    @Listen("onClick = #prevBtn")
    public void onPrev() {
        if (currentPage > 0) { currentPage--; renderTable(); }
    }

    @Listen("onClick = #nextBtn")
    public void onNext() {
        if (currentPage < getTotalPages() - 1) { currentPage++; renderTable(); }
    }

    // ──────────────────────────────────────────────────────────────────
    // Header "Download PDF" button
    // Downloads ALL currently filtered batches as a single PDF.
    // No date requirement — uses whatever is in filteredBatches.
    // ──────────────────────────────────────────────────────────────────
    @Listen("onClick = #downloadBtn")
    public void onDownloadAll() {
        errorLabel.setValue("");

        if (filteredBatches == null || filteredBatches.isEmpty()) {
            errorLabel.setValue("No batches to download. Apply filters or wait for data to load.");
            return;
        }

        // Determine the date labels for the PDF header.
        // Use the filter date inputs if set; otherwise derive from the data.
        LocalDate fromDate = toLocalDate(fromDateInput.getValue());
        LocalDate toDate   = toLocalDate(toDateInput.getValue());

        if (fromDate == null || toDate == null) {
            // Derive the actual date range from filtered data for header label
            fromDate = filteredBatches.stream()
                    .filter(b -> b.getCreatedAt() != null)
                    .map(b -> b.getCreatedAt().toLocalDate())
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.now());

            toDate = filteredBatches.stream()
                    .filter(b -> b.getCreatedAt() != null)
                    .map(b -> b.getCreatedAt().toLocalDate())
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now());
        }

        String fileName = "BatchReport_" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";

        pushBatchListPdf(filteredBatches, fromDate, toDate, fileName);
    }

    // ──────────────────────────────────────────────────────────────────
    // Render table rows
    // ──────────────────────────────────────────────────────────────────
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

            // Col 1 — Sr No
            Cell c1 = new Cell();
            c1.setAlign("center");
            Label snoLbl = new Label(String.valueOf(sno++));
            snoLbl.setSclass("sno-cell");
            c1.appendChild(snoLbl);
            row.appendChild(c1);

            // Col 2 — Batch ID
            Cell c2 = new Cell();
            Label batchIdLbl = new Label(b.getBatchId());
            batchIdLbl.setSclass("mono");
            c2.appendChild(batchIdLbl);
            row.appendChild(c2);

            // Col 3 — Cheque count
            Cell c3 = new Cell();
            c3.setAlign("center");
            c3.appendChild(new Label(String.valueOf(b.getChequeCount())));
            row.appendChild(c3);

            // Col 4 — Expected amount
            Cell c4 = new Cell();
            c4.setAlign("right");
            BigDecimal expAmt = b.getExpectedAmount() != null ? b.getExpectedAmount() : BigDecimal.ZERO;
            c4.appendChild(new Label(formatAmount(expAmt)));
            row.appendChild(c4);

            // Col 5 — Actual amount
            Cell c5 = new Cell();
            c5.setAlign("right");
            BigDecimal actAmt = b.getActualAmount() != null ? b.getActualAmount() : BigDecimal.ZERO;
            c5.appendChild(new Label(formatAmount(actAmt)));
            row.appendChild(c5);

            // Col 6 — Status badge
            Cell c6 = new Cell();
            Label statusBadge = new Label(formatStatus(b.getStatus()));
            statusBadge.setSclass(badgeClass(b.getStatus()));
            c6.appendChild(statusBadge);
            row.appendChild(c6);

            // Col 7 — Created at
            Cell c7 = new Cell();
            String createdStr = b.getCreatedAt() != null ? b.getCreatedAt().format(FMT_DT) : "—";
            Label createdLbl = new Label(createdStr);
            createdLbl.setStyle("font-size:12px;");
            c7.appendChild(createdLbl);
            row.appendChild(c7);

            // Col 8 — Per-row Cheque Detail PDF button
            // Clicking this downloads a PDF of ALL cheques under this batch.
            final Long   batchDbId  = b.getId();
            final String batchId    = b.getBatchId();
            final String batchStat  = formatStatus(b.getStatus());

            Cell c8 = new Cell();
            c8.setAlign("center");
            Button dlBtn = new Button("⬇ PDF");
            dlBtn.setSclass("btn bo btn-sm rpt-dl-btn");
            dlBtn.setTooltiptext("Download cheque detail for " + batchId);
            dlBtn.addEventListener("onClick", event ->
                pushChequePdf(batchDbId, batchId, batchStat));
            c8.appendChild(dlBtn);
            row.appendChild(c8);

            batchRows.appendChild(row);
        }

        // Pagination controls
        paginationBar.setVisible(true);
        pageInfoLabel.setValue("Page " + (currentPage + 1) + " of " + totalPages
                + "  (" + totalItems + " batches)");
        prevBtn.setDisabled(currentPage == 0);
        nextBtn.setDisabled(currentPage >= totalPages - 1);
    }

    // ──────────────────────────────────────────────────────────────────
    // Push batch-list PDF (header button)
    // ──────────────────────────────────────────────────────────────────
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

    // ──────────────────────────────────────────────────────────────────
    // Push cheque-detail PDF (per-row button)
    // ──────────────────────────────────────────────────────────────────
    private void pushChequePdf(Long batchDbId, String batchId, String batchStatus) {
        try {
            String jrxmlPath = resolveJrxmlPath("/reports/batchChequeReport.jrxml");
            if (jrxmlPath == null) { errorLabel.setValue("Cheque report template not found."); return; }

            byte[] pdfBytes = reportService.generateBatchChequeReport(
                    batchDbId, batchId, batchStatus,
                    currentUser.getFullName(), jrxmlPath);

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

    // ──────────────────────────────────────────────────────────────────
    // Resolve jrxml path via ServletContext
    // ──────────────────────────────────────────────────────────────────
    private String resolveJrxmlPath(String webPath) {
        try {
            jakarta.servlet.http.HttpServletRequest request =
                    (jakarta.servlet.http.HttpServletRequest)
                    org.zkoss.zk.ui.Executions.getCurrent().getNativeRequest();
            String path = request.getServletContext().getRealPath(webPath);
            System.out.println("ReportComposer → resolved: " + webPath + " → " + path);
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Summary
    // ──────────────────────────────────────────────────────────────────
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

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────
    private int getTotalPages() {
        if (filteredBatches == null || filteredBatches.isEmpty()) return 1;
        return (int) Math.ceil((double) filteredBatches.size() / PAGE_SIZE);
    }

    private LocalDate toLocalDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
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
}