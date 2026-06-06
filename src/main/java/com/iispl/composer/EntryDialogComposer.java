package com.iispl.composer;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.ChequeRepairService;
import com.iispl.serviceImpl.ChequeRepairServiceImpl;

/**
 * Step3EntryDialogComposer
 *
 * Modal dialog composer for entering payee name and account number (Step 3).
 * Receives chequeId via Executions.getCurrent().getArg().
 */
public class EntryDialogComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // ── Wired ─────────────────────────────────────────────────────────────

    @Wire("#step3EntryDialog")
    private Window step3EntryDialog;

    @Wire("#lblChequeNo")
    private Label lblChequeNo;

    @Wire("#lblBank")
    private Label lblBank;

    @Wire("#lblAmount")
    private Label lblAmount;

    @Wire("#tbxPayeeName")
    private Textbox tbxPayeeName;

    @Wire("#tbxAccountNo")
    private Textbox tbxAccountNo;

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
            step3EntryDialog.detach();
            return;
        }

        cheque = service.getChequeById(chequeId);
        if (cheque == null) {
            Messagebox.show("Cheque not found.", "Error", Messagebox.OK, Messagebox.ERROR);
            step3EntryDialog.detach();
            return;
        }

        populateFields();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void populateFields() {
        lblChequeNo.setValue(nvl(cheque.getChequeNo()));
        lblBank.setValue(nvl(cheque.getPresentingBankName()));
        lblAmount.setValue(
                cheque.getAmount() != null ? "₹ " + String.format("%,.2f", cheque.getAmount()) : "₹ 0.00");

        // Pre-fill if values already exist (editing scenario)
        if (cheque.getPayeeName() != null) {
            tbxPayeeName.setValue(cheque.getPayeeName());
        }
        if (cheque.getDraweeAccountNumber() != null) {
            tbxAccountNo.setValue(cheque.getDraweeAccountNumber());
        }
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    // ── Event handlers ────────────────────────────────────────────────────

    @Listen("onClick = #btnSave")
    public void onSave() {
        String payeeName = tbxPayeeName.getValue();
        String accountNo = tbxAccountNo.getValue();

        if (payeeName == null || payeeName.trim().isEmpty()) {
            Messagebox.show("Please enter the payee name.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        if (accountNo == null || accountNo.trim().isEmpty()) {
            Messagebox.show("Please enter the account number.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        cheque.setPayeeName(payeeName.trim());
        cheque.setDraweeAccountNumber(accountNo.trim());
        cheque.setRepairStatus("REPAIRED");

        boolean saved = service.savePayeeAccountRepair(cheque);
        if (saved) {
            Messagebox.show("Saved successfully.", "Success",
                    Messagebox.OK, Messagebox.INFORMATION,
                    e -> step3EntryDialog.detach());
        } else {
            Messagebox.show("Save failed. Please try again.", "Error",
                    Messagebox.OK, Messagebox.ERROR);
        }
    }

    @Listen("onClick = #btnCancel")
    public void onCancel() {
        step3EntryDialog.detach();
    }
}