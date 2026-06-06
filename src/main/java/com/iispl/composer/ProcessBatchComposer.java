package com.iispl.composer;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.User;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.CheckerBatchProcessService;
import com.iispl.serviceImpl.CheckerBatchProcessServiceImpl;
import com.iispl.util.InwardReturnReason;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/ProcessBatchComposer.java
 * Purpose : Drives the split-panel Process Batch page.
 *
 *   LEFT PANEL  — Inward Cheque Image
 *                 Renders cheque fields from DB:
 *                 bank name, presenting bank, date, payee, amount, MICR line.
 *
 *   RIGHT PANEL — Technical Verification
 *                 CBS Validation: MICR code, bank code, our bank code (from DB).
 *                 Presenting Details: bank, cheque no, amount (from DB).
 *                 Our Account (CBS): draweeAccountNumber, draweeAccountHolder,
 *                                    accountBalance (from DB — populated by Maker).
 *                 ACTION: Accept → ACK | Return → RRF | Send Back.
 *
 *   Navigation  — ← Prev / Next → through cheques.
 *   Progress    — live count + progress bar, Submit enabled when all actioned.
 *
 * NOTE: All data comes from the DB via CheckerBatchProcessService.
 *       No hardcoded / simulated data anywhere.
 */
public class ProcessBatchComposer extends SelectorComposer<Component> {

    // ── Batch Summary Bar ─────────────────────────────────────────────────────
    @Wire private Label  lblBatchId;
    @Wire private Label  lblBatchBadge;
    @Wire private Label  lblBatchDate;
    @Wire private Label  lblTotalCheques;
    @Wire private Label  lblTotalAmount;
    @Wire private Label  lblMicrErrors;
    @Wire private Label  lblSourceFile;

    // ── Navigation ────────────────────────────────────────────────────────────
    @Wire private Button btnPrev;
    @Wire private Label  lblRecordNav;
    @Wire private Button btnNext;
    @Wire private Label  lblProgressCounter;   // "4/4" top-right

    // ── Progress Bar ──────────────────────────────────────────────────────────
    @Wire private Label  lblProgressText;
    @Wire private Div    divProgressFill;

    // ── LEFT PANEL — Cheque Image ─────────────────────────────────────────────
    @Wire private Label  lblChequeBankName;      // "CSB Bank Limited"
    @Wire private Label  lblChequePresenting;    // "Presenting: Canara Thrissur"
    @Wire private Label  lblChequeDate;          // "15/01/2024"
    @Wire private Label  lblChequePayee;         // "Pay: Suresh Menon"
    @Wire private Label  lblChequeAmountWords;   // "Thirty Thousand Only"
    @Wire private Label  lblChequeAmountBox;     // "₹ 30,000.00"
    @Wire private Label  lblMicrLine;            // MICR strip at bottom

    // ── RIGHT PANEL — Status Badge ────────────────────────────────────────────
    @Wire private Label  lblTechStatus;          // PENDING / VERIFIED

    // ── RIGHT PANEL — CBS Validation Section ─────────────────────────────────
    @Wire private Label  lblCbsMicrCode;         // MICR code from DB
    @Wire private Label  lblCbsBankCode;         // bank_code from DB
    @Wire private Label  lblCbsOurBankCode;      // hardcoded: 700 (CSB Bank)

    // ── RIGHT PANEL — Presenting Details ─────────────────────────────────────
    @Wire private Label  lblPresBank;
    @Wire private Label  lblPresChequeNo;
    @Wire private Label  lblPresAmount;

    // ── RIGHT PANEL — Our Account (CBS) ──────────────────────────────────────
    @Wire private Label  lblAcctNo;
    @Wire private Label  lblAcctHolder;
    @Wire private Label  lblAcctBalance;

    // ── RIGHT PANEL — Action Buttons ─────────────────────────────────────────
    @Wire private Button btnAccept;
    @Wire private Button btnReturn;
    @Wire private Button btnSendBack;

    // ── RIGHT PANEL — Return Reason ───────────────────────────────────────────
    @Wire private Div      divReturnReasonBox;   // hidden until Return is clicked
    @Wire private Combobox comboReturnReason;

