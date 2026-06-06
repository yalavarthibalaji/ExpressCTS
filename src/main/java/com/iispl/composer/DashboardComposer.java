package com.iispl.composer;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.service.BatchUploadService;
import com.iispl.serviceImpl.BatchUploadServiceImpl;
import com.iispl.util.SessionUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardComposer extends SelectorComposer<Component> {

    private static final DateTimeFormatter DATE_FMT    = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter CELL_FMT    = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ── KPI Labels ─────────────────────────────────────────────
    @Wire private Label kpiTotalBatches;
    @Wire private Label kpiMicrPending;
    @Wire private Label kpiEntryPending;
    @Wire private Label kpiSubmitted;
    @Wire private Label kpiTotalCheques;

    // ── Header pills ───────────────────────────────────────────
    @Wire private Label lblTodayDate;
    @Wire private Label lblClearingSession;

    // ── Topbar ─────────────────────────────────────────────────
    @Wire private Label userAvatar;
    @Wire private Label userName;
    @Wire private Label userRole;

    // ── Recent batches table ───────────────────────────────────
    @Wire private Listbox recentBatchList;
    @Wire private Label   emptyBatchesMsg;

    // ── Filters ────────────────────────────────────────────────
    @Wire private Datebox  filterFromDate;
    @Wire private Datebox  filterToDate;
    @Wire private Combobox filterStatus;
    @Wire private Textbox  filterBatchId;

    // ── State ──────────────────────────────────────────────────
    private final BatchUploadService batchUploadService = new BatchUploadServiceImpl();
    private Long currentMakerId;
    private List<OutwardBatch> allBatches;

    // ══════════════════════════════════════════════════════════
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        currentMakerId = dto.getUserId();

        // Topbar
        if (userAvatar != null) userAvatar.setValue(dto.getInitials());
        if (userName   != null) userName.setValue(dto.getFullName());
        if (userRole   != null) userRole.setValue(formatRole(dto.getRoleCode()));

        // Header pills
        if (lblTodayDate       != null) lblTodayDate.setValue(LocalDate.now().format(DATE_FMT));
        if (lblClearingSession != null) lblClearingSession.setValue(resolveSession());

        // Load data
        allBatches = batchUploadService.getMyBatches(currentMakerId);
        loadKpis(allBatches);
        renderTable(allBatches);
    }

    // ══════════════════════════════════════════════════════════
    // KPI — computed from full batch list
    // ══════════════════════════════════════════════════════════
    private void loadKpis(List<OutwardBatch> batches) {
        try {
            long totalBatches = batches.size();
            long micrPending  = 0, entryPending = 0, submitted = 0, totalCheques = 0;

            for (OutwardBatch b : batches) {
                String status = b.getStatus()       != null ? b.getStatus()       : "";
                String repair = b.getRepairStatus() != null ? b.getRepairStatus() : "";

                if ("NEEDS_REPAIR".equals(repair))                                  micrPending++;
                if ("ENTRY_PENDING".equals(status) || "ENTRY_DONE".equals(status)) entryPending++;
                if ("SUBMITTED".equals(status))                                      submitted++;
                if (b.getChequeCount() > 0) totalCheques += b.getChequeCount();
            }

            setValue(kpiTotalBatches, totalBatches);
            setValue(kpiMicrPending,  micrPending);
            setValue(kpiEntryPending, entryPending);
            setValue(kpiSubmitted,    submitted);
            setValue(kpiTotalCheques, totalCheques);

        } catch (Exception e) {
            System.err.println("DashboardComposer → KPI load failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Render table from a (possibly filtered) list
    // ══════════════════════════════════════════════════════════
    private void renderTable(List<OutwardBatch> batches) {
        // Remove old rows (keep listhead)
        List<Component> toRemove = recentBatchList.getChildren().stream()
                .filter(c -> c instanceof Listitem)
                .collect(Collectors.toList());
        toRemove.forEach(recentBatchList::removeChild);

        if (batches == null || batches.isEmpty()) {
            recentBatchList.setVisible(false);
            emptyBatchesMsg.setVisible(true);
            return;
        }

        // Last 10, most recent first
        int start = Math.max(0, batches.size() - 10);
        List<OutwardBatch> recent = batches.subList(start, batches.size());

        for (int i = recent.size() - 1; i >= 0; i--) {
            OutwardBatch b    = recent.get(i);
            Listitem     item = new Listitem();

            item.appendChild(new Listcell(nvl(b.getBatchId())));
            item.appendChild(new Listcell(String.valueOf(b.getChequeCount())));
            item.appendChild(new Listcell(
                    b.getActualAmount() != null
                    ? "Rs. " + b.getActualAmount().toPlainString()
                    : "-"));
            item.appendChild(new Listcell(
                    b.getCreatedAt() != null
                    ? b.getCreatedAt().format(CELL_FMT)
                    : "-"));

            // Status badge
            Listcell statusCell  = new Listcell();
            Label    statusLabel = new Label(formatStatusLabel(b.getStatus()));
            statusLabel.setSclass(resolveStatusSclass(b.getStatus()));
            statusCell.appendChild(statusLabel);
            item.appendChild(statusCell);

            recentBatchList.appendChild(item);
        }

        recentBatchList.setVisible(true);
        emptyBatchesMsg.setVisible(false);
    }

    // ══════════════════════════════════════════════════════════
    // Filter: Apply
    // ══════════════════════════════════════════════════════════
    @Listen("onClick = #btnApplyFilter")
    public void applyFilter() {
        if (allBatches == null) return;

        java.util.Date fromRaw   = filterFromDate != null ? filterFromDate.getValue()  : null;
        java.util.Date toRaw     = filterToDate   != null ? filterToDate.getValue()    : null;
        String statusVal         = "";
        String batchIdVal        = filterBatchId  != null ? filterBatchId.getValue().trim().toLowerCase() : "";

        // Resolve status from combobox
        if (filterStatus != null && filterStatus.getSelectedItem() != null) {
            statusVal = (String) filterStatus.getSelectedItem().getValue();
            if ("ALL".equals(statusVal)) statusVal = "";
        }

        LocalDate fromDate = fromRaw != null ? fromRaw.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate() : null;
        LocalDate toDate   = toRaw   != null ? toRaw.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate() : null;

        final String finalStatus  = statusVal;
        final String finalBatchId = batchIdVal;

        List<OutwardBatch> filtered = allBatches.stream()
                .filter(b -> {
                    // Date filter
                    if (fromDate != null || toDate != null) {
                        LocalDateTime created = b.getCreatedAt();
                        if (created == null) return false;
                        LocalDate d = created.toLocalDate();
                        if (fromDate != null && d.isBefore(fromDate)) return false;
                        if (toDate   != null && d.isAfter(toDate))    return false;
                    }
                    // Status filter
                    if (!finalStatus.isEmpty()) {
                        String s = b.getStatus() != null ? b.getStatus() : "";
                        if (!s.equalsIgnoreCase(finalStatus)) return false;
                    }
                    // Batch ID search
                    if (!finalBatchId.isEmpty()) {
                        String id = b.getBatchId() != null ? b.getBatchId().toLowerCase() : "";
                        if (!id.contains(finalBatchId)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        renderTable(filtered);
    }

    // ══════════════════════════════════════════════════════════
    // Filter: Clear
    // ══════════════════════════════════════════════════════════
    @Listen("onClick = #btnClearFilter")
    public void clearFilter() {
        if (filterFromDate != null) filterFromDate.setValue(null);
        if (filterToDate   != null) filterToDate.setValue(null);
        if (filterStatus   != null) filterStatus.setValue("");
        if (filterBatchId  != null) filterBatchId.setValue("");
        renderTable(allBatches);
    }

    // Live search on Batch ID textbox (instant=true triggers onChange)
    @Listen("onChange = #filterBatchId")
    public void onBatchIdChange() {
        applyFilter();
    }

    // ══════════════════════════════════════════════════════════
    // Navigation
    // ══════════════════════════════════════════════════════════
    @Listen("onClick = #gotoBatchUpload")
    public void gotoBatchUpload() {
        Executions.sendRedirect("/outward/batchUpload/batchUpload.zul");
    }

    @Listen("onClick = #gotoMicrRepair")
    public void gotoMicrRepair() {
        Executions.sendRedirect("/outward/micrRepair/micrRepair.zul");
    }

    @Listen("onClick = #gotoAcctAmount")
    public void gotoAcctAmount() {
        Executions.sendRedirect("/outward/dataEntry/dataEntry.zul");
    }

    @Listen("onClick = #gotoViewBatches")
    public void gotoViewBatches() {
        Executions.sendRedirect("/outward/viewBatches/viewBatches.zul");
    }

    @Listen("onClick = #gotoViewBatchesSmall")
    public void gotoViewBatchesSmall() {
        Executions.sendRedirect("/outward/viewBatches/viewBatches.zul");
    }

    @Listen("onClick = #logoutBtn")
    public void doLogout() { SessionUtil.logout(); }

    @Listen("onClick = #userMgmtBtn")
    public void goToUserManagement() {
        Executions.sendRedirect("/admin/userManagement/userManagement.zul");
    }

    @Listen("onClick = #backToDashboardBtn")
    public void backToDashboard() {
        Executions.sendRedirect("/admin/adminDashboard.zul");
    }

    // ══════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════
    private void setValue(Label label, long value) {
        if (label != null) label.setValue(String.valueOf(value));
    }

    private String nvl(String s) { return s != null ? s : "-"; }

    private String resolveSession() {
        return LocalTime.now().isBefore(LocalTime.NOON) ? "Morning Clearing" : "Afternoon Clearing";
    }

    private String formatStatusLabel(String status) {
        if (status == null) return "-";
        switch (status.toUpperCase()) {
            case "UPLOADED":      return "Uploaded";
            case "NEEDS_REPAIR":  return "Needs Repair";
            case "ENTRY_PENDING": return "Entry Pending";
            case "ENTRY_DONE":    return "Entry Done";
            case "SUBMITTED":     return "Submitted";
            case "REJECTED":      return "Rejected";
            default:              return status;
        }
    }

    private String resolveStatusSclass(String status) {
        if (status == null) return "status-badge";
        switch (status.toUpperCase()) {
            case "NEEDS_REPAIR":  return "status-badge status-pending";
            case "ENTRY_PENDING": return "status-badge status-received";
            case "ENTRY_DONE":    return "status-badge status-parsed";
            case "SUBMITTED":     return "status-badge status-approved";
            case "REJECTED":      return "status-badge status-rejected";
            default:              return "status-badge";
        }
    }

    private String formatRole(String code) {
        if (code == null) return "";
        switch (code) {
            case "ADMIN":           return "Administrator";
            case "MAKER_OUTWARD":   return "Maker — Outward";
            case "CHECKER_OUTWARD": return "Checker — Outward";
            case "MAKER_INWARD":    return "Maker — Inward";
            case "CHECKER_INWARD":  return "Checker — Inward";
            default:                return code;
        }
    }
}