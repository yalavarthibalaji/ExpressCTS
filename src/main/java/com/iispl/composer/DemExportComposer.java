// File: java/com/iispl/composer/DemExportComposer.java

package com.iispl.composer;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import com.iispl.dto.DemExportResult;
import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardExport;
import com.iispl.service.DemExportService;
import com.iispl.serviceImpl.DemExportServiceImpl;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/DemExportComposer.java
 * Purpose : Drives the DEM Export screen for CHECKER_OUTWARD role.
 *
 * Three views:
 *   1. emptyStateView   — no approved batches AND no exported batches
 *   2. batchListView    — tabs for "Ready for Export" and "Exported"
 *   3. exportDetailView — selected batch's export panel
 *                         (Generate button OR CXF + CIGF file cards)
 *
 * Tab behaviour:
 *   "Ready for Export" tab → shows CHECKER_APPROVED batches only
 *   "Exported"         tab → shows EXPORTED batches only
 *   Status dropdown removed — tab controls which data is shown.
 *
 * Pagination: PAGE_SIZE rows per page, same style as Reports screen.
 *
 * Download uses ZK's Filedownload.save() — no servlet required.
 * Transmit just flips the status to TRANSMITTED (real NPCI integration
 * would happen here in production).
 */
public class DemExportComposer extends SelectorComposer<Component> {

    private final DemExportService  demExportService = new DemExportServiceImpl();
    private final DecimalFormat     moneyFmt = new DecimalFormat("#,##0.00");
    private final DateTimeFormatter dateFmt  = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Topbar ──
    @Wire private Label userAvatar;
    @Wire private Label userName;
    @Wire private Label userRole;

    // ── Three views ──
    @Wire private Div emptyStateView;
    @Wire private Div batchListView;
    @Wire private Div exportDetailView;

    // ── Batch List — tabs and filter bar ──
    @Wire private Button  tabApprovedBtn;
    @Wire private Button  tabExportedBtn;
    @Wire private Textbox listSearchBox;
    @Wire private Label   rowCountLabel;
    @Wire private Label   totalAmountLabel;
    @Wire private Rows    exportBatchRows;
    @Wire private Div     exportEmptyState;

    // ── Pagination ──
    @Wire private Button prevPageBtn;
    @Wire private Button nextPageBtn;
    @Wire private Label  pageInfoLabel;

    // ── Export Detail — summary ──
    @Wire private Label detailBatchBadge;
    @Wire private Label detailBatchId;
    @Wire private Label detailChequeCount;
    @Wire private Label detailAmount;
    @Wire private Label detailApprovedDate;
    @Wire private Label detailVerifiedBy;

    // ── Export Detail — generate area ──
    @Wire private Div    generateArea;
    @Wire private Button generateBtn;

    // ── Export Detail — file cards ──
    @Wire private Div    fileCardsArea;

    @Wire private Label  cxfFileName;
    @Wire private Label  cxfFileSize;
    @Wire private Label  cxfGenTime;
    @Wire private Label  cxfFilePath;
    @Wire private Label  cxfStatusBadge;
    @Wire private Button cxfDownloadBtn;
    @Wire private Button cxfTransmitBtn;

    @Wire private Label  cigfFileName;
    @Wire private Label  cigfFileSize;
    @Wire private Label  cigfGenTime;
    @Wire private Label  cigfFilePath;
    @Wire private Label  cigfStatusBadge;
    @Wire private Button cigfDownloadBtn;
    @Wire private Button cigfTransmitBtn;

    // ── State ──
    private Long                checkerId;
    private OutwardBatch        currentBatch;
    private List<OutwardBatch>  approvedBatches = new ArrayList<>();
    private List<OutwardBatch>  exportedBatches = new ArrayList<>();
    private OutwardExport       currentCxfExport;
    private OutwardExport       currentCigfExport;
    private boolean             showingExportedTab = false;

    // ── Pagination state ──
    private int currentPage            = 1;
    private static final int PAGE_SIZE = 10;

