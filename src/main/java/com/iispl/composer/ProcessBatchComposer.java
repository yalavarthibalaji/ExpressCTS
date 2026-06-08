package com.iispl.composer;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;

import com.iispl.dto.CbsValidationResult;
import com.iispl.dto.LoginDTO;
import com.iispl.entity.User;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.CheckerBatchProcessService;
import com.iispl.service.CbsFirebaseService;
import com.iispl.serviceImpl.CheckerBatchProcessServiceImpl;
import com.iispl.util.InwardReturnReason;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/ProcessBatchComposer.java
 *
 * CBS FIREBASE VALIDATION — where it happens:
 *   renderCheque(index)
 *     → renderTechnicalPanel(cheque)
 *         → fillCbsFromFirebase(cheque)            ← background thread
 *             → CbsFirebaseService.validate(acctNo)
 *                 → Firebase: cbs_accounts/<acct_no>
 *                     → updates: lblAcctHolder, lblAcctBalance,
 *                                lblCbsAcctValid, lblCbsBankMatch
 *
 * Flow:
 *   1. Page loads — shows "Checking CBS..." immediately
 *   2. Background thread queries Firebase (max 5 sec timeout)
 *   3. ZK Desktop activate/deactivate pushes result back to UI
 *   4. Labels update with real CBS data
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
    @Wire private Label  lblProgressCounter;

    // ── Progress Bar ──────────────────────────────────────────────────────────
    @Wire private Label  lblProgressText;
    @Wire private Div    divProgressFill;

    // ── LEFT — Image viewer tabs + panels ────────────────────────────────────
    @Wire private Div    pvImgSection;
    @Wire private Div    pvFrontPanel;
    @Wire private Div    pvBackPanel;
    @Wire private Div    pvGreyscalePanel;
    @Wire private Image  pvFrontImg;
    @Wire private Image  pvBackImg;
    @Wire private Image  pvGreyscaleImg;
    @Wire private Button    pvFrontTab;       // tab div (not a button)
    @Wire private Button    pvBackTab;        // tab div (not a button)
    @Wire private Button    pvGreyscaleTab;   // tab div (not a button)
    @Wire private Label  pvNoImgMsg;
    

    // ── LEFT — Cheque card labels ─────────────────────────────────────────────
    @Wire private Label  lblChequeBankName;
    @Wire private Label  lblChequePresenting;
    @Wire private Label  lblChequeDate;
    @Wire private Label  lblChequePayee;
    @Wire private Label  lblChequeAmountWords;
    @Wire private Label  lblChequeAmountBox;
    @Wire private Label  lblMicrLine;

    // ── RIGHT — Status badge ──────────────────────────────────────────────────
    @Wire private Label  lblTechStatus;

    // ── RIGHT — CBS Validation ────────────────────────────────────────────────
    @Wire private Label  lblCbsMicrCode;
    @Wire private Label  lblCbsBankCode;
    @Wire private Label  lblCbsOurBankCode;

    // ── RIGHT — Presenting Details ────────────────────────────────────────────
    @Wire private Label  lblPresBank;
    @Wire private Label  lblPresChequeNo;
    @Wire private Label  lblPresAmount;

    // ── RIGHT — Our Account (CBS — loaded from Firebase) ─────────────────────
    @Wire private Label  lblAcctNo;
    @Wire private Label  lblAcctHolder;    // ← updated from Firebase
    @Wire private Label  lblAcctBalance;   // ← updated from Firebase
    @Wire private Label  lblCbsAcctValid;  // ← badge: Valid / Not Found
    @Wire private Label  lblCbsBankMatch;  // ← badge: Matched / Mismatch

    // ── RIGHT — Action ────────────────────────────────────────────────────────
    @Wire private Button   btnAccept;
    @Wire private Button   btnReturn;
    @Wire private Button   btnSendBack;
    @Wire private Div      divReturnReasonBox;
    @Wire private Combobox comboReturnReason;

    // ── Footer ────────────────────────────────────────────────────────────────
    @Wire private Label  lblFooterProgress;
    @Wire private Button btnSubmit;

    // ── State ─────────────────────────────────────────────────────────────────
    private InwardBatch        currentBatch;
    private List<InwardCheque> cheques;
    private int                currentIndex = 0;
    private Map<Long, String>  actionMap    = new HashMap<>();
    private Map<Long, String>  reasonMap    = new HashMap<>();
    private String             activeTab    = "FRONT"; // FRONT | BACK | GREY

    // ── Services ──────────────────────────────────────────────────────────────
    private final CheckerBatchProcessService batchService =
            new CheckerBatchProcessServiceImpl();

    // Firebase CBS service — initialised once, reused for every cheque
    private final CbsFirebaseService cbsService = new CbsFirebaseService();
    
    // ══════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        getSelf().getDesktop().enableServerPush(true);

        // ── Wire tab buttons manually (most reliable in nested ZUL) ──
        if (pvFrontTab != null)
            pvFrontTab.addEventListener("onClick", e -> showTab("FRONT"));
        if (pvBackTab != null)
            pvBackTab.addEventListener("onClick", e -> showTab("BACK"));
        if (pvGreyscaleTab != null)
            pvGreyscaleTab.addEventListener("onClick", e -> showTab("GREY"));

        String batchId = (String) Sessions.getCurrent()
                .getAttribute("selectedBatchId");

        if (batchId == null || batchId.trim().isEmpty()) {
            Messagebox.show(
                "No batch selected. Please go back and select a batch.",
                "Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        try {
            currentBatch = batchService.loadBatchForProcessing(batchId.trim());
        } catch (Exception e) {
            Messagebox.show("Failed to load batch: " + e.getMessage(),
                "Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        cheques = currentBatch.getCheques();

        if (cheques == null || cheques.isEmpty()) {
            Messagebox.show("This batch has no cheques.",
                "Info", Messagebox.OK, Messagebox.INFORMATION);
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
        set(lblTotalAmount,  "₹ " + fmtAmt(currentBatch.getTotalAmount()));
        set(lblMicrErrors,   String.valueOf(currentBatch.getMicrErrorCount()));
        set(lblSourceFile,   nvl(currentBatch.getSourceFileName()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Render one cheque
    // ══════════════════════════════════════════════════════════════════════════

    private void renderCheque(int index) {
        if (cheques == null || index < 0 || index >= cheques.size()) return;
        currentIndex = index;

        InwardCheque c = cheques.get(index);
        int total  = cheques.size();
        int record = index + 1;

        set(lblRecordNav,       "Record " + record + " of " + total);
        set(lblProgressCounter, record + "/" + total);

        if (btnPrev != null) btnPrev.setDisabled(index == 0);
        if (btnNext != null) btnNext.setDisabled(index == total - 1);

        loadImages(c);
        renderChequeCard(c);
        renderTechnicalPanel(c);
        restoreActionState(c.getId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LEFT — Image Loading
    //  Reads front_image_path / back_image_path from inward_cheque (DB).
    //  Paths stored by BpxfParser at upload time.
    //  Served by InwardImageServlet at /imageServlet?path=<encoded>
    // ══════════════════════════════════════════════════════════════════════════

    private void loadImages(InwardCheque c) {
        // Reset tab state for each new cheque
        activeTab = "FRONT";

        String front = c.getFrontImagePath();
        String back  = c.getBackImagePath();

        boolean hasFront = front != null && !front.trim().isEmpty();
        boolean hasBack  = back  != null && !back.trim().isEmpty();

        if (!hasFront && !hasBack) {
            if (pvImgSection != null) pvImgSection.setVisible(false);
            if (pvNoImgMsg   != null) pvNoImgMsg.setVisible(true);
            return;
        }

        if (pvImgSection != null) pvImgSection.setVisible(true);
        if (pvNoImgMsg   != null) pvNoImgMsg.setVisible(false);

        // Load front + back into their panels; greyscale reuses front image src
        String frontSrc = hasFront ? imgUrl(front.trim()) : "";
        String backSrc  = hasBack  ? imgUrl(back.trim())  : "";

        if (pvFrontImg     != null) pvFrontImg.setSrc(frontSrc);
        if (pvBackImg      != null) pvBackImg.setSrc(backSrc);
        // Greyscale tab shows the front image with a CSS grayscale filter
        if (pvGreyscaleImg != null) pvGreyscaleImg.setSrc(frontSrc);

        showTab("FRONT");
        
    }

    private String imgUrl(String path) {
        try { return "/imageServlet?path=" + URLEncoder.encode(path, "UTF-8"); }
        catch (UnsupportedEncodingException e) { return "/imageServlet?path=" + path; }
    }


    /**
     * Central tab-switching method.
     * Shows the requested image panel and highlights the matching tab div.
     * Greyscale tab shows the front image with a CSS filter — no extra DB field needed.
     */
    /**
     * Switches the visible image panel when user clicks Front / Back / Greyscale tab.
     *
     * TWO-STEP approach (both steps are required):
     *
     * Step 1 — setVisible(true/false)
     *   ZK renders this as inline style="display:none" on the <div>.
     *   This is the RELIABLE way to show/hide panels in ZK.
     *   CSS alone (sclass toggle) was causing all three panels to show
     *   simultaneously because ZK's default <div> has no inline style,
     *   so a :not([style]) CSS selector matched all panels at once.
     *
     * Step 2 — setSclass() for the active border highlight
     *   pb-panel-active adds a coloured bottom border to the visible panel.
     *   This is purely decorative — visibility is controlled by setVisible().
     */
    private void showTab(String tab) {
        activeTab = tab;

        // ── Step 1: setVisible — primary visibility control ──────────────────
        if (pvFrontPanel != null)     pvFrontPanel.setVisible("FRONT".equals(tab));
        if (pvBackPanel != null)      pvBackPanel.setVisible("BACK".equals(tab));
        if (pvGreyscalePanel != null) pvGreyscalePanel.setVisible("GREY".equals(tab));

        // ── Step 2: setSclass — active border highlight on panel ─────────────
        if (pvFrontPanel != null)
            pvFrontPanel.setSclass("FRONT".equals(tab) ? "pb-img-panel pb-panel-active" : "pb-img-panel");
        if (pvBackPanel != null)
            pvBackPanel.setSclass("BACK".equals(tab) ? "pb-img-panel pb-panel-active" : "pb-img-panel");
        if (pvGreyscalePanel != null)
            pvGreyscalePanel.setSclass("GREY".equals(tab) ? "pb-img-panel pb-panel-active" : "pb-img-panel");

        // ── Step 3: highlight active tab button ──────────────────────────────
        if (pvFrontTab != null)
            pvFrontTab.setSclass("FRONT".equals(tab) ? "tab active" : "tab");
        if (pvBackTab != null)
            pvBackTab.setSclass("BACK".equals(tab) ? "tab active" : "tab");
        if (pvGreyscaleTab != null)
            pvGreyscaleTab.setSclass("GREY".equals(tab) ? "tab active" : "tab");

        // ── Step 4: sync greyscale image src with front ──────────────────────
        if ("GREY".equals(tab) && pvFrontImg != null && pvGreyscaleImg != null)
            pvGreyscaleImg.setSrc(pvFrontImg.getSrc());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LEFT — Cheque Card
    // ══════════════════════════════════════════════════════════════════════════

    private void renderChequeCard(InwardCheque c) {
        set(lblChequeBankName,   "CSB Bank Limited");
        set(lblChequePresenting, "Presenting: " + nvl(c.getPresentingBankName()));
        set(lblChequeDate,       c.getChequeDate() != null
                                 ? fmtDate(c.getChequeDate().toString()) : "—");

        String payee = c.getPayeeName() != null ? c.getPayeeName()
                     : nvl(c.getDraweeAccountHolder());
        set(lblChequePayee,       "Pay: " + payee);
        set(lblChequeAmountWords, nvl(c.getAmountInWords()));
        set(lblChequeAmountBox,   "₹ " + fmtAmt(c.getAmount()));
        set(lblMicrLine,          buildMicrLine(c));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RIGHT — Technical Panel
    // ══════════════════════════════════════════════════════════════════════════

    private void renderTechnicalPanel(InwardCheque c) {
        set(lblTechStatus, "PENDING");
        if (lblTechStatus != null) lblTechStatus.setSclass("pv-badge-pending");

        // CBS Validation block — from DB
        String micr = c.getMicrCodeCorrected() != null
                    ? c.getMicrCodeCorrected() : nvl(c.getMicrCodeRaw());
        set(lblCbsMicrCode,    micr);
        set(lblCbsBankCode,    nvl(c.getBankCode()));
        set(lblCbsOurBankCode, "700  (CSB Bank)");

        // Presenting Details — from DB
        set(lblPresBank,     nvl(c.getPresentingBankName()));
        set(lblPresChequeNo, nvl(c.getChequeNo()));
        set(lblPresAmount,   "₹ " + fmtAmt(c.getAmount()));

        // Our Account — account number from DB immediately
        set(lblAcctNo, nvl(c.getDraweeAccountNumber()));

        // ── CBS FIREBASE VALIDATION ────────────────────────────────────────
        // Show "Checking CBS..." while Firebase responds in background.
        // This is the ONLY place Firebase is called in this composer.
        // ──────────────────────────────────────────────────────────────────
        set(lblAcctHolder,  "Checking CBS...");
        set(lblAcctBalance, "—");

        if (lblCbsAcctValid != null) {
            lblCbsAcctValid.setValue("—");
            lblCbsAcctValid.setSclass("badge b-grey");
        }
        if (lblCbsBankMatch != null) {
            lblCbsBankMatch.setValue("—");
            lblCbsBankMatch.setSclass("badge b-grey");
        }

        // Fire background thread so UI doesn't freeze during Firebase call
        fillCbsFromFirebase(c);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CBS FIREBASE VALIDATION — background thread
    //
    //  WHAT HAPPENS HERE:
    //  1. Reads cheque.draweeAccountNumber from DB (already loaded)
    //  2. Calls CbsFirebaseService.validate(accountNumber)
    //     → queries Firebase Realtime DB: cbs_accounts/<account_number>
    //     → waits max 5 seconds
    //  3. Compares cheque.bankCode with "700" (our bank) → bankMatched
    //  4. Uses ZK Executions.activate/deactivate to push result back to UI
    //     (ZK requires this for any UI update from a non-ZK thread)
    //
    //  WHAT FIREBASE RETURNS (from cbs_accounts node):
    //  {
    //    account_holder: "Rajesh Kumar",
    //    bank_code: "700",
    //    balance: 125000.00,
    //    is_active: true
    //  }
    //
    //  VALIDATION RESULT:
    //  - Account Valid  = Firebase found the account AND is_active = true
    //  - Bank Matched   = cheque.bankCode equals "700" (CSB Bank code)
    //  - Not Found      = account number not in Firebase at all
    // ══════════════════════════════════════════════════════════════════════════

    private void fillCbsFromFirebase(InwardCheque cheque) {

        String accountNumber = cheque.getDraweeAccountNumber();
        String bankCode      = cheque.getBankCode();
        Desktop desktop      = getSelf().getDesktop();

        if (desktop == null) {
            System.err.println("fillCbsFromFirebase → desktop is null, skipping.");
            return;
        }

        new Thread(() -> {

            // ── Step 1: Query Firebase ──────────────────────────────────────
            CbsValidationResult cbs     = cbsService.validate(accountNumber, bankCode);
            boolean             bankMatched = cbsService.isBankMatched(bankCode);

            // ── Step 2: Check desktop still alive ───────────────────────────
            if (!desktop.isAlive()) {
                System.err.println("fillCbsFromFirebase → desktop no longer alive.");
                return;
            }

            // ── Step 3: Push result back to ZK UI ───────────────────────────
            try {
                Executions.activate(desktop);
                try {
                    if (cbs.isFound()) {
                        set(lblAcctHolder,  cbs.getAccountHolder());
                        set(lblAcctBalance, "₹ " + fmtBigDecimal(cbs.getBalance()));

                        boolean active = cbs.isActive();
                        if (lblCbsAcctValid != null) {
                            lblCbsAcctValid.setValue(active ? "Valid" : "Inactive");
                            lblCbsAcctValid.setSclass(active ? "badge b-pass" : "badge b-fail");
                        }
                    } else {
                        set(lblAcctHolder,  "Not found in CBS");
                        set(lblAcctBalance, "—");
                        if (lblCbsAcctValid != null) {
                            lblCbsAcctValid.setValue("Not Found");
                            lblCbsAcctValid.setSclass("badge b-fail");
                        }
                    }

                    if (lblCbsBankMatch != null) {
                        lblCbsBankMatch.setValue(bankMatched ? "Matched" : "Mismatch");
                        lblCbsBankMatch.setSclass(bankMatched ? "badge b-pass" : "badge b-fail");
                    }

                } finally {
                    Executions.deactivate(desktop);
                }

            } catch (Exception e) {
                System.err.println("fillCbsFromFirebase → UI update failed: "
                    + e.getClass().getSimpleName() + " — " + e.getMessage());
            }

        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Navigation
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
    //  Action Buttons
    // ══════════════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnAccept")
    public void onAccept()   { applyAction("ACCEPTED");  }

    @Listen("onClick = #btnReturn")
    public void onReturn()   { applyAction("RETURNED");  }

    @Listen("onClick = #btnSendBack")
    public void onSendBack() { applyAction("SEND_BACK"); }

    private void applyAction(String action) {
        if (cheques == null || currentIndex >= cheques.size()) return;
        Long id = cheques.get(currentIndex).getId();
        actionMap.put(id, action);

        if (!"RETURNED".equals(action)) {
            reasonMap.remove(id);
            if (comboReturnReason != null) {
                comboReturnReason.setValue("");
                comboReturnReason.setSelectedItem(null);
            }
        }

        highlightActionButtons(action);
        toggleReturnReasonBox(id);
        refreshProgress();

        if (!"RETURNED".equals(action) && currentIndex < cheques.size() - 1)
            renderCheque(currentIndex + 1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Return Reason Dropdown
    // ══════════════════════════════════════════════════════════════════════════

    private void buildReturnReasonDropdown() {
        if (comboReturnReason == null) return;
        comboReturnReason.getItems().clear();

        Map<String, String> reasons = InwardReturnReason.getReasonDropdownMap();
        for (Map.Entry<String, String> entry : reasons.entrySet()) {
            Comboitem item = comboReturnReason.appendItem(entry.getValue());
            item.setValue(entry.getKey());
        }

        comboReturnReason.addEventListener("onSelect", ev -> {
            Comboitem sel = comboReturnReason.getSelectedItem();
            if (sel != null && cheques != null && currentIndex < cheques.size())
                reasonMap.put(cheques.get(currentIndex).getId(), (String) sel.getValue());
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Progress
    // ══════════════════════════════════════════════════════════════════════════

    private void refreshProgress() {
        if (cheques == null) return;
        int total    = cheques.size();
        int actioned = actionMap.size();
        int pct      = total > 0 ? actioned * 100 / total : 0;

        String text = actioned + " of " + total + " cheques actioned";
        set(lblProgressText,   text);
        set(lblFooterProgress, text);

        if (divProgressFill != null)
            divProgressFill.setStyle("width:" + pct + "%");
        if (btnSubmit != null)
            btnSubmit.setDisabled(actioned < total);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Submit
    // ══════════════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnSubmit")
    public void onSubmitBatch() {
        LoginDTO user = (LoginDTO) Sessions.getCurrent()
                .getAttribute(SessionUtil.SESSION_KEY);

        if (user == null) {
            Messagebox.show("Session expired. Please log in again.",
                "Session Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        User checker = new User();
        checker.setId(user.getUserId());

        List<InwardCheckerAction> actions = new ArrayList<>();
        for (InwardCheque c : cheques) {
            InwardCheckerAction ca = new InwardCheckerAction();
            ca.setInwardCheque(c);
            ca.setAction(actionMap.get(c.getId()));
            if ("RETURNED".equals(ca.getAction()))
                ca.setReasonCode(reasonMap.get(c.getId()));
            actions.add(ca);
        }

        try {
            batchService.submitBatch(currentBatch, actions, checker);
            Messagebox.show(
                "Batch " + currentBatch.getBatchId() + " submitted successfully.",
                "Success", Messagebox.OK, Messagebox.INFORMATION,
                ev -> navigateBack()
            );
        } catch (IllegalArgumentException e) {
            Messagebox.show(e.getMessage(),
                "Validation Error", Messagebox.OK, Messagebox.EXCLAMATION);
        } catch (Exception e) {
            Messagebox.show("Submit failed: " + e.getMessage(),
                "Error", Messagebox.OK, Messagebox.ERROR);
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Navigation back
    // ══════════════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnBackToList")
    public void onBackToList() { navigateBack(); }

    @Listen("onClick = #btnCancel")
    public void onCancel()     { navigateBack(); }

    private void navigateBack() {
        Executions.getCurrent().sendRedirect(
            "/inward/inwardChecker/inwardCheckerVerification.zul");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private void restoreActionState(Long id) {
        String action = actionMap.get(id);
        highlightActionButtons(action);
        toggleReturnReasonBox(id);
        if ("RETURNED".equals(action) && comboReturnReason != null) {
            String code = reasonMap.get(id);
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

    private void highlightActionButtons(String action) {
        if (btnAccept   != null) btnAccept.setSclass(
            "ACCEPTED".equals(action)  ? "btn bs btn-on"   : "btn bs");
        if (btnReturn   != null) btnReturn.setSclass(
            "RETURNED".equals(action)  ? "btn bd btn-on"   : "btn bd");
        if (btnSendBack != null) btnSendBack.setSclass(
            "SEND_BACK".equals(action) ? "btn bo btn-on" : "btn bo");
    }

    private void toggleReturnReasonBox(Long id) {
        if (divReturnReasonBox != null)
            divReturnReasonBox.setVisible("RETURNED".equals(actionMap.get(id)));
    }

    private String buildMicrLine(InwardCheque c) {
        String no   = nvl(c.getChequeNo());
        String micr = c.getMicrCodeCorrected() != null
                    ? c.getMicrCodeCorrected() : nvl(c.getMicrCodeRaw());
        return "«" + no + "«   " + micr + "«   —" + c.getSeqNo() + "—";
    }

    private String fmtDate(String iso) {
        if (iso == null || iso.length() < 10) return iso != null ? iso : "—";
        try {
            String[] p = iso.split("-");
            if (p.length == 3) return p[2] + "/" + p[1] + "/" + p[0];
        } catch (Exception ignored) {}
        return iso;
    }

    private String fmtAmt(BigDecimal bd) {
        if (bd == null) return "0.00";
        return fmtBigDecimal(bd);
    }

    private String fmtBigDecimal(BigDecimal bd) {
        try {
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(bd);
        } catch (Exception e) { return bd.toPlainString(); }
    }

    private String nvl(String s) {
        return (s != null && !s.trim().isEmpty()) ? s : "—";
    }

    private void set(Label lbl, String val) {
        if (lbl != null) lbl.setValue(val != null ? val : "—");
    }
}