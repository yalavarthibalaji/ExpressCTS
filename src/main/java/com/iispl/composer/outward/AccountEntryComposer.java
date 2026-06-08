package com.iispl.composer.outward;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import com.iispl.dto.CbsValidationResult;
import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.service.AccountEntryService;
import com.iispl.service.CbsService;
import com.iispl.serviceImpl.AccountEntryServiceImpl;
import com.iispl.serviceImpl.CbsServiceImpl;
import com.iispl.util.AmountToWords;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/outward/AccountEntryComposer.java
 * Purpose : Account & Amount Entry screen for Maker Outward.
 *
 * CBS Validation Rules:
 *   1. Maker must click "Validate" before clicking "Save & Next"
 *   2. If not validated → Save is blocked with notification
 *   3. If account ACTIVE → Save allowed, CBS holder name stored
 *   4. If account INACTIVE/CLOSED/FROZEN → Save blocked,
 *      reject panel auto-opens with appropriate reason
 *   5. If account NOT FOUND → Save blocked, reject panel opens
 *   6. If CBS service error → Retry allowed (save still blocked)
 *   7. Changing account number after validation → resets validation
 *   8. Loading a new cheque → always resets validation
 */
public class AccountEntryComposer extends SelectorComposer<Component> {

    private final AccountEntryService entryService = new AccountEntryServiceImpl();
    private final CbsService          cbsService   = new CbsServiceImpl();
    private final DecimalFormat        moneyFmt     = new DecimalFormat("#,##0.00");

    // ── Topbar ──
    @Wire private Label  userAvatar;
    @Wire private Label  userName;
    @Wire private Label  userRole;

    // ── Three views ──
    @Wire private Div emptyStateView;
    @Wire private Div batchSelectView;
    @Wire private Div entryView;

    // ── Batch Select View ──
    @Wire private Rows batchSelectRows;

    // ── Entry View — Stats bar ──
    @Wire private Label statsBatchId;
    @Wire private Label statsTotal;
    @Wire private Label statsDoneCount;
    @Wire private Label statsRemCount;

    // ── Entry View — Navigation ──
    @Wire private Label navLabel;

    // ── Left Panel — Cheque Images ──
    @Wire private Image frontImage;
    @Wire private Image backImage;

    // ── Right Panel — Cheque Info (read-only) ──
    @Wire private Label chqNoDisplay;
    @Wire private Label chqDateDisplay;
    @Wire private Label entryStatusBadge;

    // ── Right Panel — Account & CBS Validation ──
    @Wire private Textbox accountNoBox;
    @Wire private Button  validateBtn;
    @Wire private Div     valResultDiv;
    @Wire private Label   valMessageLabel;  // success / warning / error message
    @Wire private Div     valDetailsDiv;    // account details panel
    @Wire private Label   valHolderLabel;   // account holder name
    @Wire private Label   valTypeLabel;     // SAVINGS / CURRENT
    @Wire private Label   valStatusBadge;   // ACTIVE / INACTIVE badge
    @Wire private Label   valIfscLabel;     // IFSC code
    @Wire private Label   valBalanceLabel;  // balance

    // ── Right Panel — Amount Entry ──
    @Wire private Decimalbox amountBox;
    @Wire private Textbox    chequeDateBox;
    @Wire private Textbox    amountInWordsBox;

    // ── Right Panel — Payee ──
    @Wire private Textbox payeeNameBox;

    // ── Footer ──
    @Wire private Div     rejectPanel;
    @Wire private Listbox rejectReasonBox;
    @Wire private Textbox rejectRemarksBox;

    // ── State ──
    private List<OutwardCheque> pendingList;
    private int                 currentIndex    = 0;
    private OutwardBatch        currentBatch;
    private String              batchId;
    private Long                currentMakerId;
    private boolean             cameFromSidebar = false;
    private int                 totalCheques    = 0;

    // ── CBS Validation State (reset per cheque) ──
    private boolean             isValidated   = false;
    private CbsValidationResult cbsResult     = null;