    // ════════════════════════════════════════════════════
    //  Page Init
    // ════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        if (!"CHECKER_OUTWARD".equals(dto.getRoleCode())) {
            Executions.sendRedirect(SessionUtil.getDashboardUrlFor(dto.getRoleCode()));
            return;
        }

        if (userAvatar != null) userAvatar.setValue(dto.getInitials());
        if (userName   != null) userName.setValue(dto.getFullName());
        if (userRole   != null) userRole.setValue("Checker — Outward");
        checkerId = dto.getUserId();

        loadInitialView();
    }

    /**
     * Loads the approved and exported batches, populates the stats row
     * and decides between empty state and the batch list view.
     */
    private void loadInitialView() {
        approvedBatches = demExportService.getExportableBatches();
        exportedBatches = demExportService.getExportedBatches();

        if (approvedBatches.isEmpty() && exportedBatches.isEmpty()) {
            showView("empty");
        } else {
            showView("list");
            renderBatchTable();
        }
    }

    // ════════════════════════════════════════════════════
    //  View Switching
    // ════════════════════════════════════════════════════

    private void showView(String view) {
        emptyStateView.setVisible("empty".equals(view));
        batchListView.setVisible("list".equals(view));
        exportDetailView.setVisible("detail".equals(view));
    }

    // ════════════════════════════════════════════════════
    //  Topbar
    // ════════════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }

    @Listen("onClick = #gotoQueueBtn")
    public void onGotoQueue() {
        Executions.sendRedirect("/outward/checkerQueue/checkerQueue.zul");
    }

    // ════════════════════════════════════════════════════
    //  Tabs (Ready for Export / Exported)
    //  Tab click resets page to 1 and sets sclass to rpt-tab
    //  to match the Reports screen underline style.
    // ════════════════════════════════════════════════════

    @Listen("onClick = #tabApprovedBtn")
    public void onTabApproved() {
        showingExportedTab = false;
        currentPage        = 1;
        tabApprovedBtn.setSclass("rpt-tab active");
        tabExportedBtn.setSclass("rpt-tab");
        renderBatchTable();
    }

    @Listen("onClick = #tabExportedBtn")
    public void onTabExported() {
        showingExportedTab = true;
        currentPage        = 1;
        tabApprovedBtn.setSclass("rpt-tab");
        tabExportedBtn.setSclass("rpt-tab active");
        renderBatchTable();
    }

    // ════════════════════════════════════════════════════
    //  Filter Bar — Apply / Clear / instant search
    // ════════════════════════════════════════════════════

    @Listen("onClick = #applyFilterBtn")
    public void onApplyFilter() {
        currentPage = 1;
        renderBatchTable();
    }

    @Listen("onClick = #clearFilterBtn")
    public void onClearFilter() {
        currentPage = 1;
        listSearchBox.setValue("");
        // Reset the native date inputs via JS
        org.zkoss.zk.ui.util.Clients.evalJavaScript(
            "var f=document.getElementById('fromDateNative'); if(f) f.value='';" +
            "var t=document.getElementById('toDateNative');   if(t) t.value='';"
        );
        renderBatchTable();
    }

    @Listen("onChange = #listSearchBox; onChanging = #listSearchBox")
    public void onSearchChange() {
        currentPage = 1;
        renderBatchTable();
    }

    // ════════════════════════════════════════════════════
    //  Pagination Listeners
    // ════════════════════════════════════════════════════

    @Listen("onClick = #prevPageBtn")
    public void onPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            renderBatchTable();
        }
    }

    @Listen("onClick = #nextPageBtn")
    public void onNextPage() {
        currentPage++;
        renderBatchTable();
    }

    // ════════════════════════════════════════════════════
    //  Render Batch Table
    //  Source list is decided by the active tab:
    //    Ready for Export tab → approvedBatches (CHECKER_APPROVED)
    //    Exported         tab → exportedBatches (EXPORTED)
    //  No status dropdown — tab controls this.
    // ════════════════════════════════════════════════════

    private void renderBatchTable() {
        List<OutwardBatch> source = showingExportedTab
                ? exportedBatches : approvedBatches;

        // ── Read filter values ──
        String search = listSearchBox.getValue() != null
                ? listSearchBox.getValue().trim().toLowerCase() : "";

        LocalDate fromDate = null;
        LocalDate toDate   = null;

        // ── Apply filters ──
        List<OutwardBatch> filtered = new ArrayList<>();
        for (OutwardBatch b : source) {

            // Batch ID search
            String id = b.getBatchId() != null ? b.getBatchId().toLowerCase() : "";
            if (!search.isEmpty() && !id.contains(search)) continue;

            // Date filter — uses verifiedAt (fall back to updatedAt)
            LocalDateTime dt = b.getVerifiedAt() != null
                    ? b.getVerifiedAt() : b.getUpdatedAt();
            if (dt != null) {
                LocalDate batchDate = dt.toLocalDate();
                if (fromDate != null && batchDate.isBefore(fromDate)) continue;
                if (toDate   != null && batchDate.isAfter(toDate))    continue;
            }

            filtered.add(b);
        }

        // ── Update summary strip ──
        rowCountLabel.setValue(String.valueOf(filtered.size()));
        BigDecimal total = BigDecimal.ZERO;
        for (OutwardBatch b : filtered) {
            if (b.getActualAmount() != null) total = total.add(b.getActualAmount());
        }
        totalAmountLabel.setValue("₹ " + moneyFmt.format(total));

        // ── Clear existing rows ──
        exportBatchRows.getChildren().clear();

        // ── Handle empty result ──
        if (filtered.isEmpty()) {
            exportEmptyState.setVisible(true);
            pageInfoLabel.setValue("Page 1 of 1 (0 batches)");
            prevPageBtn.setDisabled(true);
            nextPageBtn.setDisabled(true);
            return;
        }

        exportEmptyState.setVisible(false);

        // ── Pagination calculation ──
        int totalCount = filtered.size();
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);

        // Guard: keep currentPage within valid range
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1)          currentPage = 1;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, totalCount);

        // Update pagination bar
        pageInfoLabel.setValue(
                "Page " + currentPage + " of " + totalPages
                + " (" + totalCount + " batches)");
        prevPageBtn.setDisabled(currentPage <= 1);
        nextPageBtn.setDisabled(currentPage >= totalPages);

        // ── Render rows for current page ──
        int sno = fromIndex + 1;
        for (OutwardBatch b : filtered.subList(fromIndex, toIndex)) {
            exportBatchRows.appendChild(buildBatchRow(b, sno++));
        }
    }

    private Row buildBatchRow(final OutwardBatch batch, int sno) {
        Row row = new Row();

        // Serial number
        Label snoLbl = new Label(String.valueOf(sno));
        snoLbl.setSclass("sno-cell");
        row.appendChild(snoLbl);

        // Batch ID (mono, bold)
        Label batchIdLbl = new Label(safe(batch.getBatchId()));
        batchIdLbl.setSclass("mono fw6");
        row.appendChild(batchIdLbl);

        // Cheques count
        row.appendChild(new Label(String.valueOf(batch.getChequeCount())));

        // Amount (right-aligned mono)
        Label amtLbl = new Label(batch.getActualAmount() != null
                ? "₹" + moneyFmt.format(batch.getActualAmount()) : "—");
        amtLbl.setSclass("amt-cell");
        row.appendChild(amtLbl);

        // Approved date (from verifiedAt; fall back to updatedAt)
        LocalDateTime approvedAt = batch.getVerifiedAt() != null
                ? batch.getVerifiedAt() : batch.getUpdatedAt();
        Label dateLbl = new Label(approvedAt != null
                ? approvedAt.format(dateFmt) : "—");
        dateLbl.setSclass("mono");
        row.appendChild(dateLbl);

        // Verified by
        String verifierName = batch.getVerifiedBy() != null
                ? batch.getVerifiedBy().getFullName() : "—";
        row.appendChild(new Label(verifierName));

        // Status badge
        Label statusBadge = new Label(formatStatusLabel(batch.getStatus()));
        statusBadge.setSclass(resolveStatusBadgeClass(batch.getStatus()));
        row.appendChild(statusBadge);

        // Action button — Export or View Files based on status
        Button actionBtn = new Button();
        if ("CHECKER_APPROVED".equals(batch.getStatus())) {
            actionBtn.setLabel("Export");
            actionBtn.setSclass("btn bp btn-sm");
        } else {
            actionBtn.setLabel("View Files");
            actionBtn.setSclass("btn bo btn-sm");
        }
        actionBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
            @Override public void onEvent(Event e) {
                openDetail(batch);
            }
        });
        row.appendChild(actionBtn);

        return row;
    }

    // ════════════════════════════════════════════════════
    //  Open Detail View
    // ════════════════════════════════════════════════════

    private void openDetail(OutwardBatch batch) {
        currentBatch       = batch;
        currentCxfExport   = null;
        currentCigfExport  = null;

        // Populate summary card
        detailBatchBadge.setValue(safe(batch.getBatchId()));
        detailBatchId.setValue(safe(batch.getBatchId()));
        detailChequeCount.setValue(String.valueOf(batch.getChequeCount()));
        detailAmount.setValue(batch.getActualAmount() != null
                ? "₹" + moneyFmt.format(batch.getActualAmount()) : "—");

        LocalDateTime approvedAt = batch.getVerifiedAt() != null
                ? batch.getVerifiedAt() : batch.getUpdatedAt();
        detailApprovedDate.setValue(approvedAt != null
                ? approvedAt.format(dateFmt) : "—");

        String verifier = batch.getVerifiedBy() != null
                ? batch.getVerifiedBy().getFullName() : "—";
        detailVerifiedBy.setValue(verifier);

        // Branch the bottom area: generate vs file cards
        if ("EXPORTED".equals(batch.getStatus())) {
            generateArea.setVisible(false);
            fileCardsArea.setVisible(true);
            loadExportFiles(batch.getId());
        } else {
            generateArea.setVisible(true);
            fileCardsArea.setVisible(false);
            generateBtn.setLabel("Generate Export Files");
            generateBtn.setDisabled(false);
        }

        showView("detail");
    }

    /**
     * Loads the OutwardExport rows for this batch (1 CXF, 1 CIGF normally)
     * and populates the corresponding file cards.
     */
    private void loadExportFiles(Long batchDbId) {
        List<OutwardExport> exports = demExportService.getExportsForBatch(batchDbId);

        for (OutwardExport exp : exports) {
            if ("CXF".equalsIgnoreCase(exp.getFileType())
                    && currentCxfExport == null) {
                currentCxfExport = exp;
            } else if ("CIGF".equalsIgnoreCase(exp.getFileType())
                    && currentCigfExport == null) {
                currentCigfExport = exp;
            }
        }

        if (currentCxfExport  != null) populateFileCard("CXF",  currentCxfExport);
        if (currentCigfExport != null) populateFileCard("CIGF", currentCigfExport);
    }

    private void populateFileCard(String type, OutwardExport exp) {
        File   file    = new File(exp.getFilePath() != null ? exp.getFilePath() : "");
        String size    = file.exists()
                ? formatFileSize(file.length()) : "File missing on disk";
        String genTime = exp.getGeneratedAt() != null
                ? exp.getGeneratedAt().format(dateFmt) : "—";

        String statusLbl  = exp.getStatus();
        String badgeClass = "TRANSMITTED".equals(exp.getStatus())
                ? "badge b-pass" : "badge b-info";

        if ("CXF".equals(type)) {
            cxfFileName.setValue(safe(exp.getFileName()));
            cxfFileSize.setValue(size);
            cxfGenTime.setValue(genTime);
            cxfFilePath.setValue(safe(exp.getFilePath()));
            cxfStatusBadge.setValue(statusLbl);
            cxfStatusBadge.setSclass(badgeClass);
            cxfDownloadBtn.setDisabled(!file.exists());
            cxfTransmitBtn.setDisabled("TRANSMITTED".equals(exp.getStatus()));
        } else {
            cigfFileName.setValue(safe(exp.getFileName()));
            cigfFileSize.setValue(size);
            cigfGenTime.setValue(genTime);
            cigfFilePath.setValue(safe(exp.getFilePath()));
            cigfStatusBadge.setValue(statusLbl);
            cigfStatusBadge.setSclass(badgeClass);
            cigfDownloadBtn.setDisabled(!file.exists());
            cigfTransmitBtn.setDisabled("TRANSMITTED".equals(exp.getStatus()));
        }
    }

    // ════════════════════════════════════════════════════
    //  Back Button
    // ════════════════════════════════════════════════════

    @Listen("onClick = #backToListBtn")
    public void onBackToList() {
        currentBatch       = null;
        currentCxfExport   = null;
        currentCigfExport  = null;
        loadInitialView();
    }

    // ════════════════════════════════════════════════════
    //  Generate Export
    // ════════════════════════════════════════════════════

    @Listen("onClick = #generateBtn")
    public void onGenerate() {
        if (currentBatch == null) {
            Clients.showNotification("No batch selected.",
                    "warning", null, "top_center", 2000);
            return;
        }

        generateBtn.setLabel("Generating...");
        generateBtn.setDisabled(true);

        DemExportResult result = demExportService.exportBatch(
                currentBatch.getId(), checkerId);

        if (!result.isSuccess()) {
            generateBtn.setLabel("Generate Export Files");
            generateBtn.setDisabled(false);
            Clients.showNotification(
                    "Export failed: " + result.getErrorMessage(),
                    "error", null, "top_center", 4000);
            return;
        }

        Clients.showNotification(
                "✓ Export complete — " + result.getExportedChequeCount()
                + " cheques written to CXF and CIGF.",
                "info", null, "top_center", 3000);

        // Refresh data, then re-open the same batch (now EXPORTED)
        Long batchDbId = currentBatch.getId();
        loadInitialView();

        for (OutwardBatch b : exportedBatches) {
            if (b.getId().equals(batchDbId)) {
                openDetail(b);
                return;
            }
        }
        // Fallback: stay on list view (data already refreshed)
    }

    // ════════════════════════════════════════════════════
    //  Download Buttons (ZK Filedownload — no servlet needed)
    // ════════════════════════════════════════════════════

    @Listen("onClick = #cxfDownloadBtn")
    public void onDownloadCxf() {
        downloadExport(currentCxfExport, "CXF");
    }

    @Listen("onClick = #cigfDownloadBtn")
    public void onDownloadCigf() {
        downloadExport(currentCigfExport, "CIGF");
    }

    private void downloadExport(OutwardExport exp, String type) {
        if (exp == null) {
            Clients.showNotification(type + " export not found.",
                    "warning", null, "top_center", 2000);
            return;
        }
        File file = new File(exp.getFilePath() != null ? exp.getFilePath() : "");
        if (!file.exists() || !file.isFile()) {
            Clients.showNotification(
                    type + " file not found on disk: " + exp.getFilePath(),
                    "error", null, "top_center", 3500);
            return;
        }
        try {
            Filedownload.save(file, "application/xml");
        } catch (Exception e) {
            Clients.showNotification(
                    "Download failed: " + e.getMessage(),
                    "error", null, "top_center", 3000);
        }
    }

    // ════════════════════════════════════════════════════
    //  Transmit Buttons (flip status to TRANSMITTED)
    // ════════════════════════════════════════════════════

    @Listen("onClick = #cxfTransmitBtn")
    public void onTransmitCxf() {
        transmitExport(currentCxfExport, "CXF");
    }

    @Listen("onClick = #cigfTransmitBtn")
    public void onTransmitCigf() {
        transmitExport(currentCigfExport, "CIGF");
    }

    private void transmitExport(OutwardExport exp, String type) {
        if (exp == null) {
            Clients.showNotification(type + " export not found.",
                    "warning", null, "top_center", 2000);
            return;
        }
        boolean ok = demExportService.markTransmitted(exp.getId());
        if (!ok) {
            Clients.showNotification("Failed to mark transmitted.",
                    "error", null, "top_center", 2500);
            return;
        }
        exp.setStatus("TRANSMITTED");
        populateFileCard(type, exp);
        Clients.showNotification(
                "✓ " + type + " marked as transmitted to NPCI.",
                "info", null, "top_center", 2500);
    }

    // ════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════

    private String formatStatusLabel(String status) {
        if (status == null) return "—";
        switch (status) {
            case "CHECKER_APPROVED": return "Approved";
            case "EXPORTED":         return "Exported";
            default:                 return status;
        }
    }

    private String resolveStatusBadgeClass(String status) {
        if (status == null) return "badge b-grey";
        switch (status) {
            case "CHECKER_APPROVED": return "badge b-pass";
            case "EXPORTED":         return "badge b-cbs";
            default:                 return "badge b-grey";
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private String safe(String s) { return s != null ? s : ""; }
}