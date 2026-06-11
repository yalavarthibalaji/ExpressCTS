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
import com.iispl.entity.outward.OutwardCheckerAction;
import com.iispl.service.ReportsService;
import com.iispl.serviceImpl.ReportsServiceImpl;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/CheckerReportsComposer.java
 *
 * Handles the Checker Outward Reports page — two tabs:
 *
 *   Tab 1 — Verified Batches
 *     Shows all batches the logged-in checker has approved or exported.
 *     Supports date range / status / batch ID search filters.
 *     Header "Download PDF" → PDF of all currently filtered batches.
 *     Per-row "⬇ PDF" → Cheque detail PDF for that batch (same as Maker).
 *
 *   Tab 2 — Cheque Action Log
 *     Shows all REJECTED and REFERRED cheque actions taken by this checker.
 *     Supports date range / action type / batch ID + cheque no search filters.
 *     Header "Download PDF" → PDF of all currently filtered action rows.
 *
 * Pattern is identical to ReportsComposer (Maker):
 *   - All data loaded once on page open from DB.
 *   - All filtering done in memory using Java Streams.
 *   - Paginated at PAGE_SIZE rows per page.
 */
public class CheckerReportsComposer extends SelectorComposer<Component> {

    // ── Pagination ──────────────────────────────────────────────────
    private static final int PAGE_SIZE = 10;

    // Active tab: "BATCHES" or "ACTIONS"
    private String activeTab = "BATCHES";

    // ── Wired — Tabs ────────────────────────────────────────────────
    @Wire private Div  tabBatches;
    @Wire private Div  tabActions;

    // ── Wired — Batch Tab ────────────────────────────────────────────
    @Wire private Div     batchPanel;
    @Wire private Textbox bFromDateInput;
    @Wire private Textbox bToDateInput;
    @Wire private Listbox bStatusFilter;
    @Wire private Textbox bSearchBox;
    @Wire private Label   bErrorLabel;
    @Wire private Label   bRowCountLabel;
    @Wire private Label   bTotalAmountLabel;
    @Wire private Rows    bBatchRows;
    @Wire private Div     bEmptyState;
    @Wire private Div     bPaginationBar;
    @Wire private Button  bPrevBtn;
    @Wire private Button  bNextBtn;
    @Wire private Label   bPageInfoLabel;

    // ── Wired — Action Log Tab ───────────────────────────────────────
    @Wire private Div     actionPanel;
    @Wire private Textbox aFromDateInput;
    @Wire private Textbox aToDateInput;
    @Wire private Listbox aActionFilter;
    @Wire private Textbox aSearchBox;
    @Wire private Label   aErrorLabel;
    @Wire private Label   aRowCountLabel;
    @Wire private Rows    aActionRows;
    @Wire private Div     aEmptyState;
    @Wire private Div     aPaginationBar;
    @Wire private Button  aPrevBtn;
    @Wire private Button  aNextBtn;
    @Wire private Label   aPageInfoLabel;

    // ── Breadcrumb (inside included breadcrumb.zul) ──────────────────
    @Wire private Label breadSection;
    @Wire private Label breadSep2;
    @Wire private Label breadCurrent;

    // ── Service ──────────────────────────────────────────────────────
    private final ReportsService reportsService = new ReportsServiceImpl();

    // ── State — Batch Tab ────────────────────────────────────────────
    private List<OutwardBatch>         allBatches;
    private List<OutwardBatch>         filteredBatches;
    private int                        bCurrentPage = 0;

    // ── State — Action Log Tab ───────────────────────────────────────
    private List<OutwardCheckerAction> allActions;
    private List<OutwardCheckerAction> filteredActions;
    private int                        aCurrentPage = 0;

    
    
    // ── Session ──────────────────────────────────────────────────────
    private LoginDTO currentUser;

    // ── Formatters ───────────────────────────────────────────────────
    private static final DateTimeFormatter FMT_DT   = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FMT_PARSE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ══════════════════════════════════════════════════════════════════
    //  Page init
    // ══════════════════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        currentUser = SessionUtil.requireLogin();
        if (currentUser == null) return;