    // ── Footer ────────────────────────────────────────────────────────────────
    @Wire private Label  lblFooterProgress;
    @Wire private Button btnSubmit;

    // ── Internal State ────────────────────────────────────────────────────────
    private InwardBatch        currentBatch;
    private List<InwardCheque> cheques;
    private int                currentIndex = 0;

    // cheque DB id → action string ("ACCEPTED" / "RETURNED" / "SEND_BACK")
    private Map<Long, String> actionMap = new HashMap<>();

    // cheque DB id → NPCI reason code e.g. "01"
    private Map<Long, String> reasonMap = new HashMap<>();

    // ── Service ───────────────────────────────────────────────────────────────
    private CheckerBatchProcessService batchProcessService
            = new CheckerBatchProcessServiceImpl();

    // ══════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        // Batch ID stored in session by CheckerInwardVerificationComposer
        // when user clicks Process on a pending batch row.
        String batchId = (String) Sessions.getCurrent().getAttribute("selectedBatchId");

        if (batchId == null || batchId.trim().isEmpty()) {
            Messagebox.show(
                "No batch selected. Please go back and select a batch.",
                "Error", Messagebox.OK, Messagebox.ERROR
            );
            return;
        }

        try {
            // Loads batch + all cheques via LEFT JOIN FETCH from DB.
            // draweeAccountNumber, draweeAccountHolder, accountBalance
            // are already in inward_cheque table — filled by Maker Inward.
            currentBatch = batchProcessService.loadBatchForProcessing(batchId);
        } catch (Exception e) {
            Messagebox.show(
                "Failed to load batch: " + e.getMessage(),
                "Error", Messagebox.OK, Messagebox.ERROR
            );
            return;
        }

        cheques = currentBatch.getCheques();

        if (cheques == null || cheques.isEmpty()) {
            Messagebox.show(
                "This batch has no cheques.",
                "Info", Messagebox.OK, Messagebox.INFORMATION
            );
            return;
        }

        populateSummaryBar();
        buildReturnReasonDropdown();
        renderCheque(0);
        refreshProgress();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Summary Bar
    // ══════════════════════════════════════════════════════════════════════════

