// File: java/com/iispl/composer/CheckerOutwardDashboardComposer.java

package com.iispl.composer;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.service.CheckerService;
import com.iispl.serviceImpl.CheckerServiceImpl;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/CheckerOutwardDashboardComposer.java
 * Purpose : Checker Outward Dashboard.
 *           KPI strip + 3 quick action cards + Recent Batches panel.
 *           Filter bar uses same fbar pattern as DEM Export / Verification Queue.
 */
public class CheckerOutwardDashboardComposer extends SelectorComposer<Component> {

    private final CheckerService checkerService = new CheckerServiceImpl();

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter ROW_FMT   = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat     MONEY_FMT = new DecimalFormat("#,##0.00");
    private static final int               PAGE_SIZE  = 10;

    private List<OutwardBatch> allBatches  = new ArrayList<>();
    private int                currentPage = 1;

    // ── Page Header ───────────────────────────────────────────────
    @Wire private Label lblTodayDate;
  

    // ── KPI Labels ────────────────────────────────────────────────
    @Wire private Label kpiPending;   // Awaiting Review  = SUBMITTED + CHECKER_IN_PROGRESS
    @Wire private Label kpiHold;      // On Hold          = CHECKER_HOLD
    @Wire private Label kpiReady;     // Ready to Export  = CHECKER_APPROVED

    // ── Quick Action Buttons ──────────────────────────────────────
    @Wire private Button gotoVerificationQueue;
    @Wire private Button gotoDemExport;
    @Wire private Button gotoReports;
    @Wire private Button gotoVerificationQueueSmall;

    // ── Filter Bar — fbar pattern (same as DEM Export) ───────────
    @Wire private Textbox filterFromDateInput;   // hidden textbox bound to native date input
    @Wire private Textbox filterToDateInput;     // hidden textbox bound to native date input
    @Wire private Listbox filterStatus;          // select listbox
    @Wire private Textbox filterBatchId;         // instant search textbox
    @Wire private Button  btnApplyFilter;
    @Wire private Button  btnClearFilter;

    // ── Batch Table ───────────────────────────────────────────────
    @Wire private Listbox recentBatchList;
    @Wire private Label   emptyBatchesMsg;

    // ── Pagination ────────────────────────────────────────────────
    @Wire private org.zkoss.zul.Div batchPager;
    @Wire private Button btnPrevPage;
    @Wire private Label  pagerInfo;
    @Wire private Button btnNextPage;

