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

import com.iispl.service.MakerOutwardService;
import com.iispl.serviceImpl.MakerOutwardServiceImpl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Div;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * File    : com/iispl/composer/DashboardComposer.java
 * Purpose : Maker Outward Dashboard — KPI counts and batch table.
 *
 * Correct Batch Status Flow:
 *   NEEDS_REPAIR  → has MICR errors, waiting for MICR repair
 *   ENTRY_PENDING → MICR repair done (or no errors), ready for data entry
 *   SUBMITTED     → data entry done, in checker queue
 *   REFER_BACK    → checker referred batch back to maker
 *   REJECTED      → batch rejected
 *
 * KPI Fixes:
 *   Bug 1 — MICR Pending used repairStatus (NEVER cleared after repair).
 *            Fixed to use main batch status = NEEDS_REPAIR.
 *   Bug 2 — Entry Pending checked ENTRY_PENDING (ghost status, never set).
 *            Fixed to check ENTRY_PENDING + REFER_BACK.
 */
public class DashboardComposer extends SelectorComposer<Component> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter CELL_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ── KPI Labels ──
    @Wire private Label kpiTotalBatches;
    @Wire private Label kpiMicrPending;
    @Wire private Label kpiEntryPending;
    @Wire private Label kpiSubmitted;
    @Wire private Label kpiTotalCheques;

    // ── Header pills ──
    @Wire private Label lblTodayDate;
    @Wire private Label lblClearingSession;

    // ── Topbar ──
    @Wire private Label userAvatar;
    @Wire private Label userName;
    @Wire private Label userRole;
    
 // ── Module Picker Popup (for REFER_BACK with cheques in both modules) ──
    @Wire private Div    modulePickerModal;
    @Wire private Label  pickerBatchId;
    @Wire private Label  pickerMicrCount;
    @Wire private Label  pickerDataCount;

    // ── Service ──
    private final MakerOutwardService makerOutwardService = new MakerOutwardServiceImpl();

    // ── State for popup (which batch is currently being routed) ──
    private OutwardBatch pickerCurrentBatch;
    
    
    // ── Recent batches table ──
    @Wire private Listbox recentBatchList;
    @Wire private Label   emptyBatchesMsg;

    // ── Filters ──
    @Wire private Datebox  filterFromDate;
    @Wire private Datebox  filterToDate;
    @Wire private Combobox filterStatus;
    @Wire private Textbox  filterBatchId;

    // ── State ──
    private final BatchUploadService batchUploadService = new BatchUploadServiceImpl();
    private Long                     currentMakerId;
    private List<OutwardBatch>       allBatches;

    // ════════════════════════════════════════════════════
    //  Page Init
    // ════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        currentMakerId = dto.getUserId();

        if (userAvatar != null) userAvatar.setValue(dto.getInitials());
        if (userName   != null) userName.setValue(dto.getFullName());
        if (userRole   != null) userRole.setValue(formatRole(dto.getRoleCode()));

        if (lblTodayDate       != null)
            lblTodayDate.setValue(LocalDate.now().format(DATE_FMT));
        if (lblClearingSession != null)
            lblClearingSession.setValue(resolveSession());

        allBatches = batchUploadService.getMyBatches(currentMakerId);
        loadKpis(allBatches);
        renderTable(allBatches);
    }

    // ════════════════════════════════════════════════════
    //  KPI — computed from full batch list
    // ════════════════════════════════════════════════════

    private void loadKpis(List<OutwardBatch> batches) {
        try {
            long totalBatches = batches.size();
            long micrPending  = 0;
            long entryPending = 0;
            long referBack    = 0;
            long submitted    = 0;
            long totalCheques = 0;

            for (OutwardBatch b : batches) {
                String status = b.getStatus() != null ? b.getStatus() : "";

                // MICR Pending — batch has MICR errors waiting for repair
                // FIX: uses batch status (not repairStatus which never gets cleared)
                if ("NEEDS_REPAIR".equals(status))  micrPending++;

                // Entry Pending — MICR done, ready for data entry
                // FIX: was checking ENTRY_DONE (ghost status, never set).
                //      Now correctly checks ENTRY_PENDING.
                if ("ENTRY_PENDING".equals(status)) entryPending++;

                // Refer Back — checker sent batch back to maker
                if ("REFER_BACK".equals(status))    referBack++;

                if ("SUBMITTED".equals(status))     submitted++;

                if (b.getChequeCount() > 0)         totalCheques += b.getChequeCount();
            }

            setValue(kpiTotalBatches, totalBatches);
            setValue(kpiMicrPending,  micrPending);
            // Entry Pending KPI = ENTRY_PENDING + REFER_BACK
            // Both need maker attention at the data entry step
            setValue(kpiEntryPending, entryPending + referBack);
            setValue(kpiSubmitted,    submitted);
            setValue(kpiTotalCheques, totalCheques);

        } catch (Exception e) {
            System.err.println(
                "DashboardComposer → KPI load failed: " + e.getMessage());
        }
    }

    private void renderTable(List<OutwardBatch> batches) {
        List<Component> toRemove = recentBatchList.getChildren().stream()
                .filter(c -> c instanceof Listitem)
                .collect(Collectors.toList());
        toRemove.forEach(recentBatchList::removeChild);

        if (batches == null || batches.isEmpty()) {
            recentBatchList.setVisible(false);
            emptyBatchesMsg.setVisible(true);
            return;
        }

        int start = Math.max(0, batches.size() - 10);
        List<OutwardBatch> recent = batches.subList(start, batches.size());

        for (int i = recent.size() - 1; i >= 0; i--) {
            final OutwardBatch b = recent.get(i);
            Listitem item = new Listitem();

            // Store the batch object on the listitem so the onSelect handler
            // can read it without doing another lookup.
            item.setValue(b);
            // Make it visually obvious the row is clickable
            item.setStyle("cursor: pointer;");

            item.appendChild(new Listcell(nvl(b.getBatchId())));
            item.appendChild(new Listcell(String.valueOf(b.getChequeCount())));
            item.appendChild(new Listcell(
                    b.getActualAmount() != null
                    ? "Rs. " + b.getActualAmount().toPlainString() : "-"));
            item.appendChild(new Listcell(
                    b.getCreatedAt() != null
                    ? b.getCreatedAt().format(CELL_FMT) : "-"));

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
    // ════════════════════════════════════════════════════
    //  Filters
    // ════════════════════════════════════════════════════

    @Listen("onClick = #btnApplyFilter")
    public void applyFilter() {
        if (allBatches == null) return;

        java.util.Date fromRaw = filterFromDate != null ? filterFromDate.getValue() : null;
        java.util.Date toRaw   = filterToDate   != null ? filterToDate.getValue()   : null;
        String statusVal       = "";
        String batchIdVal      = filterBatchId  != null
            ? filterBatchId.getValue().trim().toLowerCase() : "";

        if (filterStatus != null && filterStatus.getSelectedItem() != null) {
            statusVal = (String) filterStatus.getSelectedItem().getValue();
            if ("ALL".equals(statusVal)) statusVal = "";
        }

        LocalDate fromDate = fromRaw != null ? fromRaw.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate() : null;
        LocalDate toDate   = toRaw   != null ? toRaw.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate() : null;

        final String fStatus  = statusVal;
        final String fBatchId = batchIdVal;

        List<OutwardBatch> filtered = allBatches.stream()
                .filter(b -> {
                    if (fromDate != null || toDate != null) {
                        LocalDateTime created = b.getCreatedAt();
                        if (created == null) return false;
                        LocalDate d = created.toLocalDate();
                        if (fromDate != null && d.isBefore(fromDate)) return false;
                        if (toDate   != null && d.isAfter(toDate))    return false;
                    }
                    if (!fStatus.isEmpty()) {
                        String s = b.getStatus() != null ? b.getStatus() : "";
                        if (!s.equalsIgnoreCase(fStatus)) return false;
                    }
                    if (!fBatchId.isEmpty()) {
                        String id = b.getBatchId() != null
                            ? b.getBatchId().toLowerCase() : "";
                        if (!id.contains(fBatchId)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        renderTable(filtered);
    }

    @Listen("onClick = #btnClearFilter")
    public void clearFilter() {
        if (filterFromDate != null) filterFromDate.setValue(null);
        if (filterToDate   != null) filterToDate.setValue(null);
        if (filterStatus   != null) filterStatus.setValue("");
        if (filterBatchId  != null) filterBatchId.setValue("");
        renderTable(allBatches);
    }

    @Listen("onChange = #filterBatchId")
    public void onBatchIdChange() { applyFilter(); }

    // ════════════════════════════════════════════════════
    //  Navigation
    // ════════════════════════════════════════════════════

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
        Executions.sendRedirect("/outward/acctAmount/acctAmount.zul");
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

    // ════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════

    private void setValue(Label label, long value) {
        if (label != null) label.setValue(String.valueOf(value));
    }

    private String nvl(String s) { return s != null ? s : "-"; }

    private String resolveSession() {
        return LocalTime.now().isBefore(LocalTime.NOON)
            ? "Morning Clearing" : "Afternoon Clearing";
    }
    
    /**
     * Human-readable label for each batch status.
     * Covers full workflow including post-submission checker statuses
     * because a maker sees their own batches even after they move to checker.
     */
    private String formatStatusLabel(String status) {
        if (status == null) return "-";
        switch (status.toUpperCase()) {
            // ── Maker-side ──
            case "NEEDS_REPAIR":         return "Needs MICR Repair";
            case "ENTRY_PENDING":        return "Pending Data Entry";
            case "SUBMITTED":            return "Submitted";
            case "REFER_BACK":           return "Referred Back";
            // ── Checker-side (read-only for maker) ──
            case "CHECKER_IN_PROGRESS":  return "Checker In Progress";
            case "CHECKER_HOLD":         return "On Hold";
            case "CHECKER_APPROVED":     return "Approved";
            case "EXPORTED":             return "Exported";
            // ── Final ──
            case "REJECTED":             return "Rejected";
            default:                     return status;
        }
    }

    /**
     * CSS class for status badge in dashboard table.
     */
    private String resolveStatusSclass(String status) {
        if (status == null) return "status-badge";
        switch (status.toUpperCase()) {
            case "NEEDS_REPAIR":         return "status-badge status-pending";
            case "ENTRY_PENDING":        return "status-badge status-received";
            case "SUBMITTED":            return "status-badge status-info";
            case "REFER_BACK":           return "status-badge status-refer";
            case "CHECKER_IN_PROGRESS":  return "status-badge status-info";
            case "CHECKER_HOLD":         return "status-badge status-refer";
            case "CHECKER_APPROVED":     return "status-badge status-approved";
            case "EXPORTED":             return "status-badge status-exported";
            case "REJECTED":             return "status-badge status-rejected";
            default:                     return "status-badge";
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
    
 // ════════════════════════════════════════════════════════════════
//  Row Click — route by status
// ════════════════════════════════════════════════════════════════

@Listen("onSelect = #recentBatchList")
public void onBatchRowSelect() {
    Listitem sel = recentBatchList.getSelectedItem();
    if (sel == null) return;

    OutwardBatch batch = (OutwardBatch) sel.getValue();
    if (batch == null) return;

    // Always clear selection after handling so the same row can be clicked again
    recentBatchList.clearSelection();

    routeByStatus(batch);
}

/**
 * Decides where to send the user based on the batch status.
 * For REFER_BACK with cheques in both fixable modules, shows the
 * module-picker popup; otherwise navigates directly.
 */
private void routeByStatus(OutwardBatch batch) {
    String status  = batch.getStatus();
    String batchId = batch.getBatchId();

    if (status == null) {
        Clients.showNotification("Batch has no status — cannot route.",
                "warning", null, "top_center", 2000);
        return;
    }

    switch (status) {
        case "NEEDS_REPAIR":
            Executions.sendRedirect(
                "/outward/micrRepair/micrRepair.zul?batchId=" + batchId);
            return;

        case "ENTRY_PENDING":
            Executions.sendRedirect(
                "/outward/acctAmount/acctAmount.zul?batchId=" + batchId);
            return;

        case "REFER_BACK":
            handleReferBackClick(batch);
            return;

        // Statuses where the batch is OUT OF the maker's hands —
        // open View Batches (read-only) so they can inspect details.
        case "SUBMITTED":
        case "CHECKER_IN_PROGRESS":
        case "CHECKER_HOLD":
        case "CHECKER_APPROVED":
        case "EXPORTED":
        case "REJECTED":
            Executions.sendRedirect(
                "/outward/viewBatches/viewBatches.zul?batchId=" + batchId);
            return;

        default:
            Clients.showNotification("No action defined for status: " + status,
                    "info", null, "top_center", 2000);
    }
}

/**
 * REFER_BACK click:
 *   - Lookup how many cheques are referred to each module
 *   - 0 + 0 → shouldn't happen (REFER_BACK means at least 1 referred); show warning
 *   - X + 0 → navigate to MICR Repair
 *   - 0 + Y → navigate to Account Entry
 *   - X + Y → show module-picker popup
 */
private void handleReferBackClick(OutwardBatch batch) {
    int[] counts = makerOutwardService.getReferralCounts(batch.getId());
    int micrCount = counts[0];
    int dataCount = counts[1];

    if (micrCount == 0 && dataCount == 0) {
        Clients.showNotification(
                "Batch is in Refer Back but has no referred cheques. "
              + "Check the batch in View Batches.",
                "warning", null, "top_center", 3000);
        Executions.sendRedirect(
            "/outward/viewBatches/viewBatches.zul?batchId=" + batch.getBatchId());
        return;
    }

    if (micrCount > 0 && dataCount == 0) {
        Executions.sendRedirect(
            "/outward/micrRepair/micrRepair.zul?batchId=" + batch.getBatchId());
        return;
    }

    if (dataCount > 0 && micrCount == 0) {
        Executions.sendRedirect(
            "/outward/acctAmount/acctAmount.zul?batchId=" + batch.getBatchId());
        return;
    }

    // Both > 0 → show the picker popup
    showModulePicker(batch, micrCount, dataCount);
}

// ════════════════════════════════════════════════════════════════
//  Module Picker Popup
// ════════════════════════════════════════════════════════════════

private void showModulePicker(OutwardBatch batch, int micrCount, int dataCount) {
    pickerCurrentBatch = batch;
    pickerBatchId.setValue(batch.getBatchId());
    pickerMicrCount.setValue(String.valueOf(micrCount));
    pickerDataCount.setValue(String.valueOf(dataCount));
    modulePickerModal.setSclass("modal-ov open");
}

private void hideModulePicker() {
    modulePickerModal.setSclass("modal-ov");
    pickerCurrentBatch = null;
}

@Listen("onClick = #modulePickerCloseBtn, #pickerCancelBtn")
public void onPickerClose() {
    hideModulePicker();
}

@Listen("onClick = #pickerGoMicrBtn")
public void onPickerGoMicr() {
    if (pickerCurrentBatch == null) {
        hideModulePicker();
        return;
    }
    String batchId = pickerCurrentBatch.getBatchId();
    hideModulePicker();
    Executions.sendRedirect(
        "/outward/micrRepair/micrRepair.zul?batchId=" + batchId);
}

@Listen("onClick = #pickerGoDataBtn")
public void onPickerGoData() {
    if (pickerCurrentBatch == null) {
        hideModulePicker();
        return;
    }
    String batchId = pickerCurrentBatch.getBatchId();
    hideModulePicker();
    Executions.sendRedirect(
        "/outward/acctAmount/acctAmount.zul?batchId=" + batchId);
}
}