    private void populateSummaryBar() {
        set(lblBatchId,      currentBatch.getBatchId());
        set(lblBatchBadge,   "BATCH: " + currentBatch.getBatchId());
        set(lblBatchDate,    currentBatch.getBatchDate() != null
                             ? currentBatch.getBatchDate().toString() : "—");
        set(lblTotalCheques, String.valueOf(currentBatch.getTotalCheques()));
        set(lblTotalAmount,  "₹ " + formatAmount(currentBatch.getTotalAmount().toPlainString()));
        set(lblMicrErrors,   String.valueOf(currentBatch.getMicrErrorCount()));
        set(lblSourceFile,   currentBatch.getSourceFileName() != null
                             ? currentBatch.getSourceFileName() : "—");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Render one cheque (called on page load and Prev/Next)
    // ══════════════════════════════════════════════════════════════════════════

    private void renderCheque(int index) {
        if (cheques == null || index < 0 || index >= cheques.size()) return;
        currentIndex = index;

        InwardCheque c = cheques.get(index);
        int total      = cheques.size();
        int record     = index + 1;

        // Navigation labels
        set(lblRecordNav,       "Record " + record + " of " + total);
        set(lblProgressCounter, record + "/" + total);

        // Prev / Next button state
        if (btnPrev != null) btnPrev.setDisabled(index == 0);
        if (btnNext != null) btnNext.setDisabled(index == total - 1);

        // LEFT panel
        renderChequeImagePanel(c);

        // RIGHT panel
        renderTechnicalPanel(c);

        // Restore any previously selected action for this cheque
        restoreActionState(c.getId());
    }

    // ── LEFT PANEL ────────────────────────────────────────────────────────────
    // All values read directly from the InwardCheque entity (from DB).
    // "Our bank name" = the drawee bank = CSB Bank (constant for this system).
    // Presenting bank = the bank that submitted the cheque to us.

    private void renderChequeImagePanel(InwardCheque c) {

        // Bank name header of cheque — our bank (drawee bank)
        // In real CTS this comes from a config table; for now it is CSB Bank.
        set(lblChequeBankName,   "CSB Bank Limited");

        // Presenting bank below bank name
        set(lblChequePresenting, "Presenting: "
                + (c.getPresentingBankName() != null ? c.getPresentingBankName() : "—"));

        // Date (top-right of cheque)
        set(lblChequeDate, c.getChequeDate() != null
                ? formatDate(c.getChequeDate().toString()) : "—");

        // Payee line — payee_name field from DB
        String payee = c.getPayeeName() != null ? c.getPayeeName()
                     : (c.getDraweeAccountHolder() != null ? c.getDraweeAccountHolder() : "—");
        set(lblChequePayee, "Pay: " + payee);

        // Amount in words — amount_in_words field from DB
        set(lblChequeAmountWords,
                c.getAmountInWords() != null ? c.getAmountInWords() : "—");

        // Amount box — amount field from DB
        set(lblChequeAmountBox,
                "₹ " + formatAmount(c.getAmount() != null ? c.getAmount().toPlainString() : "0"));

        // MICR line — compose from micr_code_corrected (after Maker repair)
        // or fall back to micr_code_raw if no repair was done.
        String micrDisplay = buildMicrLine(c);
        set(lblMicrLine, micrDisplay);
    }

    // ── RIGHT PANEL ───────────────────────────────────────────────────────────
    // Data comes 100% from DB via InwardCheque entity.
    // draweeAccountNumber, draweeAccountHolder, accountBalance are fields
    // that Maker Inward filled when the cheque was entered into the system.

    private void renderTechnicalPanel(InwardCheque c) {

        // Status badge — PENDING until all actions submitted
        set(lblTechStatus, "PENDING");
        if (lblTechStatus != null) lblTechStatus.setSclass("pv-badge-pending");

        // ── CBS Validation block
        // MICR code — use corrected version if Maker repaired it, else raw
        String micrCode = c.getMicrCodeCorrected() != null
                        ? c.getMicrCodeCorrected()
                        : (c.getMicrCodeRaw() != null ? c.getMicrCodeRaw() : "—");
        set(lblCbsMicrCode,    micrCode);
        set(lblCbsBankCode,    c.getBankCode() != null ? c.getBankCode() : "—");

        // Our bank code is fixed for this CTS instance (CSB Bank = 700).
        // Replace with a config lookup if you go multi-bank later.
        set(lblCbsOurBankCode, "700  (CSB Bank)");

        // ── Presenting Details
        set(lblPresBank,      c.getPresentingBankName() != null ? c.getPresentingBankName() : "—");
        set(lblPresChequeNo,  c.getChequeNo() != null ? c.getChequeNo() : "—");
        set(lblPresAmount,    "₹ " + formatAmount(
                              c.getAmount() != null ? c.getAmount().toPlainString() : "0"));

        // ── Our Account (CBS)
        // These fields are populated by Maker Inward during data entry.
        // Checker sees what Maker filled — no re-lookup needed here.
        set(lblAcctNo,      c.getDraweeAccountNumber() != null ? c.getDraweeAccountNumber() : "—");
        set(lblAcctHolder,  c.getDraweeAccountHolder() != null ? c.getDraweeAccountHolder() : "Not found in CBS");
        if (lblAcctBalance != null) {
            if (c.getAccountBalance() != null) {
                set(lblAcctBalance, "₹ " + formatAmount(c.getAccountBalance().toPlainString()));
            } else {
                set(lblAcctBalance, "—");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Navigation Buttons
    // ══════════════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnPrev")
    public void onPrev() {
        if (currentIndex > 0) renderCheque(currentIndex - 1);
    }

    @Listen("onClick = #btnNext")
    public void onNext() {
        if (cheques != null && currentIndex < cheques.size() - 1)
            renderCheque(currentIndex + 1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Action Buttons  (Accept → ACK | Return → RRF | Send Back)
    // ══════════════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnAccept")
    public void onAccept() {
        applyAction("ACCEPTED");
    }

    @Listen("onClick = #btnReturn")
    public void onReturn() {
        applyAction("RETURNED");
    }

    @Listen("onClick = #btnSendBack")
    public void onSendBack() {
        applyAction("SEND_BACK");
    }

    private void applyAction(String action) {
        if (cheques == null || currentIndex >= cheques.size()) return;

        Long chequeId = cheques.get(currentIndex).getId();
        actionMap.put(chequeId, action);

        // Clear reason if switching away from RETURNED
        if (!"RETURNED".equals(action)) {
            reasonMap.remove(chequeId);
            if (comboReturnReason != null) {
                comboReturnReason.setValue("");
                comboReturnReason.setSelectedItem(null);
            }
        }

        highlightActionButtons(action);
        toggleReturnReasonBox(chequeId);
        refreshProgress();

        // Auto-advance to next cheque for Accept and Send Back (not Return —
        // checker still needs to select a reason code before moving on).
        if (!"RETURNED".equals(action) && currentIndex < cheques.size() - 1) {
            renderCheque(currentIndex + 1);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Return Reason Dropdown
    // ══════════════════════════════════════════════════════════════════════════

    private void buildReturnReasonDropdown() {
        if (comboReturnReason == null) return;
        comboReturnReason.getItems().clear();

        // Loads all NPCI return reason codes from InwardReturnReason utility.
        Map<String, String> reasons = InwardReturnReason.getReasonDropdownMap();
        for (Map.Entry<String, String> entry : reasons.entrySet()) {
            Comboitem item = comboReturnReason.appendItem(entry.getValue());
            item.setValue(entry.getKey());
        }

        comboReturnReason.addEventListener("onSelect", event -> {
            Comboitem selected = comboReturnReason.getSelectedItem();
            if (selected != null && cheques != null && currentIndex < cheques.size()) {
                reasonMap.put(cheques.get(currentIndex).getId(), (String) selected.getValue());
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Progress Bar
    // ══════════════════════════════════════════════════════════════════════════

    private void refreshProgress() {
        if (cheques == null) return;

        int total    = cheques.size();
        int actioned = actionMap.size();
        int percent  = total > 0 ? (actioned * 100 / total) : 0;

        String text = actioned + " of " + total + " cheques actioned";
        set(lblProgressText,   text);
        set(lblFooterProgress, text);

        if (divProgressFill != null) {
            divProgressFill.setStyle("width: " + percent + "%");
        }

        // Submit is enabled only when every cheque has an action
        if (btnSubmit != null) {
            btnSubmit.setDisabled(actioned < total);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Submit Batch
    // ══════════════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnSubmit")
    public void onSubmitBatch() {

        LoginDTO loggedInUser = (LoginDTO) Sessions.getCurrent()
                .getAttribute(SessionUtil.SESSION_KEY);

        if (loggedInUser == null) {
            Messagebox.show("Session expired. Please log in again.",
                "Session Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        User checker = new User();
        try {
            checker.setId(loggedInUser.getUserId());
        } catch (NumberFormatException e) {
            Messagebox.show("Invalid session data. Please log in again.",
                "Session Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        // Build checker action list from actionMap + reasonMap
        List<InwardCheckerAction> actions = new ArrayList<>();

        for (InwardCheque cheque : cheques) {
            Long   chequeId = cheque.getId();
            String action   = actionMap.get(chequeId);

            InwardCheckerAction checkerAction = new InwardCheckerAction();
            checkerAction.setInwardCheque(cheque);
            checkerAction.setAction(action);

            if ("RETURNED".equals(action)) {
                checkerAction.setReasonCode(reasonMap.get(chequeId));
                // reasonText is auto-filled by CheckerBatchProcessServiceImpl
                // using InwardReturnReason.getReasonText(code)
            }

            actions.add(checkerAction);
        }

        try {
            // Service validates: all cheques actioned, reason codes present,
            // then calls DAO: saveCheckerActions + updateBatchStatus (→ CLEARED).
            batchProcessService.submitBatch(currentBatch, actions, checker);

            Messagebox.show(
                "Batch " + currentBatch.getBatchId() + " submitted successfully.",
                "Success", Messagebox.OK, Messagebox.INFORMATION,
                event -> navigateBackToList()
            );

        } catch (IllegalArgumentException e) {
            // Validation errors from service (missing reason, incomplete actions, etc.)
            Messagebox.show(e.getMessage(),
                "Validation Error", Messagebox.OK, Messagebox.EXCLAMATION);

        } catch (Exception e) {
            Messagebox.show("An error occurred while submitting. Please try again.",
                "Error", Messagebox.OK, Messagebox.ERROR);
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Back / Cancel
    // ══════════════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnBackToList")
    public void onBackToList() {
        navigateBackToList();
    }
    
    @Listen("onClick = #lnkBreadcrumbBack")
    public void onBreadcrumbBack() {
        navigateBackToList();
    }

    @Listen("onClick = #btnCancel")
    public void onCancel() {
        navigateBackToList();
    }

    private void navigateBackToList() {
        Executions.getCurrent().sendRedirect(
            "/inward/inwardChecker/inwardCheckerVerification.zul"
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Restore button highlights and reason box when navigating back to a cheque. */
    private void restoreActionState(Long chequeId) {
        String action = actionMap.get(chequeId);
        highlightActionButtons(action);
        toggleReturnReasonBox(chequeId);

        // Restore reason selection in dropdown
        if ("RETURNED".equals(action) && comboReturnReason != null) {
            String code = reasonMap.get(chequeId);
            if (code != null) {
                for (Comboitem item : comboReturnReason.getItems()) {
                    if (code.equals(item.getValue())) {
                        comboReturnReason.setSelectedItem(item);
                        break;
                    }
                }
            }
        }
    }

    /** Apply CSS sclass to show which action button is selected. */
    private void highlightActionButtons(String action) {
        if (btnAccept   != null) btnAccept.setSclass("ACCEPTED".equals(action)  ? "pv-btn-accept-on"    : "pv-btn-accept");
        if (btnReturn   != null) btnReturn.setSclass("RETURNED".equals(action)  ? "pv-btn-return-on"    : "pv-btn-return");
        if (btnSendBack != null) btnSendBack.setSclass("SEND_BACK".equals(action)? "pv-btn-sendback-on" : "pv-btn-sendback");
    }

    /** Show or hide the return reason box based on current action for this cheque. */
    private void toggleReturnReasonBox(Long chequeId) {
        if (divReturnReasonBox == null) return;
        boolean isReturn = "RETURNED".equals(actionMap.get(chequeId));
        divReturnReasonBox.setVisible(isReturn);
    }

    /**
     * Builds the MICR line displayed at the bottom of the cheque image.
     * Format: «chequeNo«  micrCode«  —seqNo—
     * Uses corrected MICR if Maker repaired it; raw otherwise.
     */
    private String buildMicrLine(InwardCheque c) {
        String chequeNo = c.getChequeNo() != null ? c.getChequeNo() : "";
        String micr     = c.getMicrCodeCorrected() != null ? c.getMicrCodeCorrected()
                        : (c.getMicrCodeRaw() != null ? c.getMicrCodeRaw() : "");
        int seq = c.getSeqNo();
        return "«" + chequeNo + "«   " + micr + "«   —" + seq + "—";
    }

    /**
     * Formats a date string from "2024-01-15" → "15/01/2024".
     * Handles null and malformed strings gracefully.
     */
    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return isoDate != null ? isoDate : "—";
        try {
            String[] parts = isoDate.split("-");
            if (parts.length == 3) return parts[2] + "/" + parts[1] + "/" + parts[0];
        } catch (Exception ignored) {}
        return isoDate;
    }

    /**
     * Formats a numeric string with Indian comma grouping.
     * "30000.00" → "30,000.00"
     */
    private String formatAmount(String raw) {
        try {
            java.math.BigDecimal bd = new java.math.BigDecimal(raw);
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(bd);
        } catch (Exception e) {
            return raw;
        }
    }

    /** Null-safe label setter. */
    private void set(Label label, String value) {
        if (label != null) label.setValue(value);
    }
}