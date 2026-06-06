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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PayeeAccountComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // ── Wired ─────────────────────────────────────────────────────────────

    @Wire("#lblBatchBadge")    private Label    lblBatchBadge;
    @Wire("#lblPendingBadge")  private Label    lblPendingBadge;
    @Wire("#lblPageInfo")      private Label    lblPageInfo;
    @Wire("#chequeListbox")    private Listbox  chequeListbox;
    @Wire("#cmbFilter")        private Combobox cmbFilter;
    @Wire("#btnPrevPage")      private Button   btnPrevPage;
    @Wire("#btnNextPage")      private Button   btnNextPage;

    // Entry popup
    @Wire("#entryPopup")       private Window   entryPopup;
    @Wire("#lblPopupChqNo")    private Label    lblPopupChqNo;
    @Wire("#lblPopupAmt")      private Label    lblPopupAmt;
    @Wire("#lblPopupBank")     private Label    lblPopupBank;
    @Wire("#txtPayeeName")     private Textbox  txtPayeeName;
    @Wire("#txtAccNo")         private Textbox  txtAccNo;
    @Wire("#txtEntryRemarks")  private Textbox  txtEntryRemarks;
    @Wire("#btnSaveEntry")     private Button   btnSaveEntry;
    @Wire("#btnReferEntry")    private Button   btnReferEntry;
    @Wire("#btnCancelEntry")   private Button   btnCancelEntry;

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
            var batches = service.getRepairEligibleBatches();
            if (batches != null && !batches.isEmpty()) {
                currentBatchId = batches.get(0).getBatchId();
            }
        }

        if (currentBatchId != null) {
            allCheques = service.getStep3ChequesByBatchId(currentBatchId);
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
                if (currentPage < totalPages()) { currentPage++; renderTable(); }
            });
        }

        // Popup buttons
        if (btnSaveEntry != null) {
            btnSaveEntry.addEventListener(Events.ON_CLICK, e -> doSaveEntry());
        }
        if (btnReferEntry != null) {
            btnReferEntry.addEventListener(Events.ON_CLICK, e -> doReferEntry());
        }
        if (btnCancelEntry != null) {
            btnCancelEntry.addEventListener(Events.ON_CLICK,
                    e -> entryPopup.setVisible(false));
        }
    }

    // ── Render ────────────────────────────────────────────────────────────

    private void renderTable() {
        if (chequeListbox == null || allCheques == null) return;

        List<InwardCheque> filtered = getFilteredList();
        int total = filtered.size();
        currentPage = Math.min(currentPage, Math.max(1, totalPages(filtered)));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        chequeListbox.getItems().clear();
        int rowNum = from + 1;

        for (InwardCheque c : filtered.subList(from, to)) {
            Listitem row = new Listitem();

            addCell(row, String.valueOf(rowNum++));
            addCell(row, c.getChequeNo());
            addCell(row, nvl(c.getPresentingBankName()));

            // Amount — right-aligned
            Listcell amtCell = new Listcell(
                    c.getAmount() != null ? "₹ " + fmt(c.getAmount()) : "—");
            amtCell.setStyle("text-align:right");
            row.appendChild(amtCell);

            // Payee Name — show "—" if not yet entered (matches Image 3)
            addCell(row, nvlDash(c.getPayeeName()));

            // Account No — show "—" if not yet entered
            addCell(row, nvlDash(c.getDraweeAccountNumber()));

            // Status badge
            Listcell statusCell = new Listcell();
            statusCell.setStyle("text-align:center");
            Label badge = new Label(resolveStatusLabel(c.getRepairStatus()));
            badge.setSclass(resolveStatusSclass(c.getRepairStatus()));
            statusCell.appendChild(badge);
            row.appendChild(statusCell);

            // Action — Enter button
            Listcell actionCell = new Listcell();
            actionCell.setStyle("text-align:center");
            Button enterBtn = new Button("Enter");
            enterBtn.setSclass("btn-repair");
            final InwardCheque ref = c;
            enterBtn.addEventListener(Events.ON_CLICK, ev -> openEntryPopup(ref));
            actionCell.appendChild(enterBtn);
            row.appendChild(actionCell);

            chequeListbox.appendChild(row);
        }

        // Pagination
        int tp = totalPages(filtered);
        if (lblPageInfo != null) {
            lblPageInfo.setValue("Page " + currentPage + " of " + tp
                    + " | " + total + " records");
        }
        if (btnPrevPage != null) btnPrevPage.setDisabled(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisabled(currentPage >= tp);

        // Pending count — those with no payee or account yet
        long pending = allCheques.stream()
                .filter(c -> c.getPayeeName() == null || c.getPayeeName().isEmpty()
                          || c.getDraweeAccountNumber() == null
                          || c.getDraweeAccountNumber().isEmpty())
                .count();
        if (lblPendingBadge != null) lblPendingBadge.setValue(pending + " PENDING");
    }

    // ── Popup ─────────────────────────────────────────────────────────────

    private void openEntryPopup(InwardCheque c) {
        selectedCheque = c;
        lblPopupChqNo.setValue(nvl(c.getChequeNo()));
        lblPopupAmt.setValue(c.getAmount() != null ? "₹ " + fmt(c.getAmount()) : "—");
        lblPopupBank.setValue(nvl(c.getPresentingBankName()));
        txtPayeeName.setValue(nvl2(c.getPayeeName()));
        txtAccNo.setValue(nvl2(c.getDraweeAccountNumber()));
        txtEntryRemarks.setValue(nvl2(c.getRemarks()));
        entryPopup.setVisible(true);
    }

    private void doSaveEntry() {
        if (selectedCheque == null) return;

        String payee = txtPayeeName.getValue() != null
                ? txtPayeeName.getValue().trim() : "";
        String accNo = txtAccNo.getValue() != null
                ? txtAccNo.getValue().trim() : "";

        if (payee.isEmpty()) {
            Messagebox.show("Payee Name is required.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        if (accNo.isEmpty()) {
            Messagebox.show("Account Number is required.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        selectedCheque.setPayeeName(payee);
        selectedCheque.setDraweeAccountNumber(accNo);
        selectedCheque.setRemarks(txtEntryRemarks.getValue());
        service.saveStep3Entry(selectedCheque);   // sets ENTRY_DONE inside

        entryPopup.setVisible(false);
        Messagebox.show("Entry saved for cheque " + selectedCheque.getChequeNo() + " ✓",
                "Success", Messagebox.OK, Messagebox.INFORMATION);
        renderTable();
    }

    private void doReferEntry() {
        if (selectedCheque == null) return;
        selectedCheque.setRepairStatus("REFERRED_BACK");
        service.saveStep3Entry(selectedCheque);
        entryPopup.setVisible(false);
        Messagebox.show("Cheque " + selectedCheque.getChequeNo() + " referred back.",
                "Info", Messagebox.OK, Messagebox.INFORMATION);
        renderTable();
    }

    // ── Navigation ────────────────────────────────────────────────────────

    @Listen("onClick = #btnBackToStep2")
    public void onBackToStep2() {
        Executions.getCurrent().sendRedirect(
                "/inward/inwardMicr/DateAmount.zul?batchId=" + currentBatchId);
    }

    @Listen("onClick = #btnProceedChecker")
    public void onProceedToChecker() {
        Messagebox.show(
            "Proceed to Inward Checker with batch " + currentBatchId + "?",
            "Confirm", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
            evt -> {
                if (Messagebox.ON_YES.equals(evt.getName())) {
                    service.proceedToInwardChecker(currentBatchId);
                    Executions.getCurrent().sendRedirect(
                            "/inward/inwardChecker/InwardChecker.zul"
                            + "?batchId=" + currentBatchId);
                }
            });
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
                                ? c.getRepairStatus() : "NEEDS_ENTRY";
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

    private String nvl(String v)     { return (v != null && !v.isEmpty()) ? v : "—";  }
    private String nvlDash(String v) { return (v != null && !v.isEmpty()) ? v : "—"; }
    private String nvl2(String v)    { return v != null ? v : "";                      }
    private String fmt(BigDecimal v) { return String.format("%,.2f", v);               }

    private String resolveStatusLabel(String s) {
        if (s == null || s.isEmpty()) return "NEEDS ENTRY";
        return switch (s.toUpperCase()) {
            case "ENTRY_DONE"    -> "COMPLETED";
            case "REFERRED_BACK" -> "REFERRED BACK";
            default              -> "NEEDS ENTRY";
        };
    }

    private String resolveStatusSclass(String s) {
        if (s == null || s.isEmpty()) return "badge-needs-repair";
        return switch (s.toUpperCase()) {
            case "ENTRY_DONE"    -> "badge-repaired";
            case "REFERRED_BACK" -> "badge-referred";
            default              -> "badge-needs-repair";
        };
    }
}