    // ════════════════════════════════════════════════════════════════
    //  Page Init
    // ════════════════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        if (!("CHECKER_OUTWARD".equals(dto.getRoleCode()))) {
            Executions.sendRedirect(SessionUtil.getDashboardUrlFor(dto.getRoleCode()));
            return;
        }

        // Page header pills
        if (lblTodayDate       != null) lblTodayDate.setValue(LocalDate.now().format(DATE_FMT));
       

        // KPI strip
        loadKpis();

        // Quick action buttons
        if (gotoVerificationQueue != null)
            gotoVerificationQueue.addEventListener(Events.ON_CLICK, e ->
                Executions.sendRedirect("/outward/checkerQueue/checkerQueue.zul"));

        if (gotoDemExport != null)
            gotoDemExport.addEventListener(Events.ON_CLICK, e ->
                Executions.sendRedirect("/outward/demExport/demExport.zul"));

        if (gotoReports != null)
            gotoReports.addEventListener(Events.ON_CLICK, e ->
                Executions.sendRedirect("/reports/checkerReports.zul"));

        if (gotoVerificationQueueSmall != null)
            gotoVerificationQueueSmall.addEventListener(Events.ON_CLICK, e ->
                Executions.sendRedirect("/outward/checkerQueue/checkerQueue.zul"));

        // Recent Batches
        allBatches  = checkerService.getCheckerQueueBatches();
        currentPage = 1;
        applyFilter();
    }

    // ════════════════════════════════════════════════════════════════
    //  KPI Strip
    // ════════════════════════════════════════════════════════════════

    private void loadKpis() {
        try {
            Map<String, Integer> counts = checkerService.getDashboardCounts();

            // Awaiting Review = batches the checker still needs to action
            int awaitingReview = counts.getOrDefault("pending",    0)
                               + counts.getOrDefault("inProgress", 0);

            // On Hold = checker parked these for later
            int onHold = counts.getOrDefault("hold", 0);

            // Ready to Export = checker approved, waiting for DEM export
            int readyToExport = counts.getOrDefault("approved", 0);

            setValue(kpiPending, awaitingReview);
            setValue(kpiHold,    onHold);
            setValue(kpiReady,   readyToExport);

        } catch (Exception e) {
            System.err.println("CheckerOutwardDashboardComposer → loadKpis failed: "
                    + e.getMessage());
        }
    }

    private void setValue(Label label, int value) {
        if (label != null) label.setValue(String.valueOf(value));
    }

    // ════════════════════════════════════════════════════════════════
    //  Filter Events
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnApplyFilter")
    public void applyFilter() {
        currentPage = 1;
        renderBatches();
    }

    @Listen("onClick = #btnClearFilter")
    public void clearFilter() {
        currentPage = 1;
        // Clear native HTML date inputs via JavaScript
        Clients.evalJavaScript(
            "var f = document.getElementById('filterFromDateNative'); if(f) f.value = '';" +
            "var t = document.getElementById('filterToDateNative');   if(t) t.value = '';"
        );
        filterFromDateInput.setValue("");
        filterToDateInput.setValue("");
        filterStatus.setSelectedIndex(0);
        filterBatchId.setValue("");
        renderBatches();
    }

    // Instant search fires as user types
    @Listen("onChange = #filterBatchId; onChanging = #filterBatchId")
    public void onBatchIdChange() {
        currentPage = 1;
        renderBatches();
    }

    // ════════════════════════════════════════════════════════════════
    //  Pagination Events
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnPrevPage")
    public void onPrevPage() {
        if (currentPage > 1) { currentPage--; renderBatches(); }
    }

    @Listen("onClick = #btnNextPage")
    public void onNextPage() {
        currentPage++;
        renderBatches();
    }

    // ════════════════════════════════════════════════════════════════
    //  Core: Filter + Paginate + Render
    // ════════════════════════════════════════════════════════════════

    private void renderBatches() {

        // ── Read filter values ──────────────────────────────────
        String statusVal  = getSelectedValue(filterStatus);
        String batchIdVal = filterBatchId.getValue() != null
                ? filterBatchId.getValue().trim().toLowerCase() : "";

        LocalDate fromDate = parseDate(filterFromDateInput.getValue());
        LocalDate toDate   = parseDate(filterToDateInput.getValue());

        // ── Filter ──────────────────────────────────────────────
        List<OutwardBatch> filtered = new ArrayList<>();
        for (OutwardBatch b : allBatches) {

            // Batch ID search
            if (!batchIdVal.isEmpty()
                    && !b.getBatchId().toLowerCase().contains(batchIdVal)) continue;

            // Status filter
            if (!"ALL".equals(statusVal) && !statusVal.isEmpty()
                    && !statusVal.equals(b.getStatus())) continue;

            // Date filter — uses submittedAt, falls back to createdAt
            if (fromDate != null || toDate != null) {
                LocalDate batchDate = b.getSubmittedAt() != null
                        ? b.getSubmittedAt().toLocalDate()
                        : b.getCreatedAt().toLocalDate();
                if (fromDate != null && batchDate.isBefore(fromDate)) continue;
                if (toDate   != null && batchDate.isAfter(toDate))    continue;
            }

            filtered.add(b);
        }

        // ── Empty state ─────────────────────────────────────────
        if (filtered.isEmpty()) {
            clearListbox();
            recentBatchList.setVisible(false);
            emptyBatchesMsg.setVisible(true);
            batchPager.setVisible(false);
            return;
        }

        emptyBatchesMsg.setVisible(false);

        // ── Pagination ───────────────────────────────────────────
        int totalCount = filtered.size();
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1)          currentPage = 1;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, totalCount);

        pagerInfo.setValue("Page " + currentPage + " of " + totalPages
                + " (" + totalCount + " batches)");
        btnPrevPage.setDisabled(currentPage <= 1);
        btnNextPage.setDisabled(currentPage >= totalPages);
        batchPager.setVisible(true);

        // ── Render rows ──────────────────────────────────────────
        clearListbox();
        for (OutwardBatch batch : filtered.subList(fromIndex, toIndex)) {
            recentBatchList.appendChild(buildListitem(batch));
        }
        recentBatchList.setVisible(true);
    }

    private Listitem buildListitem(OutwardBatch batch) {
        Listitem item = new Listitem();

        Listcell idCell = new Listcell(batch.getBatchId());
        idCell.setStyle("font-weight:600; font-family:monospace;");
        item.appendChild(idCell);

        item.appendChild(new Listcell(String.valueOf(batch.getChequeCount())));

        String amt = batch.getActualAmount() != null
                ? "₹ " + MONEY_FMT.format(batch.getActualAmount())
                : batch.getExpectedAmount() != null
                    ? "₹ " + MONEY_FMT.format(batch.getExpectedAmount())
                    : "—";
        Listcell amtCell = new Listcell(amt);
        amtCell.setStyle("font-family:monospace;");
        item.appendChild(amtCell);

        String submittedAt = batch.getSubmittedAt() != null
                ? batch.getSubmittedAt().format(ROW_FMT) : "—";
        item.appendChild(new Listcell(submittedAt));

        Listcell statusCell = new Listcell();
        Label statusLbl = new Label(resolveStatusLabel(batch.getStatus()));
        statusLbl.setSclass("status-badge " + resolveStatusSclass(batch.getStatus()));
        statusCell.appendChild(statusLbl);
        item.appendChild(statusCell);

        // Click row → go to verification queue for that batch
        final Long batchDbId = batch.getId();
        item.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
            @Override public void onEvent(Event e) {
                if ("SUBMITTED".equals(batch.getStatus())
                        || "CHECKER_IN_PROGRESS".equals(batch.getStatus())) {
                    Executions.sendRedirect(
                        "/outward/checkerQueue/checkerQueue.zul?batchId=" + batchDbId);
                } else {
                    Executions.sendRedirect("/outward/checkerQueue/checkerQueue.zul");
                }
            }
        });

        return item;
    }

    private void clearListbox() {
        List<Component> toRemove = new ArrayList<>();
        for (Component c : recentBatchList.getChildren()) {
            if (c instanceof Listitem) toRemove.add(c);
        }
        for (Component c : toRemove) recentBatchList.removeChild(c);
    }

    // ════════════════════════════════════════════════════════════════
    //  Logout
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() { SessionUtil.logout(); }

    // ════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ════════════════════════════════════════════════════════════════

    private String resolveSession() {
        int hour = java.time.LocalTime.now().getHour();
        return (hour < 12) ? "Morning Session" : "Afternoon Session";
    }

    /** Parses yyyy-MM-dd from native HTML date input. */
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return LocalDate.parse(value.trim()); } catch (Exception e) { return null; }
    }

    private String getSelectedValue(Listbox listbox) {
        Listitem sel = listbox.getSelectedItem();
        if (sel == null) return "ALL";
        Object val = sel.getValue();
        return val != null ? val.toString().trim() : "ALL";
    }

    private String resolveStatusLabel(String status) {
        if (status == null) return "—";
        switch (status) {
            case "SUBMITTED":           return "Pending";
            case "CHECKER_IN_PROGRESS": return "In Progress";
            case "CHECKER_HOLD":        return "Hold";
            case "REFER_BACK":          return "With Maker";
            case "CHECKER_APPROVED":    return "Ready to Export";
            case "EXPORTED":            return "Exported";
            case "REJECTED":            return "Rejected";
            default:                    return status;
        }
    }

    private String resolveStatusSclass(String status) {
        if (status == null) return "";
        switch (status) {
            case "SUBMITTED":           return "status-pending";
            case "CHECKER_IN_PROGRESS": return "status-info";
            case "CHECKER_HOLD":        return "status-refer";
            case "REFER_BACK":          return "status-refer";
            case "CHECKER_APPROVED":    return "status-approved";
            case "EXPORTED":            return "status-exported";
            case "REJECTED":            return "status-rejected";
            default:                    return "";
        }
    }
}