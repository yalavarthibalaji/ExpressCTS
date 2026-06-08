package com.iispl.composer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;

import com.iispl.dto.InwardBatchDto;
import com.iispl.service.InwardBatchService;
import com.iispl.serviceImpl.InwardBatchServiceImpl;

public class MakerInwardDashboardComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ── Wired Components ──────────────────────────────────────────────────

    @Wire private Listbox  pendingBatches;
    @Wire private Label    emptyBatchesMsg;

    // Stat labels
    @Wire private Label    lblPendingCount;
    @Wire private Label    lblClearedCount;
    @Wire private Label    lblTotalCount;

    // Toolbar
    @Wire private Textbox  txtBatchSearch;
    @Wire private Datebox  dateBatchFilter;
    @Wire private Combobox cmbSort;
    @Wire private Button   btnClearDate;
    @Wire private Label    lblTableCount;

    // ── Service ───────────────────────────────────────────────────────────

    private final InwardBatchService service = new InwardBatchServiceImpl();

    // ── State ─────────────────────────────────────────────────────────────

    private List<InwardBatchDto> allBatches;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        loadBatches();
        wireToolbarEvents();
    }

    // =====================================================================
    // DATA LOAD
    // =====================================================================

    private void loadBatches() {
        allBatches = service.getAllBatcheDtos();

        if (allBatches == null || allBatches.isEmpty()) {
            lblPendingCount.setValue("0");
            lblClearedCount.setValue("0");
            lblTotalCount.setValue("0");
            showEmpty();
            return;
        }

        // Stat cards
        long pending = allBatches.stream()
                .filter(b -> "RECEIVED".equals(b.getStatus())
                          || "PARSED".equals(b.getStatus()))
                .count();

        long cleared = allBatches.stream()
                .filter(b -> "APPROVED".equals(b.getStatus()))
                .count();

        lblPendingCount.setValue(String.valueOf(pending));
        lblClearedCount.setValue(String.valueOf(cleared));
        lblTotalCount.setValue(String.valueOf(allBatches.size()));

        // Default render — newest first
        renderTable();
    }

    // =====================================================================
    // TOOLBAR EVENT WIRING
    // =====================================================================

    private void wireToolbarEvents() {

        // Search — read value directly from InputEvent to avoid stale getValue()
        if (txtBatchSearch != null) {
            txtBatchSearch.addEventListener(Events.ON_CHANGE, e -> {
                InputEvent ie = (InputEvent) e;
                renderTableWithSearch(ie.getValue());
            });
        }

        // Date filter
        if (dateBatchFilter != null) {
            dateBatchFilter.addEventListener(Events.ON_CHANGE, e -> renderTable());
        }

        // Sort
        if (cmbSort != null) {
            cmbSort.addEventListener(Events.ON_SELECT, e -> renderTable());
        }
    }

    // ── Clear date button ─────────────────────────────────────────────────

    @Listen("onClick = #btnClearDate")
    public void onClearDate() {
        if (dateBatchFilter != null) {
            dateBatchFilter.setValue(null);
        }
        renderTable();
    }

    // =====================================================================
    // RENDER — entry points
    // =====================================================================

    /**
     * Called by date filter, sort, and clear button.
     * Reads search value from the textbox component.
     */
    private void renderTable() {
        String search = (txtBatchSearch != null && txtBatchSearch.getValue() != null)
                ? txtBatchSearch.getValue().trim().toLowerCase() : "";
        renderTableWithSearch(search);
    }

    /**
     * Called by search listener with the live InputEvent value.
     * This avoids ZK's component buffer lag on every keystroke.
     */
    private void renderTableWithSearch(String rawSearch) {
        if (allBatches == null) return;

        final String search = rawSearch != null ? rawSearch.trim().toLowerCase() : "";

        // ── Date filter ──────────────────────────────────────────────────
        final LocalDate filterDate =
                (dateBatchFilter != null && dateBatchFilter.getValue() != null)
                        ? dateBatchFilter.getValue()
                                .toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        : null;

        // ── Apply filters ────────────────────────────────────────────────
        List<InwardBatchDto> filtered = allBatches.stream()
                .filter(b -> {
                    // Search: match batch ID or status
                    if (!search.isEmpty()) {
                        String bId  = b.getBatchId() != null
                                ? b.getBatchId().toLowerCase() : "";
                        String stat = b.getStatus()  != null
                                ? b.getStatus().toLowerCase()  : "";
                        if (!bId.contains(search) && !stat.contains(search)) return false;
                    }
                    // Date: match parsedAt date exactly
                    if (filterDate != null) {
                        if (b.getParsedAt() == null) return false;
                        LocalDate batchDate = b.getParsedAt().toLocalDate();
                        if (!batchDate.equals(filterDate)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // ── Sort ─────────────────────────────────────────────────────────
        String sortVal = "NEWEST";
        if (cmbSort != null && cmbSort.getSelectedItem() != null) {
            Object v = cmbSort.getSelectedItem().getValue();
            if (v != null) sortVal = v.toString();
        }

        switch (sortVal) {
            case "OLDEST"  -> filtered.sort(
                    Comparator.comparing(InwardBatchDto::getParsedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())));
            case "ERRORS"  -> filtered.sort(
                    Comparator.comparingInt(InwardBatchDto::getMicrErrorCount).reversed());
            case "CHEQUES" -> filtered.sort(
                    Comparator.comparingInt(InwardBatchDto::getTotalCheques).reversed());
            default        -> filtered.sort(
                    Comparator.comparing(InwardBatchDto::getParsedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())));
        }

        // ── Render rows ──────────────────────────────────────────────────
        pendingBatches.getItems().clear();

        if (filtered.isEmpty()) {
            showEmpty();
            return;
        }

        showTable();

        for (InwardBatchDto dto : filtered) {
            appendRow(dto);
        }

        if (lblTableCount != null) {
            lblTableCount.setValue(String.valueOf(filtered.size()));
        }
    }

    // =====================================================================
    // ROW BUILDER
    // =====================================================================

    private void appendRow(InwardBatchDto dto) {
        Listitem item = new Listitem();

        // Batch ID
        item.appendChild(new Listcell(dto.getBatchId()));

        // No. of Cheques
        item.appendChild(new Listcell(String.valueOf(dto.getTotalCheques())));

        // MICR Error Count
        item.appendChild(new Listcell(String.valueOf(dto.getMicrErrorCount())));

        // Parsed At
        item.appendChild(new Listcell(
                dto.getParsedAt() != null
                        ? dto.getParsedAt().format(DISPLAY_FMT) : "—"));

        // Status badge
        Listcell statusCell = new Listcell();
        statusCell.setStyle("text-align:center");
        Label statusBadge = new Label(dto.getStatus());
        statusBadge.setSclass(resolveStatusSclass(dto.getStatus()));
        statusCell.appendChild(statusBadge);
        item.appendChild(statusCell);

        // Action button — passes batchId so RejectRepair loads correct batch
        Listcell actionCell = new Listcell();
        actionCell.setStyle("text-align:center");
        Button repairBtn = new Button("Repair");
        repairBtn.setSclass("btn-view");
        repairBtn.addEventListener("onClick", event ->
                Executions.sendRedirect(
                        "/inward/inwardMicr/RejectRepair.zul?batchId=" + dto.getBatchId()));
        actionCell.appendChild(repairBtn);
        item.appendChild(actionCell);

        pendingBatches.appendChild(item);
    }

    // =====================================================================
    // VISIBILITY HELPERS
    // =====================================================================

    private void showEmpty() {
        if (pendingBatches  != null) pendingBatches.setVisible(false);
        if (emptyBatchesMsg != null) emptyBatchesMsg.setVisible(true);
        if (lblTableCount   != null) lblTableCount.setValue("0");
    }

    private void showTable() {
        if (pendingBatches  != null) pendingBatches.setVisible(true);
        if (emptyBatchesMsg != null) emptyBatchesMsg.setVisible(false);
    }

    // =====================================================================
    // STATUS BADGE CSS
    // =====================================================================

    private String resolveStatusSclass(String status) {
        if (status == null) return "status-badge";
        return switch (status.toUpperCase()) {
            case "RECEIVED" -> "status-badge status-received";
            case "PARSED"   -> "status-badge status-parsed";
            case "APPROVED" -> "status-badge status-approved";
            case "REJECTED" -> "status-badge status-rejected";
            default         -> "status-badge";
        };
    }

    // =====================================================================
    // NAVIGATION
    // =====================================================================

    @Listen("onClick = #btnFileProcessing")
    public void onFileProcessing() {
        Executions.getCurrent().sendRedirect("/inward/bpxfUpload/bpxfUpload.zul");
    }

    @Listen("onClick = #btnRejectRepair")
    public void onRejectRepair() {
        Executions.getCurrent().sendRedirect("/inward/inwardMicr/RejectRepair.zul");
    }

    @Listen("onClick = #btnPendingOverlay")
    public void onPendingOverlay() {
        Executions.getCurrent().sendRedirect("/inward/inwardMicr/RejectRepair.zul");
    }

    @Listen("onClick = #btnClearedOverlay")
    public void onClearedOverlay() {
        Executions.getCurrent().sendRedirect("/inward/bpxfUpload/bpxfUpload.zul");
    }

    @Listen("onClick = #btnTotalOverlay")
    public void onTotalOverlay() {
        Executions.getCurrent().sendRedirect("/inward/bpxfUpload/bpxfUpload.zul");
    }
}