        // Breadcrumb: Home › Outward › Reports
        if (breadSection != null) { breadSection.setValue("Outward"); breadSection.setVisible(true); }
        if (breadSep2    != null)   breadSep2.setVisible(true);
        if (breadCurrent != null)   breadCurrent.setValue("Reports");

        // Load all data once from DB
        allBatches  = reportsService.getVerifiedBatches(currentUser.getUserId());
        allActions  = reportsService.getCheckerActionLog(currentUser.getUserId());

        filteredBatches = allBatches;
        filteredActions = allActions;

        // Show batch tab by default
        showTab("BATCHES");
    }

    // ══════════════════════════════════════════════════════════════════
    //  Tab switching
    // ══════════════════════════════════════════════════════════════════

    @Listen("onClick = #tabBatches")
    public void onTabBatches() {
        showTab("BATCHES");
    }

    @Listen("onClick = #tabActions")
    public void onTabActions() {
        showTab("ACTIONS");
    }

    private void showTab(String tab) {
        activeTab = tab;

        boolean isBatch = "BATCHES".equals(tab);

        // Update tab header styles
        tabBatches.setSclass(isBatch  ? "cr-tab cr-tab-active" : "cr-tab");
        tabActions.setSclass(!isBatch ? "cr-tab cr-tab-active" : "cr-tab");

        // Show/hide panels
        batchPanel.setVisible(isBatch);
        actionPanel.setVisible(!isBatch);

        // Render the visible panel
        if (isBatch) {
            bCurrentPage = 0;
            renderBatchTable();
        } else {
            aCurrentPage = 0;
            renderActionTable();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  BATCH TAB — Filter
    // ══════════════════════════════════════════════════════════════════

    @Listen("onClick = #bApplyFilterBtn")
    public void onBatchApplyFilter() {
        doBatchFilter();
    }

    @Listen("onChange = #bSearchBox")
    public void onBatchSearch() {
        doBatchFilter();
    }

    private void doBatchFilter() {
        bErrorLabel.setValue("");

        LocalDate fromDate = toLocalDate(bFromDateInput.getValue());
        LocalDate toDate   = toLocalDate(bToDateInput.getValue());

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            bErrorLabel.setValue("From Date cannot be after To Date.");
            return;
        }

        String statusVal = getSelectedValue(bStatusFilter);
        String searchVal = bSearchBox.getValue() == null
                ? "" : bSearchBox.getValue().trim().toUpperCase();

        filteredBatches = allBatches.stream().filter(b -> {
            // Date filter — compare against verifiedAt (not createdAt)
            if (fromDate != null && b.getVerifiedAt() != null
                    && b.getVerifiedAt().toLocalDate().isBefore(fromDate)) return false;
            if (toDate != null && b.getVerifiedAt() != null
                    && b.getVerifiedAt().toLocalDate().isAfter(toDate))   return false;
            // Status filter
            if (!"ALL".equals(statusVal) && !statusVal.equals(b.getStatus())) return false;
            // Search filter
            if (!searchVal.isEmpty()
                    && b.getBatchId() != null
                    && !b.getBatchId().toUpperCase().contains(searchVal)) return false;
            return true;
        }).collect(Collectors.toList());

        bCurrentPage = 0;
        renderBatchTable();
    }

    @Listen("onClick = #bClearFilterBtn")
    public void onBatchClearFilter() {
        bFromDateInput.setValue("");
        bToDateInput.setValue("");
        bStatusFilter.setSelectedIndex(0);
        bSearchBox.setValue("");
        bErrorLabel.setValue("");
        filteredBatches = allBatches;
        bCurrentPage    = 0;
        renderBatchTable();
    }

    // ══════════════════════════════════════════════════════════════════
    //  BATCH TAB — Pagination
    // ══════════════════════════════════════════════════════════════════

    @Listen("onClick = #bPrevBtn")
    public void onBatchPrev() {
        if (bCurrentPage > 0) { bCurrentPage--; renderBatchTable(); }
    }

    @Listen("onClick = #bNextBtn")
    public void onBatchNext() {
        if (bCurrentPage < getBatchTotalPages() - 1) { bCurrentPage++; renderBatchTable(); }
    }

    // ══════════════════════════════════════════════════════════════════
    //  BATCH TAB — Download PDF (header button)
    // ══════════════════════════════════════════════════════════════════

    @Listen("onClick = #bDownloadBtn")
    public void onBatchDownload() {
        bErrorLabel.setValue("");

        if (filteredBatches == null || filteredBatches.isEmpty()) {
            bErrorLabel.setValue("No batches to download. Apply filters or wait for data to load.");
            return;
        }

        // Use filter date inputs for PDF header label; derive from data if not set
        LocalDate fromDate = toLocalDate(bFromDateInput.getValue());
        LocalDate toDate   = toLocalDate(bToDateInput.getValue());

        if (fromDate == null || toDate == null) {
            fromDate = filteredBatches.stream()
                    .filter(b -> b.getVerifiedAt() != null)
                    .map(b -> b.getVerifiedAt().toLocalDate())
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.now());
            toDate = filteredBatches.stream()
                    .filter(b -> b.getVerifiedAt() != null)
                    .map(b -> b.getVerifiedAt().toLocalDate())
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now());
        }

        String fileName = "VerifiedBatchReport_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                + ".pdf";

        try {
            String jrxmlPath = resolveJrxmlPath("/reports/checkerBatchReport.jrxml");
            if (jrxmlPath == null) { bErrorLabel.setValue("Report template not found."); return; }

            byte[] pdfBytes = reportsService.generateCheckerBatchReport(
                    filteredBatches, currentUser.getFullName(), fromDate, toDate, jrxmlPath);

            if (pdfBytes == null || pdfBytes.length == 0) {
                bErrorLabel.setValue("Report generation failed. Please try again.");
                return;
            }

            Filedownload.save(new AMedia(fileName, "pdf", "application/pdf",
                    new ByteArrayInputStream(pdfBytes)));
            bErrorLabel.setValue("");

        } catch (Exception e) {
            e.printStackTrace();
            bErrorLabel.setValue("Report generation failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  BATCH TAB — Render table
    // ══════════════════════════════════════════════════════════════════

    private void renderBatchTable() {
        bBatchRows.getChildren().clear();
        updateBatchSummary();

        if (filteredBatches == null || filteredBatches.isEmpty()) {
            bEmptyState.setVisible(true);
            bPaginationBar.setVisible(false);
            return;
        }

        bEmptyState.setVisible(false);

        int totalItems = filteredBatches.size();
        int totalPages = getBatchTotalPages();

        if (bCurrentPage >= totalPages) bCurrentPage = totalPages - 1;
        if (bCurrentPage < 0)           bCurrentPage = 0;

        int fromIndex = bCurrentPage * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, totalItems);

        List<OutwardBatch> pageData = filteredBatches.subList(fromIndex, toIndex);
        int sno = fromIndex + 1;

        for (OutwardBatch b : pageData) {
            Row row = new Row();
            row.setSclass("rpt-row");

            // Col 1 — Sr No
            Cell c1 = new Cell(); c1.setAlign("center");
            Label snoLbl = new Label(String.valueOf(sno++));
            snoLbl.setSclass("sno-cell");
            c1.appendChild(snoLbl); row.appendChild(c1);

            // Col 2 — Batch ID
            Cell c2 = new Cell();
            Label batchIdLbl = new Label(nvl(b.getBatchId(), "—"));
            batchIdLbl.setSclass("mono");
            c2.appendChild(batchIdLbl); row.appendChild(c2);

            // Col 3 — Cheques
            Cell c3 = new Cell(); c3.setAlign("center");
            c3.appendChild(new Label(String.valueOf(b.getChequeCount())));
            row.appendChild(c3);

            // Col 4 — Maker name
            Cell c4 = new Cell();
            String makerName = (b.getCreatedBy() != null && b.getCreatedBy().getFullName() != null)
                    ? b.getCreatedBy().getFullName() : "—";
            Label makerLbl = new Label(makerName);
            makerLbl.setStyle("font-size:12px;");
            c4.appendChild(makerLbl); row.appendChild(c4);

            // Col 5 — Actual amount
            Cell c5 = new Cell(); c5.setAlign("right");
            BigDecimal actAmt = b.getActualAmount() != null ? b.getActualAmount() : BigDecimal.ZERO;
            Label amtLbl = new Label(formatAmount(actAmt));
            amtLbl.setSclass("amt-cell");
            c5.appendChild(amtLbl); row.appendChild(c5);

            // Col 6 — Verified at
            Cell c6 = new Cell();
            String verifiedStr = b.getVerifiedAt() != null ? b.getVerifiedAt().format(FMT_DT) : "—";
            Label verifiedLbl = new Label(verifiedStr);
            verifiedLbl.setStyle("font-size:12px;");
            c6.appendChild(verifiedLbl); row.appendChild(c6);

            // Col 7 — Status badge
            Cell c7 = new Cell();
            Label statusBadge = new Label(formatStatus(b.getStatus()));
            statusBadge.setSclass(badgeClass(b.getStatus()));
            c7.appendChild(statusBadge); row.appendChild(c7);

            // Col 8 — Per-row PDF (Cheque Detail PDF — same as maker reports)
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

            bBatchRows.appendChild(row);
        }

        // Pagination controls
        bPaginationBar.setVisible(true);
        bPageInfoLabel.setValue("Page " + (bCurrentPage + 1) + " of " + totalPages
                + "  (" + totalItems + " batches)");
        bPrevBtn.setDisabled(bCurrentPage == 0);
        bNextBtn.setDisabled(bCurrentPage >= totalPages - 1);
    }

    // ══════════════════════════════════════════════════════════════════
    //  ACTION LOG TAB — Filter
    // ══════════════════════════════════════════════════════════════════

    @Listen("onClick = #aApplyFilterBtn")
    public void onActionApplyFilter() {
        doActionFilter();
    }

    @Listen("onChange = #aSearchBox")
    public void onActionSearch() {
        doActionFilter();
    }

    private void doActionFilter() {
        aErrorLabel.setValue("");

        LocalDate fromDate = toLocalDate(aFromDateInput.getValue());
        LocalDate toDate   = toLocalDate(aToDateInput.getValue());

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            aErrorLabel.setValue("From Date cannot be after To Date.");
            return;
        }

        String actionVal = getSelectedValue(aActionFilter);
        String searchVal = aSearchBox.getValue() == null
                ? "" : aSearchBox.getValue().trim().toUpperCase();

        filteredActions = allActions.stream().filter(a -> {
            // Date filter — compare against actionedAt
            if (fromDate != null && a.getActionedAt() != null
                    && a.getActionedAt().toLocalDate().isBefore(fromDate)) return false;
            if (toDate != null && a.getActionedAt() != null
                    && a.getActionedAt().toLocalDate().isAfter(toDate))   return false;
            // Action type filter
            if (!"ALL".equals(actionVal) && !actionVal.equals(a.getAction())) return false;
            // Search: matches batch ID or cheque number
            if (!searchVal.isEmpty()) {
                boolean batchMatch  = a.getOutwardBatch()  != null
                        && a.getOutwardBatch().getBatchId()  != null
                        && a.getOutwardBatch().getBatchId().toUpperCase().contains(searchVal);
                boolean chequeMatch = a.getOutwardCheque() != null
                        && a.getOutwardCheque().getChequeNo() != null
                        && a.getOutwardCheque().getChequeNo().toUpperCase().contains(searchVal);
                if (!batchMatch && !chequeMatch) return false;
            }
            return true;
        }).collect(Collectors.toList());

        aCurrentPage = 0;
        renderActionTable();
    }

    @Listen("onClick = #aClearFilterBtn")
    public void onActionClearFilter() {
        aFromDateInput.setValue("");
        aToDateInput.setValue("");
        aActionFilter.setSelectedIndex(0);
        aSearchBox.setValue("");
        aErrorLabel.setValue("");
        filteredActions = allActions;
        aCurrentPage    = 0;
        renderActionTable();
    }

    // ══════════════════════════════════════════════════════════════════
    //  ACTION LOG TAB — Pagination
    // ══════════════════════════════════════════════════════════════════

    @Listen("onClick = #aPrevBtn")
    public void onActionPrev() {
        if (aCurrentPage > 0) { aCurrentPage--; renderActionTable(); }
    }

    @Listen("onClick = #aNextBtn")
    public void onActionNext() {
        if (aCurrentPage < getActionTotalPages() - 1) { aCurrentPage++; renderActionTable(); }
    }

    // ══════════════════════════════════════════════════════════════════
    //  ACTION LOG TAB — Download PDF (header button)
    // ══════════════════════════════════════════════════════════════════

    @Listen("onClick = #aDownloadBtn")
    public void onActionDownload() {
        aErrorLabel.setValue("");

        if (filteredActions == null || filteredActions.isEmpty()) {
            aErrorLabel.setValue("No action records to download. Apply filters or wait for data.");
            return;
        }

        LocalDate fromDate = toLocalDate(aFromDateInput.getValue());
        LocalDate toDate   = toLocalDate(aToDateInput.getValue());

        if (fromDate == null || toDate == null) {
            fromDate = filteredActions.stream()
                    .filter(a -> a.getActionedAt() != null)
                    .map(a -> a.getActionedAt().toLocalDate())
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.now());
            toDate = filteredActions.stream()
                    .filter(a -> a.getActionedAt() != null)
                    .map(a -> a.getActionedAt().toLocalDate())
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now());
        }

        String fileName = "ChequeActionLog_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                + ".pdf";

        try {
            String jrxmlPath = resolveJrxmlPath("/reports/checkerChequeActionReport.jrxml");
            if (jrxmlPath == null) { aErrorLabel.setValue("Report template not found."); return; }

            byte[] pdfBytes = reportsService.generateCheckerActionLogReport(
                    filteredActions, currentUser.getFullName(), fromDate, toDate, jrxmlPath);

            if (pdfBytes == null || pdfBytes.length == 0) {
                aErrorLabel.setValue("Report generation failed. Please try again.");
                return;
            }

            Filedownload.save(new AMedia(fileName, "pdf", "application/pdf",
                    new ByteArrayInputStream(pdfBytes)));
            aErrorLabel.setValue("");

        } catch (Exception e) {
            e.printStackTrace();
            aErrorLabel.setValue("Report generation failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  ACTION LOG TAB — Render table
    // ══════════════════════════════════════════════════════════════════

    private void renderActionTable() {
        aActionRows.getChildren().clear();
        updateActionSummary();

        if (filteredActions == null || filteredActions.isEmpty()) {
            aEmptyState.setVisible(true);
            aPaginationBar.setVisible(false);
            return;
        }

        aEmptyState.setVisible(false);

        int totalItems = filteredActions.size();
        int totalPages = getActionTotalPages();

        if (aCurrentPage >= totalPages) aCurrentPage = totalPages - 1;
        if (aCurrentPage < 0)           aCurrentPage = 0;

        int fromIndex = aCurrentPage * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, totalItems);

        List<OutwardCheckerAction> pageData = filteredActions.subList(fromIndex, toIndex);
        int sno = fromIndex + 1;

        for (OutwardCheckerAction a : pageData) {
            Row row = new Row();
            row.setSclass("rpt-row");

            // Col 1 — Sr No
            Cell c1 = new Cell(); c1.setAlign("center");
            Label snoLbl = new Label(String.valueOf(sno++));
            snoLbl.setSclass("sno-cell");
            c1.appendChild(snoLbl); row.appendChild(c1);

            // Col 2 — Batch ID
            Cell c2 = new Cell();
            String batchId = (a.getOutwardBatch() != null && a.getOutwardBatch().getBatchId() != null)
                    ? a.getOutwardBatch().getBatchId() : "—";
            Label batchLbl = new Label(batchId);
            batchLbl.setSclass("mono");
            c2.appendChild(batchLbl); row.appendChild(c2);

            // Col 3 — Cheque No
            Cell c3 = new Cell();
            String chequeNo = (a.getOutwardCheque() != null && a.getOutwardCheque().getChequeNo() != null)
                    ? a.getOutwardCheque().getChequeNo() : "—";
            Label chqLbl = new Label(chequeNo);
            chqLbl.setSclass("mono");
            c3.appendChild(chqLbl); row.appendChild(c3);

            // Col 4 — Payee Name
            Cell c4 = new Cell();
            String payeeName = (a.getOutwardCheque() != null && a.getOutwardCheque().getPayeeName() != null)
                    ? a.getOutwardCheque().getPayeeName() : "—";
            Label payeeLbl = new Label(payeeName);
            payeeLbl.setStyle("font-size:12px;");
            c4.appendChild(payeeLbl); row.appendChild(c4);

            // Col 5 — Amount
            Cell c5 = new Cell(); c5.setAlign("right");
            BigDecimal amt = (a.getOutwardCheque() != null && a.getOutwardCheque().getAmount() != null)
                    ? a.getOutwardCheque().getAmount() : BigDecimal.ZERO;
            Label amtLbl = new Label(formatAmount(amt));
            amtLbl.setSclass("amt-cell");
            c5.appendChild(amtLbl); row.appendChild(c5);

            // Col 6 — Action badge
            Cell c6 = new Cell(); c6.setAlign("center");
            Label actionBadge = new Label(formatAction(a.getAction()));
            actionBadge.setSclass(actionBadgeClass(a.getAction()));
            c6.appendChild(actionBadge); row.appendChild(c6);

            // Col 7 — Reason Code
            Cell c7 = new Cell(); c7.setAlign("center");
            Label reasonLbl = new Label(nvl(a.getReasonCode(), "—"));
            reasonLbl.setSclass("chip");
            c7.appendChild(reasonLbl); row.appendChild(c7);

            // Col 8 — Remarks
            Cell c8 = new Cell();
            Label remarksLbl = new Label(nvl(a.getRemarks(), "—"));
            remarksLbl.setStyle("font-size:11px; color: var(--tm);");
            c8.appendChild(remarksLbl); row.appendChild(c8);

            // Col 9 — Actioned At
            Cell c9 = new Cell();
            String actionedAt = a.getActionedAt() != null ? a.getActionedAt().format(FMT_DT) : "—";
            Label actionedLbl = new Label(actionedAt);
            actionedLbl.setStyle("font-size:12px;");
            c9.appendChild(actionedLbl); row.appendChild(c9);

            aActionRows.appendChild(row);
        }

        // Pagination
        aPaginationBar.setVisible(true);
        aPageInfoLabel.setValue("Page " + (aCurrentPage + 1) + " of " + totalPages
                + "  (" + totalItems + " actions)");
        aPrevBtn.setDisabled(aCurrentPage == 0);
        aNextBtn.setDisabled(aCurrentPage >= totalPages - 1);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Per-row Cheque Detail PDF (reuses Maker's batchChequeReport.jrxml)
    // ══════════════════════════════════════════════════════════════════

    private void pushChequePdf(Long batchDbId, String batchId, String batchStatus) {
        try {
            String jrxmlPath = resolveJrxmlPath("/reports/batchChequeReport.jrxml");
            if (jrxmlPath == null) { bErrorLabel.setValue("Cheque report template not found."); return; }

            byte[] pdfBytes = reportsService.generateBatchChequeReport(
                    batchDbId, batchId, batchStatus, currentUser.getFullName(), jrxmlPath);

            if (pdfBytes == null || pdfBytes.length == 0) {
                bErrorLabel.setValue("Cheque report generation failed for batch " + batchId);
                return;
            }

            String fileName = "Cheques_" + batchId.replace("-", "") + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                    + ".pdf";

            Filedownload.save(new AMedia(fileName, "pdf", "application/pdf",
                    new ByteArrayInputStream(pdfBytes)));
            bErrorLabel.setValue("");

        } catch (Exception e) {
            e.printStackTrace();
            bErrorLabel.setValue("Cheque report failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Summary strips
    // ══════════════════════════════════════════════════════════════════

    private void updateBatchSummary() {
        if (filteredBatches == null || filteredBatches.isEmpty()) {
            bRowCountLabel.setValue("0");
            bTotalAmountLabel.setValue("₹ 0.00");
            return;
        }
        BigDecimal grandTotal = filteredBatches.stream()
                .filter(b -> b.getActualAmount() != null)
                .map(OutwardBatch::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        bRowCountLabel.setValue(String.valueOf(filteredBatches.size()));
        bTotalAmountLabel.setValue("₹ " + formatAmount(grandTotal));
    }

    private void updateActionSummary() {
        if (filteredActions == null || filteredActions.isEmpty()) {
            aRowCountLabel.setValue("0");
            return;
        }
        aRowCountLabel.setValue(String.valueOf(filteredActions.size()));
    }

    // ══════════════════════════════════════════════════════════════════
    //  Resolve jrxml path via ServletContext
    // ══════════════════════════════════════════════════════════════════

    private String resolveJrxmlPath(String webPath) {
        try {
            jakarta.servlet.http.HttpServletRequest request =
                    (jakarta.servlet.http.HttpServletRequest)
                    org.zkoss.zk.ui.Executions.getCurrent().getNativeRequest();
            String path = request.getServletContext().getRealPath(webPath);
            System.out.println("CheckerReportsComposer → resolved: " + webPath + " → " + path);
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════

    private int getBatchTotalPages() {
        if (filteredBatches == null || filteredBatches.isEmpty()) return 1;
        return (int) Math.ceil((double) filteredBatches.size() / PAGE_SIZE);
    }

    private int getActionTotalPages() {
        if (filteredActions == null || filteredActions.isEmpty()) return 1;
        return (int) Math.ceil((double) filteredActions.size() / PAGE_SIZE);
    }

    private LocalDate toLocalDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(s.trim(), FMT_PARSE);
        } catch (Exception e) {
            return null;
        }
    }

    private String getSelectedValue(Listbox listbox) {
        Listitem sel = listbox.getSelectedItem();
        return (sel != null) ? (String) sel.getValue() : "ALL";
    }

    private String formatAmount(BigDecimal val) {
        if (val == null) return "0.00";
        return String.format("%,.2f", val);
    }

    private String formatStatus(String status) {
        if (status == null) return "—";
        switch (status) {
            case "CHECKER_APPROVED": return "Checker Approved";
            case "EXPORTED":         return "Exported";
            default:                 return status;
        }
    }

    private String formatAction(String action) {
        if (action == null) return "—";
        switch (action) {
            case "REJECTED": return "Rejected";
            default:         return action;
        }
    }

    private String badgeClass(String status) {
        if (status == null) return "badge b-grey";
        switch (status) {
            case "CHECKER_APPROVED": return "badge b-pass";
            case "EXPORTED":         return "badge b-exp";
            default:                 return "badge b-grey";
        }
    }

    private String actionBadgeClass(String action) {
        if (action == null) return "badge b-grey";
        switch (action) {
            case "REJECTED": return "badge b-fail";
            default:         return "badge b-grey";
        }
    }

    private String nvl(String val, String fallback) {
        return (val != null && !val.trim().isEmpty()) ? val : fallback;
    }
}