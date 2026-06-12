package com.iispl.composer.outward;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import com.iispl.dto.CbsValidationResult;
import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.service.AccountEntryService;
import com.iispl.service.CbsService;
import com.iispl.service.MakerOutwardService;
import com.iispl.serviceImpl.AccountEntryServiceImpl;
import com.iispl.serviceImpl.CbsServiceImpl;
import com.iispl.serviceImpl.MakerOutwardServiceImpl;
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


    // ── Three views ──
    @Wire private Div emptyStateView;
    @Wire private Div batchSelectView;
    @Wire private Div entryView;

    // ── Batch Select View ──
    @Wire private Rows batchSelectRows;
    
    
    
 // ── Pagination ──
    @Wire private Div    batchPager;
    @Wire private Button btnPrevPage;
    @Wire private Button btnNextPage;
    @Wire private Label  batchPagerInfo;

    // ── Entry View — Stats bar ──
    @Wire private Label statsBatchId;
    @Wire private Label statsTotal;
    @Wire private Label statsDoneCount;
    @Wire private Label statsRemCount;

    // ── Entry View — Navigation ──
    @Wire private Label navLabel;

    // ── Left Panel — Cheque Images (MICR-style tabs) ──
    @Wire private Button tabFrontBtn;
    @Wire private Button tabBackBtn;
    @Wire private Div    chqFront;
    @Wire private Div    chqBack;
    @Wire private Image  frontImage;
    @Wire private Image  backImage;

    // ── Right Panel — Cheque Info (read-only) ──
    @Wire private Label chqNoDisplay;
    @Wire private Label chqDateDisplay;
    @Wire private Label entryStatusBadge;

    // ── Right Panel — Account & CBS Validation ──
    @Wire private Textbox accountNoBox;
    @Wire private Button  validateBtn;
    @Wire private Button saveNextBtn;
    @Wire private Div     valResultDiv;
    @Wire private Label   valMessageLabel;  // success / warning / error message
    @Wire private Div     valDetailsDiv;    // account details panel
    @Wire private Label   valHolderLabel;   // account holder name
    @Wire private Label   valTypeLabel;     // SAVINGS / CURRENT
    @Wire private Label   valStatusBadge;   // ACTIVE / INACTIVE badge
    @Wire private Label   valIfscLabel;     // IFSC code
    @Wire private Label   valBalanceLabel;  // balance

    // ── CBS cache hint ──
    @Wire private Div   cbsCacheDiv;
    @Wire private Label cbsCacheLabel;

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
    private String              currentMakerName;
    private final MakerOutwardService makerOutwardService = new MakerOutwardServiceImpl();
    private boolean             cameFromSidebar = false;
    private int                 totalCheques    = 0;
    
    
 // ── Pagination State ──
    private static final int    PAGE_SIZE        = 1;
    private int                 batchPage        = 0;
    private List<OutwardBatch>  batchDisplayList = new ArrayList<>();

    // ── CBS Validation State (reset per cheque) ──
    private boolean             isValidated   = false;
    private CbsValidationResult cbsResult     = null;

    // ── Unsaved-changes flag ──
    private boolean isDirty = false;

    // ── CBS session cache (account number → result) ──
    private final Map<String, CbsValidationResult> cbsCache = new HashMap<>();

    // ── XML-parsed amount used for the mismatch warning ──
    private BigDecimal originalXmlAmount = null;

    // ── Guard that prevents the amountInWordsBox auto-fill from firing
    //    its own onChange handler during programmatic prefill ──
    private boolean autoFillingWords = false;

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

        currentMakerId = dto.getUserId();
        currentMakerName = dto.getFullName();

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
        // Hide pager when leaving batch select
        if (!"batchSelect".equals(view)) {
            batchPager.setVisible(false);
        }
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

        batchDisplayList = batches;
        batchPage = 0;
        showView("batchSelect");
        renderBatchPage();
    }

    private void renderBatchPage() {
        batchSelectRows.getChildren().clear();

        int totalItems = batchDisplayList.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);

        if (batchPage >= totalPages) batchPage = totalPages - 1;
        if (batchPage < 0)           batchPage = 0;

        int fromIndex = batchPage * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, totalItems);

        List<OutwardBatch> pageData = batchDisplayList.subList(fromIndex, toIndex);

        // Fetch pending counts for ALL visible batches in ONE query
        // (avoids one DB round-trip per batch row).
        List<Long> ids = new ArrayList<>();
        for (OutwardBatch b : pageData) ids.add(b.getId());
        Map<Long, Integer> pendingMap = entryService.getPendingCountsForBatches(ids);

        int idx = fromIndex + 1;
        for (OutwardBatch b : pageData) {
            int pending = pendingMap.getOrDefault(b.getId(), 0);
            batchSelectRows.appendChild(buildBatchSelectRow(idx++, b, pending));
        }

        batchPager.setVisible(totalPages > 1);
        batchPagerInfo.setValue("Page " + (batchPage + 1) + " of " + totalPages
                + "  (" + totalItems + " batches)");
        btnPrevPage.setDisabled(batchPage == 0);
        btnNextPage.setDisabled(batchPage >= totalPages - 1);
    }                                           // ← renderBatchPage() closes HERE

    @Listen("onClick = #btnPrevPage")
    public void onPrevPage() {
        if (batchPage > 0) {
            batchPage--;
            renderBatchPage();
        }
    }                                           // ← onPrevPage() closes HERE

    @Listen("onClick = #btnNextPage")
    public void onNextPage() {
        if (batchDisplayList != null) {
            int totalPages = (int) Math.ceil(
                    (double) batchDisplayList.size() / PAGE_SIZE);
            if (batchPage < totalPages - 1) {
                batchPage++;
                renderBatchPage();
            }
        }
    }                                           // ← onNextPage() closes HERE}

    private Row buildBatchSelectRow(int idx, final OutwardBatch b, int pending) {
        Row row = new Row();
        row.appendChild(new Label(String.valueOf(idx)));

        Label batchIdLbl = new Label(safe(b.getBatchId()));
        batchIdLbl.setSclass("mono");
        row.appendChild(batchIdLbl);

        row.appendChild(new Label(String.valueOf(b.getChequeCount())));
        row.appendChild(new Label(
            b.getActualAmount() != null
            ? "₹" + moneyFmt.format(b.getActualAmount()) : "—"));

        // Pending count passed in by caller (single bulk query for all batches)
        row.appendChild(new Label(String.valueOf(pending)));
        row.appendChild(new Label(String.valueOf(b.getChequeCount() - pending)));

        // Status badge reflects the actual batch status —
        // "Referred Back" for REFER_BACK, "Entry Done" otherwise.
        String statusText;
        String badgeSclass;
        if ("REFER_BACK".equals(b.getStatus())) {
            statusText  = "Referred Back";
            badgeSclass = "badge b-warn";
        } else {
            statusText  = "Entry Done";
            badgeSclass = "badge b-info";
        }
        Label statusBadge = new Label(statusText);
        statusBadge.setSclass(badgeSclass);
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

        // Reset per-batch session state
        cbsCache.clear();          // fresh CBS cache for new batch
        isDirty = false;           // no unsaved changes yet

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

        // Last cheque — no "next" to go to, show "Save" only
        if (pendingList.size() == 1) {
            saveNextBtn.setLabel("Save");
        } else {
            saveNextBtn.setLabel("Save and Next");
        }

        entryStatusBadge.setValue("Pending");
        entryStatusBadge.setSclass("badge b-pend");

        // Cheque info
        chqNoDisplay.setValue(safe(cheque.getChequeNo()));
        chqDateDisplay.setValue(
            cheque.getChequeDate() != null
            ? cheque.getChequeDate().toString() : "—");

        // Account section
        accountNoBox.setValue(safe(cheque.getAccountNo()));

        // ── Always reset image panel to FRONT tab on new cheque ──
        chqFront.setVisible(true);
        chqBack.setVisible(false);
        tabFrontBtn.setSclass("chq-tab chq-tab-active");
        tabBackBtn.setSclass("chq-tab");

        // ── RESET CBS validation state for every new cheque ──
        resetCbsValidation();

        // Capture XML-parsed amount for the mismatch warning.
        // Only for first-time entry (PENDING); skip for re-entry of
        // CHECKER_REFERRED cheques.
        if ("PENDING".equals(cheque.getStatus())) {
            originalXmlAmount = cheque.getAmount();
        } else {
            originalXmlAmount = null;
        }

        // Amount pre-fill — guard onChange handler from firing for our setValue
        autoFillingWords = true;
        if (cheque.getAmount() != null) {
            amountBox.setValue(cheque.getAmount());
            amountInWordsBox.setValue(
                AmountToWords.convert(cheque.getAmount().doubleValue()));
        } else {
            amountBox.setValue((BigDecimal) null);
            amountInWordsBox.setValue("");
        }
        autoFillingWords = false;

        chequeDateBox.setValue(
            cheque.getChequeDate() != null
            ? cheque.getChequeDate().toString() : "");

        payeeNameBox.setValue(safe(cheque.getPayeeName()));

        loadImages(cheque);
        rejectPanel.setVisible(false);

        // ── Reset auxiliary divs on every cheque load ──
        cbsCacheDiv.setVisible(false);

        // Form is fresh — no unsaved changes yet
        isDirty = false;
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
        cbsCacheDiv.setVisible(false);
        isDirty = true;
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

        String key = accNo.trim();

        // Check the CBS session cache first — saves a network call
        if (cbsCache.containsKey(key)) {
            CbsValidationResult cached = cbsCache.get(key);
            cbsResult = cached;
            displayCbsResult(cached);
            // Show small hint that result came from cache
            cbsCacheDiv.setVisible(true);
            cbsCacheLabel.setValue("⚡ CBS result loaded from session cache "
                                   + "(no network call made).");
            System.out.println("AccountEntryComposer → CBS cache hit: " + key);
            return;
        }

        // ── Show loading state ──
        validateBtn.setLabel("Validating...");
        validateBtn.setDisabled(true);
        valResultDiv.setVisible(false);
        valDetailsDiv.setVisible(false);
        cbsCacheDiv.setVisible(false);

        System.out.println("AccountEntryComposer → CBS validate (live): " + key);

        // ── Call CBS (Firebase) ──
        CbsValidationResult result = cbsService.validateAccount(key);
        cbsResult = result;

        // Store result in session cache so the next click on the same
        // account number returns instantly
        cbsCache.put(key, result);

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
        // Skip if this onChange was triggered by our own setValue() during
        // loadChequeForm() — otherwise we would overwrite a freshly loaded
        // amount-in-words value with the computed one before the user has
        // even seen it.
        if (autoFillingWords) return;

        BigDecimal amt = amountBox.getValue();
        autoFillingWords = true;
        if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
            amountInWordsBox.setValue(
                AmountToWords.convert(amt.doubleValue()));
        } else {
            amountInWordsBox.setValue("");
        }
        autoFillingWords = false;

        // Form has been modified by the user
        isDirty = true;
    }

    // ════════════════════════════════════════════════════
    //  IMAGE TAB SWITCHING (MICR Repair style)
    // ════════════════════════════════════════════════════

    @Listen("onClick = #tabFrontBtn")
    public void onTabFront() {
        chqFront.setVisible(true);
        chqBack.setVisible(false);
        tabFrontBtn.setSclass("chq-tab chq-tab-active");
        tabBackBtn.setSclass("chq-tab");
    }

    @Listen("onClick = #tabBackBtn")
    public void onTabBack() {
        chqFront.setVisible(false);
        chqBack.setVisible(true);
        tabFrontBtn.setSclass("chq-tab");
        tabBackBtn.setSclass("chq-tab chq-tab-active");
    }

    // ════════════════════════════════════════════════════
    //  Dirty-flag tracking for other form fields
    // ════════════════════════════════════════════════════

    @Listen("onChanging = #chequeDateBox; onChanging = #payeeNameBox")
    public void onFormFieldChanging() {
        isDirty = true;
    }

    // ════════════════════════════════════════════════════
    //  Navigation (Prev / Next)
    // ════════════════════════════════════════════════════

    @Listen("onClick = #prevBtn")
    public void onPrev() {
        if (pendingList == null || pendingList.isEmpty()) return;
        navigateTo((currentIndex - 1 + pendingList.size()) % pendingList.size());
    }

    @Listen("onClick = #nextBtn")
    public void onNext() {
        if (pendingList == null || pendingList.isEmpty()) return;
        navigateTo((currentIndex + 1) % pendingList.size());
    }

    /**
     * Shared navigation helper that prompts about unsaved changes before
     * moving away from the current cheque. Used by the Prev / Next buttons.
     */
    private void navigateTo(final int newIndex) {
        if (newIndex == currentIndex) return;

        if (isDirty) {
            Messagebox.show(
                "You have unsaved changes on this cheque.\n\n"
                + "Navigate away anyway? Your edits will be lost.",
                "Unsaved Changes",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        isDirty      = false;
                        currentIndex = newIndex;
                        loadChequeForm(pendingList.get(currentIndex));
                    }
                }
            );
        } else {
            currentIndex = newIndex;
            loadChequeForm(pendingList.get(currentIndex));
        }
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

        // ── Amount mismatch check ──
        // If the maker-entered amount differs from the XML-parsed amount
        // by more than 1%, show a YES/NO confirmation before saving.
        // Catches data-entry errors like missed/extra zeros.
        if (originalXmlAmount != null
                && originalXmlAmount.compareTo(BigDecimal.ZERO) > 0
                && isMismatch(amount, originalXmlAmount)) {
            final String finalWords  = words;
            final String finalAccNo  = accNo;
            final String finalDate   = dateStr;
            final String finalPayee  = payee;
            final BigDecimal finalAmount = amount;

            String msg = String.format(
                "Entered amount ₹%s differs from the XML-parsed amount ₹%s "
                + "by more than 1%%.%n%nThis could be a data entry error.%n%n"
                + "Save the entered value anyway?",
                amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                originalXmlAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()
            );
            Messagebox.show(msg, "Amount Mismatch Warning",
                Messagebox.YES | Messagebox.NO, Messagebox.EXCLAMATION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        doSaveCheque(cheque, finalAccNo, finalAmount,
                                     finalWords, finalDate, finalPayee);
                    }
                });
            return;
        }

        doSaveCheque(cheque, accNo, amount, words, dateStr, payee);
    }

    /**
     * Performs the actual cheque save after all validations + amount-mismatch
     * confirmation are passed. Updates the thumbnail strip and progress bar,
     * then either advances to the next cheque or finalises the batch.
     */
    private void doSaveCheque(OutwardCheque cheque,
                              String accNo, BigDecimal amount,
                              String words, String dateStr, String payee) {

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
            accountHolder,
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

        // Clear unsaved-changes flag — save completed successfully
        isDirty = false;

        pendingList.remove(currentIndex);

        if (entryService.isAllEntriesDone(currentBatch.getId())) {
            // Auto-submit only for the NORMAL flow (ENTRY_PENDING batches).
            // For REFER_BACK batches, the maker manually re-submits from
            // View Batches so they explicitly confirm the fix.
            if (!"REFER_BACK".equals(currentBatch.getStatus())) {
                entryService.submitBatch(currentBatch.getId(), currentMakerId);
                Clients.showNotification(
                    "All entries done! Batch " + batchId
                    + " submitted to Checker queue.",
                    "info", null, "top_center", 4000);
            } else {
                // REFER_BACK: check ALL modules, not just Data Entry
                int remaining = makerOutwardService.countActiveReferrals(currentBatch.getId());
                if (remaining == 0) {
                    showResubmitPopup();
                    return;  // BUG: was missing — popup must complete before redirect
                } else {
                    // MICR module still has referred cheques pending
                    Clients.showNotification(
                        remaining + " referred cheque(s) still pending in MICR Repair module.",
                        "info", null, "top_center", 3000);
                    showBatchSubmittedState();
                }
                return;
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

        // Clear unsaved-changes flag — save completed successfully
        isDirty = false;

        pendingList.remove(currentIndex);

        if (entryService.isAllEntriesDone(currentBatch.getId())) {
            // Auto-submit only for the NORMAL flow.
            // For REFER_BACK batches, the maker manually re-submits later.
            if (!"REFER_BACK".equals(currentBatch.getStatus())) {
                entryService.submitBatch(currentBatch.getId(), currentMakerId);
            } else {
                int remAfterReject = makerOutwardService.countActiveReferrals(currentBatch.getId());
                System.out.println("AccountEntryComposer → REFER_BACK batch "
                    + currentBatch.getBatchId()
                    + " — referred cheque rejected, remaining=" + remAfterReject);
                if (remAfterReject == 0) {
                    showResubmitPopup();
                    return;  // popup handles redirect on YES
                }
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
        // Prompt about unsaved changes before leaving the screen
        if (isDirty) {
            Messagebox.show(
                "You have unsaved changes on this cheque.\n\n"
                + "Leave anyway? Your edits will be lost.",
                "Unsaved Changes",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        isDirty = false;
                        doBack();
                    }
                }
            );
        } else {
            doBack();
        }
    }

    /**
     * cameFromSidebar drives the back navigation:
     *   true  → user arrived from the sidebar link → return to the in-page
     *           batch selection view.
     *   false → user arrived via the batchId URL parameter (i.e. from the
     *           MICR Repair screen's "Proceed to Account Entry" button) →
     *           go back to MICR Repair.
     */
    private void doBack() {
        if (cameFromSidebar) {
            loadBatchSelectView();
        } else {
            Executions.sendRedirect("/outward/micrRepair/micrRepair.zul");
        }
    }

    // ════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════

    private String  safe(String s)    { return s != null ? s.trim() : ""; }
    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
 // ════════════════════════════════════════════════════════════════
    //  Resubmit popup — shown when all referred cheques are fixed
    // ════════════════════════════════════════════════════════════════

    /**
     * Shows a YES/NO popup asking the maker to resubmit the batch to Checker.
     * Fires only when countActiveReferrals() == 0 for a REFER_BACK batch.
     * If both MICR and Data Entry modules had referred cheques, this popup
     * only appears when the LAST cheque across BOTH modules is completed.
     */
    private void showResubmitPopup() {
        String batchId = currentBatch != null ? currentBatch.getBatchId() : "";
        Messagebox.show(
            "All referred cheques in batch " + batchId + " have been fixed.\n\n"
            + "Resubmit to Checker now?",
            "Resubmit to Checker",
            Messagebox.YES | Messagebox.NO,
            Messagebox.QUESTION,
            event -> {
                if (Messagebox.ON_YES.equals(event.getName())) {
                    doResubmit();
                }
            }
        );
    }

    private void doResubmit() {
        if (currentBatch == null) return;
        boolean ok = makerOutwardService.resubmitBatch(
                currentBatch.getId(), currentMakerId, currentMakerName);
        if (ok) {
            Clients.showNotification(
                "Batch " + currentBatch.getBatchId() + " resubmitted to Checker.",
                "info", null, "top_center", 3000);
        } else {
            Clients.showNotification(
                "Resubmit failed. Please try from View Batches.",
                "error", null, "top_center", 3000);
        }
        // Navigate away AFTER popup interaction — not before
        showBatchSubmittedState();
    }

    // ════════════════════════════════════════════════════════════════
    //  Amount mismatch tolerance check
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns true if the maker-entered amount differs from the XML-parsed
     * amount by more than 1%.
     *
     * Why 1%? Banks tolerate small rounding when re-keying handwritten
     * cheques but anything beyond that probably indicates a missed/extra
     * digit (e.g. ₹5,000 typed as ₹50,000 or vice-versa).
     *
     * BigDecimal.compareTo is used (not equals) so 1000 vs 1000.00 compare equal.
     */
    private boolean isMismatch(BigDecimal entered, BigDecimal original) {
        if (entered == null || original == null) return false;
        if (original.compareTo(BigDecimal.ZERO) <= 0) return false;
        BigDecimal diff      = entered.subtract(original).abs();
        BigDecimal tolerance = original.multiply(new BigDecimal("0.01"));
        return diff.compareTo(tolerance) > 0;
    }
}