    // ════════════════════════════════════════════════════
    //  Page Init
    // ════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        if (!"MAKER_OUTWARD".equals(dto.getRoleCode())) {
            Executions.sendRedirect(
                SessionUtil.getDashboardUrlFor(dto.getRoleCode()));
            return;
        }

        userAvatar.setValue(dto.getInitials());
        userName.setValue(dto.getFullName());
        userRole.setValue("Maker — Outward");
        currentMakerId = dto.getUserId();

        showView("none");

        batchId = Executions.getCurrent().getParameter("batchId");

        if (batchId != null && !batchId.trim().isEmpty()) {
            cameFromSidebar = false;
            currentBatch    = entryService.getBatch(batchId.trim());

            if (currentBatch == null) {
                Clients.showNotification(
                    "Batch not found: " + batchId,
                    "error", null, "top_center", 3000);
                Executions.sendRedirect(
                    "/outward/batchUpload/batchUpload.zul");
                return;
            }
            showView("entry");
            loadEntryView();
        } else {
            cameFromSidebar = true;
            loadBatchSelectView();
        }
    }

    // ════════════════════════════════════════════════════
    //  View Manager
    // ════════════════════════════════════════════════════

    private void showView(String view) {
        emptyStateView.setVisible("empty".equals(view));
        batchSelectView.setVisible("batchSelect".equals(view));
        entryView.setVisible("entry".equals(view));
    }

    // ════════════════════════════════════════════════════
    //  Topbar
    // ════════════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }

    // ════════════════════════════════════════════════════
    //  Empty State
    // ════════════════════════════════════════════════════

    @Listen("onClick = #goToMicrRepairBtn")
    public void onGoToMicrRepair() {
        Executions.sendRedirect("/outward/micrRepair/micrRepair.zul");
    }

    // ════════════════════════════════════════════════════
    //  Batch Select View
    // ════════════════════════════════════════════════════

    private void loadBatchSelectView() {
        List<OutwardBatch> batches =
            entryService.getEntryBatches(currentMakerId);

        if (batches == null || batches.isEmpty()) {
            showView("empty");
            return;
        }

        showView("batchSelect");
        batchSelectRows.getChildren().clear();
        int idx = 1;
        for (OutwardBatch b : batches) {
            batchSelectRows.appendChild(buildBatchSelectRow(idx++, b));
        }
    }

    private Row buildBatchSelectRow(int idx, final OutwardBatch b) {
        Row row = new Row();
        row.appendChild(new Label(String.valueOf(idx)));

        Label batchIdLbl = new Label(safe(b.getBatchId()));
        batchIdLbl.setSclass("mono");
        row.appendChild(batchIdLbl);

        row.appendChild(new Label(String.valueOf(b.getChequeCount())));
        row.appendChild(new Label(
            b.getActualAmount() != null
            ? "₹" + moneyFmt.format(b.getActualAmount()) : "—"));

        int pending = 0;
        try {
            pending = entryService.getPendingCheques(b.getId()).size();
        } catch (Exception e) {
            pending = b.getChequeCount();
        }
        row.appendChild(new Label(String.valueOf(pending)));
        row.appendChild(new Label(String.valueOf(b.getChequeCount() - pending)));

        Label statusBadge = new Label("Entry Done");
        statusBadge.setSclass("badge b-info");
        row.appendChild(statusBadge);

        Button selectBtn = new Button("Select");
        selectBtn.setSclass("btn bp btn-sm");
        selectBtn.addEventListener(Events.ON_CLICK,
            new EventListener<Event>() {
                @Override public void onEvent(Event e) {
                    onBatchSelected(b);
                }
            });
        row.appendChild(selectBtn);
        return row;
    }

    private void onBatchSelected(OutwardBatch batch) {
        currentBatch = batch;
        batchId      = batch.getBatchId();
        showView("entry");
        loadEntryView();
    }

    // ════════════════════════════════════════════════════
    //  Entry View Setup
    // ════════════════════════════════════════════════════

    private void loadEntryView() {
        pendingList  = entryService.getPendingCheques(currentBatch.getId());
        totalCheques = currentBatch.getChequeCount();
        currentIndex = 0;

        refreshStatsBar();

        if (pendingList.isEmpty()) {
            Clients.showNotification(
                "All entries already done for this batch.",
                "info", null, "top_center", 3000);
            return;
        }

        loadChequeForm(pendingList.get(currentIndex));
    }

    private void refreshStatsBar() {
        statsBatchId.setValue(safe(batchId));
        statsTotal.setValue(currentBatch.getActualAmount() != null
            ? "₹" + moneyFmt.format(currentBatch.getActualAmount()) : "—");
        statsDoneCount.setValue(
            String.valueOf(totalCheques - pendingList.size()));
        statsRemCount.setValue(String.valueOf(pendingList.size()));
    }

    // ════════════════════════════════════════════════════
    //  Load One Cheque into the Form
    // ════════════════════════════════════════════════════

    private void loadChequeForm(OutwardCheque cheque) {
        navLabel.setValue((currentIndex + 1) + " of " + pendingList.size());

        entryStatusBadge.setValue("Pending");
        entryStatusBadge.setSclass("badge b-pend");

        // Cheque info
        chqNoDisplay.setValue(safe(cheque.getChequeNo()));
        chqDateDisplay.setValue(
            cheque.getChequeDate() != null
            ? cheque.getChequeDate().toString() : "—");

        // Account section
        accountNoBox.setValue(safe(cheque.getAccountNo()));

        // ── RESET CBS validation state for every new cheque ──
        resetCbsValidation();

        // Amount pre-fill
        if (cheque.getAmount() != null) {
            amountBox.setValue(cheque.getAmount());
            amountInWordsBox.setValue(
                AmountToWords.convert(cheque.getAmount().doubleValue()));
        } else {
            amountBox.setValue((BigDecimal) null);
            amountInWordsBox.setValue("");
        }

        chequeDateBox.setValue(
            cheque.getChequeDate() != null
            ? cheque.getChequeDate().toString() : "");

        payeeNameBox.setValue(safe(cheque.getPayeeName()));

        loadImages(cheque);
        rejectPanel.setVisible(false);
    }

    private void loadImages(OutwardCheque cheque) {
        try {
            String fp = cheque.getFrontImagePath();
            frontImage.setSrc(fp != null && !fp.trim().isEmpty()
                ? "/imageServlet?path="
                  + URLEncoder.encode(fp.trim(), "UTF-8") : "");

            String bp = cheque.getBackImagePath();
            backImage.setSrc(bp != null && !bp.trim().isEmpty()
                ? "/imageServlet?path="
                  + URLEncoder.encode(bp.trim(), "UTF-8") : "");
        } catch (UnsupportedEncodingException e) {
            System.err.println("AccountEntryComposer → image URL encode failed: "
                    + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════
    //  CBS Validation State Reset
    // ════════════════════════════════════════════════════

    private void resetCbsValidation() {
        isValidated = false;
        cbsResult   = null;
        valResultDiv.setVisible(false);
        valDetailsDiv.setVisible(false);
        valMessageLabel.setValue("");
        valMessageLabel.setSclass("val-ok");
        validateBtn.setSclass("btn bo");
        validateBtn.setLabel("Validate");
        validateBtn.setDisabled(false);
    }

    // ════════════════════════════════════════════════════
    //  Account Number Change — Reset Validation
    //  If maker edits the account number after validating,
    //  the validation result is no longer valid.
    // ════════════════════════════════════════════════════

    @Listen("onChange = #accountNoBox; onChanging = #accountNoBox")
    public void onAccountNoChange() {
        if (isValidated || cbsResult != null) {
            resetCbsValidation();
        }
    }

    // ════════════════════════════════════════════════════
    //  CBS Validate Button
    // ════════════════════════════════════════════════════

    @Listen("onClick = #validateBtn")
    public void onValidate() {
        String accNo = accountNoBox.getValue();

        if (isBlank(accNo)) {
            Clients.showNotification(
                "Please enter an account number before validating.",
                "warning", null, "top_center", 2000);
            return;
        }

        // ── Show loading state ──
        validateBtn.setLabel("Validating...");
        validateBtn.setDisabled(true);
        valResultDiv.setVisible(false);
        valDetailsDiv.setVisible(false);

        System.out.println("AccountEntryComposer → CBS validate: " + accNo.trim());

        // ── Call CBS (Firebase) ──
        CbsValidationResult result = cbsService.validateAccount(accNo.trim());
        cbsResult = result;

        // ── Reset button ──
        validateBtn.setLabel("Validate");
        validateBtn.setDisabled(false);

        // ── Display result ──
        displayCbsResult(result);
    }

    /**
     * Displays the CBS validation result in the UI.
     * Updates: valResultDiv, valMessageLabel, valDetailsDiv,
     *          valHolderLabel, valTypeLabel, valStatusBadge,
     *          valIfscLabel, valBalanceLabel
     */
    private void displayCbsResult(CbsValidationResult result) {
        valResultDiv.setVisible(true);

        if (result.isFound() && result.isActive()) {
            // ══ Case 1: ACTIVE account — save allowed ══
            isValidated = true;
            validateBtn.setSclass("btn bs");   // green button = validated

            valMessageLabel.setValue(
                "✓ Account validated — " + safe(result.getAccountHolderName()));
            valMessageLabel.setSclass("val-ok");

            valDetailsDiv.setVisible(true);
            valHolderLabel.setValue(safe(result.getAccountHolderName()));
            valTypeLabel.setValue(safe(result.getAccountType()));
            valStatusBadge.setValue("ACTIVE");
            valStatusBadge.setSclass("badge b-pass");
            valIfscLabel.setValue(safe(result.getIfscCode()));
            valBalanceLabel.setValue("₹" + moneyFmt.format(result.getBalance()));

            System.out.println("AccountEntryComposer → Validation PASSED: "
                    + result.getAccountHolderName());

        } else if (result.isFound() && !result.isActive()) {
            // ══ Case 2: Account found but INACTIVE/CLOSED/FROZEN ══
            isValidated = false;
            validateBtn.setSclass("btn bo");

            String status = result.getStatusLabel();
            valMessageLabel.setValue(
                "⚠ Account is " + status
                + ". This cheque must be rejected.");
            valMessageLabel.setSclass("val-warn");

            valDetailsDiv.setVisible(true);
            valHolderLabel.setValue(safe(result.getAccountHolderName()));
            valTypeLabel.setValue("—");
            valStatusBadge.setValue(result.getAccountStatus());
            valStatusBadge.setSclass("badge b-fail");
            valIfscLabel.setValue("—");
            valBalanceLabel.setValue("—");

            // Auto-open reject panel with appropriate reason pre-selected
            openRejectPanelForInactiveAccount(result.getAccountStatus());

            System.out.println("AccountEntryComposer → Account is "
                    + status + " — reject required");

        } else {
            // ══ Case 3: NOT FOUND or CBS error ══
            isValidated = false;
            validateBtn.setSclass("btn bo");

            String msg = result.getErrorMessage() != null
                ? result.getErrorMessage()
                : "Account not found in CBS.";
            valMessageLabel.setValue("✗ " + msg);
            valMessageLabel.setSclass("val-err");

            valDetailsDiv.setVisible(false);

            if (!result.isFound()) {
                // Account not found — open reject panel
                openRejectPanelForNotFoundAccount();
            }

            System.out.println("AccountEntryComposer → Validation FAILED: "
                    + msg);
        }
    }

    /**
     * Opens reject panel and pre-selects the reason code for
     * INACTIVE / CLOSED / FROZEN accounts.
     */
    private void openRejectPanelForInactiveAccount(String accountStatus) {
        rejectPanel.setVisible(true);
        rejectRemarksBox.setValue(
            "CBS validation failed — Account status: " + accountStatus);

        // Pre-select appropriate reason code
        if ("FROZEN".equalsIgnoreCase(accountStatus)) {
            selectRejectReason("14"); // Account Blocked or Frozen
        } else if ("CLOSED".equalsIgnoreCase(accountStatus)) {
            selectRejectReason("13"); // Account Closed or Transferred
        } else {
            selectRejectReason("14"); // Inactive = Blocked
        }

        Clients.showNotification(
            "Account is " + accountStatus
            + ". Please reject this cheque using the panel below.",
            "warning", null, "top_center", 4000);
    }

    /**
     * Opens reject panel when account is not found in CBS.
     */
    private void openRejectPanelForNotFoundAccount() {
        rejectPanel.setVisible(true);
        rejectRemarksBox.setValue("CBS validation failed — Account not found in CBS records.");
        selectRejectReason("15"); // Not Drawn on Us
        Clients.showNotification(
            "Account not found in CBS. Please reject this cheque.",
            "warning", null, "top_center", 4000);
    }

    /**
     * Pre-selects a rejection reason in the dropdown by value code.
     */
    private void selectRejectReason(String reasonCode) {
        for (int i = 0; i < rejectReasonBox.getItemCount(); i++) {
            Object val = rejectReasonBox.getItemAtIndex(i).getValue();
            if (reasonCode.equals(val != null ? val.toString() : "")) {
                rejectReasonBox.setSelectedIndex(i);
                return;
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  Amount → Auto-fill Words
    // ════════════════════════════════════════════════════

    @Listen("onChange = #amountBox")
    public void onAmountChange() {
        BigDecimal amt = amountBox.getValue();
        if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
            amountInWordsBox.setValue(
                AmountToWords.convert(amt.doubleValue()));
        }
    }

    // ════════════════════════════════════════════════════
    //  Navigation (Prev / Next)
    // ════════════════════════════════════════════════════

    @Listen("onClick = #prevBtn")
    public void onPrev() {
        if (pendingList == null || pendingList.isEmpty()) return;
        currentIndex = (currentIndex - 1 + pendingList.size())
                        % pendingList.size();
        loadChequeForm(pendingList.get(currentIndex));
    }

    @Listen("onClick = #nextBtn")
    public void onNext() {
        if (pendingList == null || pendingList.isEmpty()) return;
        currentIndex = (currentIndex + 1) % pendingList.size();
        loadChequeForm(pendingList.get(currentIndex));
    }

    // ════════════════════════════════════════════════════
    //  Save & Next
    // ════════════════════════════════════════════════════

    @Listen("onClick = #saveNextBtn")
    public void onSaveNext() {
        OutwardCheque cheque = pendingList.get(currentIndex);

        String     accNo   = accountNoBox.getValue();
        BigDecimal amount  = amountBox.getValue();
        String     dateStr = chequeDateBox.getValue();
        String     words   = amountInWordsBox.getValue();
        String     payee   = payeeNameBox.getValue();

        // ── Validation 1: CBS must be validated first ──
        if (!isValidated) {
            Clients.showNotification(
                "⚠ Account must be validated via CBS before saving. "
                + "Click the 'Validate' button first.",
                "warning", null, "top_center", 3500);
            return;
        }

        // ── Validation 2: CBS result must be ACTIVE ──
        // This is a safety check in case state gets out of sync
        if (cbsResult != null && !cbsResult.isActive()) {
            Clients.showNotification(
                "Account is " + cbsResult.getStatusLabel()
                + ". This cheque cannot be saved — it must be rejected.",
                "error", null, "top_center", 4000);
            rejectPanel.setVisible(true);
            return;
        }

        // ── Validation 3: Required entry fields ──
        if (isBlank(accNo)) {
            Clients.showNotification(
                "Account number is required.",
                "warning", null, "top_center", 2000);
            return;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            Clients.showNotification(
                "Amount is required and must be greater than zero.",
                "warning", null, "top_center", 2000);
            return;
        }
        if (isBlank(dateStr)) {
            Clients.showNotification(
                "Cheque date is required.",
                "warning", null, "top_center", 2000);
            return;
        }
        if (isBlank(words)) {
            Clients.showNotification(
                "Amount in words is required.",
                "warning", null, "top_center", 2000);
            return;
        }
        if (isBlank(payee)) {
            Clients.showNotification(
                "Payee name is required.",
                "warning", null, "top_center", 2000);
            return;
        }

        if (isBlank(words) && amount != null) {
            words = AmountToWords.convert(amount.doubleValue());
        }

        // ── Get CBS account holder name to store ──
        String accountHolder = (cbsResult != null
                && cbsResult.getAccountHolderName() != null
                && !cbsResult.getAccountHolderName().isEmpty())
            ? cbsResult.getAccountHolderName()
            : "Validated";

        // ── Save to DB ──
        boolean ok = entryService.saveEntry(
            cheque.getId(),
            accNo.trim(),
            accountHolder,      // ← real CBS account holder name
            amount,
            words.trim(),
            dateStr.trim(),
            payee.trim(),
            currentMakerId);

        if (!ok) {
            Clients.showNotification(
                "Failed to save entry. Please try again.",
                "error", null, "top_center", 2500);
            return;
        }

        Clients.showNotification(
            "✓ Cheque " + cheque.getChequeNo() + " entry saved.",
            "info", null, "top_center", 2000);

        pendingList.remove(currentIndex);

        if (entryService.isAllEntriesDone(currentBatch.getId())) {
            // Auto-submit only for the NORMAL flow (ENTRY_PENDING batches).
            // For REFER_BACK batches, the maker manually re-submits from
            // View Batches so they explicitly confirm the fix.
            if (!"REFER_BACK".equals(currentBatch.getStatus())) {
                entryService.submitBatch(currentBatch.getId());
                Clients.showNotification(
                    "All entries done! Batch " + batchId
                    + " submitted to Checker queue.",
                    "info", null, "top_center", 4000);
            } else {
                Clients.showNotification(
                    "All referred data-entry cheques fixed. Go to View Batches "
                  + "to re-submit this batch to the Checker.",
                    "info", null, "top_center", 4000);
                System.out.println("AccountEntryComposer → REFER_BACK batch "
                    + currentBatch.getBatchId()
                    + " — all data-entry referrals fixed, awaiting maker re-submit.");
            }
            showBatchSubmittedState();
            return;
        }

        if (pendingList.isEmpty()) {
            showBatchSubmittedState();
        } else {
            if (currentIndex >= pendingList.size()) {
                currentIndex = pendingList.size() - 1;
            }
            refreshStatsBar();
            loadChequeForm(pendingList.get(currentIndex));
        }
    }

    private void showBatchSubmittedState() {
        refreshStatsBar();
        Executions.sendRedirect("/outward/batchUpload/batchUpload.zul");
    }

    // ════════════════════════════════════════════════════
    //  Reject Panel
    // ════════════════════════════════════════════════════

    @Listen("onClick = #rejectTriggerBtn")
    public void onRejectTrigger() {
        rejectPanel.setVisible(true);
        if (rejectReasonBox.getItemCount() > 0) {
            rejectReasonBox.setSelectedIndex(0);
        }
        rejectRemarksBox.setValue("");
    }

    @Listen("onClick = #cancelRejectBtn")
    public void onCancelReject() {
        rejectPanel.setVisible(false);
    }

    @Listen("onClick = #confirmRejectBtn")
    public void onConfirmReject() {
        if (rejectReasonBox.getSelectedItem() == null
                || isBlank(rejectReasonBox.getSelectedItem()
                                          .getValue().toString())) {
            Clients.showNotification(
                "Please select a rejection reason.",
                "warning", null, "top_center", 2500);
            return;
        }

        OutwardCheque cheque  = pendingList.get(currentIndex);
        String        reason  = rejectReasonBox.getSelectedItem()
                                               .getValue().toString();
        String        remarks = rejectRemarksBox.getValue();

        boolean ok = entryService.rejectCheque(
            cheque.getId(), reason, remarks, currentMakerId);

        if (!ok) {
            Clients.showNotification(
                "Rejection failed. Please try again.",
                "error", null, "top_center", 2500);
            return;
        }

        Clients.showNotification(
            "Cheque " + cheque.getChequeNo() + " rejected.",
            "info", null, "top_center", 2000);

        pendingList.remove(currentIndex);

        if (entryService.isAllEntriesDone(currentBatch.getId())) {
            // Auto-submit only for the NORMAL flow.
            // For REFER_BACK batches, the maker manually re-submits later.
            if (!"REFER_BACK".equals(currentBatch.getStatus())) {
                entryService.submitBatch(currentBatch.getId());
            } else {
                System.out.println("AccountEntryComposer → REFER_BACK batch "
                    + currentBatch.getBatchId()
                    + " — all entries resolved, awaiting maker re-submit.");
            }
            showBatchSubmittedState();
            return;
        }

        if (pendingList.isEmpty()) {
            showBatchSubmittedState();
        } else {
            if (currentIndex >= pendingList.size()) {
                currentIndex = pendingList.size() - 1;
            }
            refreshStatsBar();
            loadChequeForm(pendingList.get(currentIndex));
        }
    }

    // ════════════════════════════════════════════════════
    //  Navigation — Back
    // ════════════════════════════════════════════════════

    @Listen("onClick = #backBtn")
    public void onBack() {
        if (cameFromSidebar) {
            loadBatchSelectView();
        } else {
            Executions.sendRedirect("/outward/batchUpload/batchUpload.zul");
        }
    }

    // ════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════

    private String  safe(String s)    { return s != null ? s.trim() : ""; }
    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}