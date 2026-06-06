package com.iispl.composer;

import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.RejectRepairService;
import com.iispl.serviceImpl.RejectRepairServiceImpl;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DateAmountComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Wired components ──────────────────────────────────────────────────

    @Wire("#lblBatchBadge")     private Label    lblBatchBadge;
    @Wire("#lblPendingBadge")   private Label    lblPendingBadge;
    @Wire("#lblPageInfo")       private Label    lblPageInfo;
    @Wire("#chequeListbox")     private Listbox  chequeListbox;
    @Wire("#cmbFilter")         private Combobox cmbFilter;
    @Wire("#btnPrevPage")       private Button   btnPrevPage;
    @Wire("#btnNextPage")       private Button   btnNextPage;
    @Wire("#btnBackToStep1")    private Button   btnBackToStep1;
    @Wire("#btnNextStep3")      private Button   btnNextStep3;

    // Review popup
    @Wire("#reviewPopup")       private Window   reviewPopup;
    @Wire("#lblProcDate")       private Label    lblProcDate;
    @Wire("#lblProcAmt")        private Label    lblProcAmt;
    @Wire("#lblRcvdDate")       private Label    lblRcvdDate;
    @Wire("#lblRcvdAmt")        private Label    lblRcvdAmt;
    @Wire("#dtCorrectedDate")   private Datebox  dtCorrectedDate;
    @Wire("#numCorrectedAmt")   private Doublebox numCorrectedAmt;
    @Wire("#txtRemarks")        private Textbox  txtRemarks;
    @Wire("#btnAccept")         private Button   btnAccept;
    @Wire("#btnReject")         private Button   btnReject;
    @Wire("#btnRefer")          private Button   btnRefer;
    @Wire("#btnCancelPopup")    private Button   btnCancelPopup;

    // Reject reason popup
    @Wire("#rejectReasonPopup") private Window   rejectReasonPopup;
    @Wire("#cmbRejectReason")   private Combobox cmbRejectReason;
    @Wire("#txtRejectRemarks")  private Textbox  txtRejectRemarks;
    @Wire("#btnConfirmReject")  private Button   btnConfirmReject;
    @Wire("#btnCancelReject")   private Button   btnCancelReject;

    // ── Service & state ───────────────────────────────────────────────────

    private final RejectRepairService service = new RejectRepairServiceImpl();

    private String             currentBatchId;
    private List<InwardCheque> allCheques;
    private InwardCheque       selectedCheque;

    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        currentBatchId = Executions.getCurrent().getParameter("batchId");
        if (currentBatchId == null || currentBatchId.trim().isEmpty()) {
            // Fallback — first eligible batch
            var batches = service.getRepairEligibleBatches();
            if (batches != null && !batches.isEmpty()) {
                currentBatchId = batches.get(0).getBatchId();
            }
        }

        if (currentBatchId != null) {
            allCheques = service.getStep2ChequesByBatchId(currentBatchId);
            if (lblBatchBadge != null) {
                lblBatchBadge.setValue("BATCH: " + currentBatchId);
            }
            renderTable();
        }

        wireEvents();
    }

    // ── Wire events ───────────────────────────────────────────────────────

    private void wireEvents() {
        if (cmbFilter != null) {
            cmbFilter.addEventListener(Events.ON_SELECT, e -> {
                currentPage = 1; renderTable();
            });
        }
        if (btnPrevPage != null) {
            btnPrevPage.addEventListener(Events.ON_CLICK, e -> {
                if (currentPage > 1) { currentPage--; renderTable(); }
            });
        }
        if (btnNextPage != null) {
            btnNextPage.addEventListener(Events.ON_CLICK, e -> {
                int tp = totalPages(); if (currentPage < tp) { currentPage++; renderTable(); }
            });
        }

        // Popup buttons
        if (btnAccept      != null) btnAccept.addEventListener(Events.ON_CLICK,      e -> doAccept());
        if (btnReject      != null) btnReject.addEventListener(Events.ON_CLICK,      e -> openRejectPopup());
        if (btnRefer       != null) btnRefer.addEventListener(Events.ON_CLICK,       e -> doRefer());
        if (btnCancelPopup != null) btnCancelPopup.addEventListener(Events.ON_CLICK, e -> reviewPopup.setVisible(false));
        if (btnConfirmReject != null) btnConfirmReject.addEventListener(Events.ON_CLICK, e -> doConfirmReject());
        if (btnCancelReject  != null) btnCancelReject.addEventListener(Events.ON_CLICK,  e -> rejectReasonPopup.setVisible(false));
    }

    // ── Render ────────────────────────────────────────────────────────────

    private void renderTable() {
        if (chequeListbox == null || allCheques == null) return;

        List<InwardCheque> filtered = getFilteredList();
        int total = filtered.size();
        currentPage = Math.min(currentPage, Math.max(1, totalPages(filtered)));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        List<InwardCheque> page = filtered.subList(from, to);

        chequeListbox.getItems().clear();
        int rowNum = from + 1;

        for (InwardCheque c : page) {
            Listitem row = new Listitem();

            addCell(row, String.valueOf(rowNum++));
            addCell(row, c.getChequeNo());
            addCell(row, nvl(c.getPresentingBankName()));

            // Proc Date (chequeDate from CXF file)
            addCell(row, c.getChequeDate() != null
                    ? c.getChequeDate().format(FMT) : "—");

            // Rcvd Date (OCR-read date — may have '?' chars)
            Listcell rcvdDateCell = new Listcell(
                    c.getChequeDateOcr() != null
                            ? c.getChequeDateOcr().format(FMT) : "1?/01/2024");
            boolean dateErr = c.getChequeDateOcr() == null
                    || !c.getChequeDateOcr().equals(c.getChequeDate());
            if (dateErr) rcvdDateCell.setStyle("color:var(--danger);font-weight:600");
            row.appendChild(rcvdDateCell);

            // Proc Amount
            Listcell procAmtCell = new Listcell(
                    c.getAmount() != null ? "₹ " + fmt(c.getAmount()) : "—");
            procAmtCell.setStyle("text-align:right");
            row.appendChild(procAmtCell);

            // Rcvd Amount
            Listcell rcvdAmtCell = new Listcell(
                    c.getAmountOcr() != null ? "₹ " + fmt(c.getAmountOcr()) : "—");
            boolean amtErr = c.getAmountOcr() == null
                    || c.getAmountOcr().compareTo(c.getAmount()) != 0;
            if (amtErr) rcvdAmtCell.setStyle(
                    "text-align:right;color:var(--danger);font-weight:600");
            else rcvdAmtCell.setStyle("text-align:right");
            row.appendChild(rcvdAmtCell);

            // Status badge
            Listcell statusCell = new Listcell();
            statusCell.setStyle("text-align:center");
            Label badge = new Label(resolveStatusLabel(c.getRepairStatus()));
            badge.setSclass(resolveStatusSclass(c.getRepairStatus()));
            statusCell.appendChild(badge);
            row.appendChild(statusCell);

            // Action — Review button
            Listcell actionCell = new Listcell();
            actionCell.setStyle("text-align:center");
            Button reviewBtn = new Button("Review");
            reviewBtn.setSclass("btn-repair");
            final InwardCheque ref = c;
            reviewBtn.addEventListener(Events.ON_CLICK, ev -> openReviewPopup(ref));
            actionCell.appendChild(reviewBtn);
            row.appendChild(actionCell);

            chequeListbox.appendChild(row);
        }

        // Pagination info
        int tp = totalPages(filtered);
        if (lblPageInfo != null) {
            lblPageInfo.setValue("Page " + currentPage + " of " + tp
                    + " | " + total + " records");
        }
        if (btnPrevPage != null) btnPrevPage.setDisabled(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisabled(currentPage >= tp);

        // Pending count
        long pending = allCheques.stream()
                .filter(c -> "NEEDS_REPAIR".equalsIgnoreCase(c.getRepairStatus())
                          || c.getRepairStatus() == null)
                .count();
        if (lblPendingBadge != null) lblPendingBadge.setValue(pending + " PENDING");
    }

    // ── Popup: Review ─────────────────────────────────────────────────────

    private void openReviewPopup(InwardCheque c) {
        selectedCheque = c;

        lblProcDate.setValue(c.getChequeDate() != null
                ? c.getChequeDate().format(FMT) : "—");
        lblProcAmt.setValue(c.getAmount() != null
                ? "₹ " + fmt(c.getAmount()) : "—");
        lblRcvdDate.setValue(c.getChequeDateOcr() != null
                ? c.getChequeDateOcr().format(FMT) : "1?/01/2024");
        lblRcvdAmt.setValue(c.getAmountOcr() != null
                ? "₹ " + fmt(c.getAmountOcr()) : "—");

        dtCorrectedDate.setValue(null);
        numCorrectedAmt.setValue(null);
        txtRemarks.setValue("");

        reviewPopup.setVisible(true);
    }

    private void doAccept() {
        if (selectedCheque == null) return;
        if (dtCorrectedDate.getValue() == null) {
            Messagebox.show("Please enter corrected date.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        if (numCorrectedAmt.getValue() == null) {
            Messagebox.show("Please enter corrected amount.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        // Persist corrections
        selectedCheque.setChequeDateOcr(
                dtCorrectedDate.getValue().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        selectedCheque.setAmountOcr(BigDecimal.valueOf(numCorrectedAmt.getValue()));
        selectedCheque.setRepairStatus("REPAIRED");
        selectedCheque.setRemarks(txtRemarks.getValue());
        service.saveStep2Repair(selectedCheque);

        reviewPopup.setVisible(false);
        Messagebox.show("Cheque " + selectedCheque.getChequeNo() + " accepted ✓",
                "Success", Messagebox.OK, Messagebox.INFORMATION);
        renderTable();
    }

    private void openRejectPopup() {
        reviewPopup.setVisible(false);
        cmbRejectReason.setSelectedItem(null);
        txtRejectRemarks.setValue("");
        rejectReasonPopup.setVisible(true);
    }

    private void doConfirmReject() {
        if (cmbRejectReason.getSelectedItem() == null) {
            Messagebox.show("Please select a reject reason.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        if (selectedCheque == null) return;
        selectedCheque.setRepairStatus("REJECTED");
        selectedCheque.setRemarks(txtRejectRemarks.getValue());
        service.saveStep2Repair(selectedCheque);

        rejectReasonPopup.setVisible(false);
        Messagebox.show("Cheque " + selectedCheque.getChequeNo() + " rejected.",
                "Info", Messagebox.OK, Messagebox.INFORMATION);
        renderTable();
    }

    private void doRefer() {
        if (selectedCheque == null) return;
        selectedCheque.setRepairStatus("REFERRED_BACK");
        service.saveStep2Repair(selectedCheque);
        reviewPopup.setVisible(false);
        Messagebox.show("Cheque " + selectedCheque.getChequeNo() + " referred back.",
                "Info", Messagebox.OK, Messagebox.INFORMATION);
        renderTable();
    }

    // ── Navigation ────────────────────────────────────────────────────────

    @Listen("onClick = #btnBackToStep1")
    public void onBackToStep1() {
        Executions.getCurrent().sendRedirect(
                "/inward/inwardMicr/RejectRepair.zul?batchId=" + currentBatchId);
    }

    @Listen("onClick = #btnNextStep3")
    public void onNextStep3() {
        Executions.getCurrent().sendRedirect(
                "/inward/inwardMicr/PayeeAccount.zul?batchId=" + currentBatchId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private List<InwardCheque> getFilteredList() {
        if (allCheques == null) return Collections.emptyList();

        final String filterVal =
                (cmbFilter != null && cmbFilter.getSelectedItem() != null
                 && cmbFilter.getSelectedItem().getValue() != null)
                        ? cmbFilter.getSelectedItem().getValue().toString()
                        : "";

        return allCheques.stream()
                .filter(c -> {
                    if (!filterVal.isEmpty()) {
                        String rs = c.getRepairStatus() != null
                                ? c.getRepairStatus() : "";
                        return rs.equalsIgnoreCase(filterVal);
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private int totalPages() { return totalPages(getFilteredList()); }
    private int totalPages(List<?> list) {
        return Math.max(1, (int) Math.ceil((double) list.size() / PAGE_SIZE));
    }

    private void addCell(Listitem row, String text) {
        row.appendChild(new Listcell(text != null ? text : "—"));
    }

    private String nvl(String v) { return (v != null && !v.isEmpty()) ? v : "—"; }
    private String fmt(BigDecimal v) { return String.format("%,.2f", v); }

    private String resolveStatusLabel(String s) {
        if (s == null || s.isEmpty()) return "NEEDS REPAIR";
        return switch (s.toUpperCase()) {
            case "REPAIRED"      -> "REPAIRED";
            case "REFERRED_BACK" -> "REFERRED BACK";
            case "REJECTED"      -> "REJECTED";
            default              -> "NEEDS REPAIR";
        };
    }

    private String resolveStatusSclass(String s) {
        if (s == null || s.isEmpty()) return "badge-needs-repair";
        return switch (s.toUpperCase()) {
            case "REPAIRED"      -> "badge-repaired";
            case "REFERRED_BACK" -> "badge-referred";
            case "REJECTED"      -> "badge-fail";
            default              -> "badge-needs-repair";
        };
    }
}