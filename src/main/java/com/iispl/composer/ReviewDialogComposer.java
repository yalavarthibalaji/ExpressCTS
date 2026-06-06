package com.iispl.composer;


import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Window;

import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.ChequeRepairService;
import com.iispl.serviceImpl.ChequeRepairServiceImpl;

/**
 * Step2ReviewDialogComposer
 *
 * Modal dialog composer for reviewing and correcting date & amount (Step 2).
 * Receives chequeId and batchId via Executions.getCurrent().getArg().
 */
public class ReviewDialogComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Wired ─────────────────────────────────────────────────────────────

    @Wire("#step2ReviewDialog")
    private Window step2ReviewDialog;

    @Wire("#lblChequeNo")
    private Label lblChequeNo;

    @Wire("#lblBank")
    private Label lblBank;

    @Wire("#lblProcDate")
    private Label lblProcDate;

    @Wire("#lblProcAmount")
    private Label lblProcAmount;

    @Wire("#dtbRcvdDate")
    private Datebox dtbRcvdDate;

    @Wire("#dbxRcvdAmount")
    private Decimalbox dbxRcvdAmount;

    // ── State ─────────────────────────────────────────────────────────────

    private InwardCheque cheque;
    private final ChequeRepairService service = new ChequeRepairServiceImpl();

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        Long chequeId = (Long) Executions.getCurrent().getArg().get("chequeId");
        if (chequeId == null) {
            Messagebox.show("Missing cheque ID.", "Error", Messagebox.OK, Messagebox.ERROR);
            step2ReviewDialog.detach();
            return;
        }

        cheque = service.getChequeById(chequeId);
        if (cheque == null) {
            Messagebox.show("Cheque not found.", "Error", Messagebox.OK, Messagebox.ERROR);
            step2ReviewDialog.detach();
            return;
        }

        populateFields();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void populateFields() {
        lblChequeNo.setValue(nvl(cheque.getChequeNo()));
        lblBank.setValue(nvl(cheque.getPresentingBankName()));
        lblProcDate.setValue(
                cheque.getChequeDate() != null ? cheque.getChequeDate().format(DATE_FMT) : "");
        lblProcAmount.setValue(
                cheque.getAmount() != null ? "₹ " + String.format("%,.2f", cheque.getAmount()) : "₹ 0.00");

        // Pre-fill editable fields with OCR values (or fall back to declared)
        if (cheque.getChequeDateOcr() != null) {
            dtbRcvdDate.setValue(
                    Date.from(cheque.getChequeDateOcr().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        } else if (cheque.getChequeDate() != null) {
            dtbRcvdDate.setValue(
                    Date.from(cheque.getChequeDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        BigDecimal ocrAmt = cheque.getAmountOcr() != null ? cheque.getAmountOcr() : cheque.getAmount();
        dbxRcvdAmount.setValue(ocrAmt);
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    // ── Event handlers ────────────────────────────────────────────────────

    @Listen("onClick = #btnSave")
    public void onSave() {
        Date selectedDate = dtbRcvdDate.getValue();
        BigDecimal selectedAmt = dbxRcvdAmount.getValue();

        if (selectedDate == null) {
            Messagebox.show("Please enter the received date.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        if (selectedAmt == null || selectedAmt.compareTo(BigDecimal.ZERO) <= 0) {
            Messagebox.show("Please enter a valid received amount.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        cheque.setChequeDateOcr(
                selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        cheque.setAmountOcr(selectedAmt);
        cheque.setRepairStatus("REPAIRED");

        boolean saved = service.saveDateAmountRepair(cheque);
        if (saved) {
            Messagebox.show("Saved successfully.", "Success",
                    Messagebox.OK, Messagebox.INFORMATION,
                    e -> step2ReviewDialog.detach());
        } else {
            Messagebox.show("Save failed. Please try again.", "Error",
                    Messagebox.OK, Messagebox.ERROR);
        }
    }

    @Listen("onClick = #btnCancel")
    public void onCancel() {
        step2ReviewDialog.detach();
    }
}