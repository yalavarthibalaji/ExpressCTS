package com.iispl.composer.outward;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.service.MicrRepairService;
import com.iispl.serviceImpl.MicrRepairServiceImpl;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/outward/MicrRepairComposer.java
 * Purpose : MICR Repair screen for Maker Outward.
 *
 * Save / Prev / Next Button Logic:
 *   - When a cheque loads with yellow (wrong) fields:
 *       saveNextBtn → DISABLED  (user must fix all yellow fields first)
 *       prevBtn     → DISABLED  (user cannot skip without fixing)
 *       nextBtn     → DISABLED  (user cannot skip without fixing)
 *
 *   - As user types in any MICR textbox, the button state is re-evaluated.
 *
 *   - saveNextBtn is ENABLED only when:
 *       (1) All 6 fields have the correct exact length.
 *       (2) Every yellow field has a value DIFFERENT from its original wrong value.
 *
 *   - prevBtn and nextBtn are ENABLED only when there are NO yellow fields
 *     on the current cheque (i.e. the cheque has no pending MICR errors
 *     that still need the maker's attention).
 *     This prevents the maker from navigating away without fixing errors.
 *
 * onChanging fires on every keystroke — uses InputEvent.getValue()
 *   for the field being typed, and .getValue() for all other fields.
 * onChange fires on focus-out — all fields use .getValue().
 */
public class MicrRepairComposer extends SelectorComposer<Component> {

    private final MicrRepairService micrService = new MicrRepairServiceImpl();
    private final DecimalFormat     moneyFmt    = new DecimalFormat("#,##0.00");

    // ── Topbar ──
    @Wire private Label  userAvatar;
    @Wire private Label  userName;
    @Wire private Label  userRole;

    // ── Four views ──
    @Wire private Div emptyStateView;
    @Wire private Div batchSelectView;
    @Wire private Div listView;
    @Wire private Div repairView;

    // ── Batch Select View ──
    @Wire private Rows batchSelectRows;

    // ── List View ──
    @Wire private Label  listBatchBadge;
    @Wire private Label  listPendingBadge;
    @Wire private Rows   micrRows;
    @Wire private Button proceedBtn;
    @Wire private Label  proceedHint;

    // ── Repair View toolbar ──
    @Wire private Label  repairBatchBadge;
    @Wire private Label  repairIssueBadge;
    @Wire private Label  recLabel;

    // ── Left Panel ──
    @Wire private Div    chqFront;
    @Wire private Div    chqBack;
    @Wire private Button tabFrontBtn;
    @Wire private Button tabBackBtn;
    @Wire private Image  frontImage;
    @Wire private Image  backImage;
    @Wire private Label  termChqNo;
    @Wire private Label  termMicrCode;

    // ── Navigation buttons — wired to control disabled state ──
    @Wire private Button prevBtn;
    @Wire private Button nextBtn;

    // ── Right Panel — MICR Entry Fields ──
    @Wire private Textbox chqNoBox;
    @Wire private Textbox cityCodeBox;
    @Wire private Textbox bankCodeBox;
    @Wire private Textbox branchCodeBox;
    @Wire private Textbox baseNumberBox;
    @Wire private Textbox transCodeBox;
    @Wire private Textbox repairRemarksBox;

    // ── Save & Next Button (wired to control enabled/disabled state) ──
    @Wire private Button saveNextBtn;

    // ── Reject Panel ──
    @Wire private Div     rejectPanel;
    @Wire private Listbox rejectReasonBox;
    @Wire private Textbox rejectRemarksBox;

    // ── State ──
    private List<OutwardCheque> repairList;
    private int                 currentIndex    = 0;
    private OutwardBatch        currentBatch;
    private String              batchId;
    private Long                currentMakerId;
    private boolean             cameFromSidebar = false;

    // Tracks whether the current cheque has any yellow (wrong) fields.
    // Set to true in loadRepairView() when yellow fields are found.
    // Used to decide whether Prev/Next should be blocked.
    private boolean currentChequeHasErrors = false;

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
            currentBatch    = micrService.getBatch(batchId.trim());

            if (currentBatch == null) {
                Clients.showNotification(
                    "Batch not found: " + batchId,
                    "error", null, "top_center", 3000);
                Executions.sendRedirect(
                    "/outward/batchUpload/batchUpload.zul");
                return;
            }
            showView("list");
            loadAndShowList();

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
        listView.setVisible("list".equals(view));
        repairView.setVisible("repair".equals(view));
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

    @Listen("onClick = #goToBatchUploadBtn")
    public void onGoToBatchUpload() {
        Executions.sendRedirect("/outward/batchUpload/batchUpload.zul");
    }

    // ════════════════════════════════════════════════════
    //  Batch Select View
    // ════════════════════════════════════════════════════

    private void loadBatchSelectView() {
        List<OutwardBatch> batches =
            micrService.getBatchesNeedingRepair(currentMakerId);

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
        row.appendChild(new Label(b.getActualAmount() != null
            ? "₹" + moneyFmt.format(b.getActualAmount()) : "—"));
        row.appendChild(new Label(String.valueOf(b.getChequeCount())));
        row.appendChild(new Label("0"));

        Label statusBadge = new Label("Needs Repair");
        statusBadge.setSclass("badge b-pend");
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
        showView("list");
        loadAndShowList();
    }

    // ════════════════════════════════════════════════════
    //  List View
    // ════════════════════════════════════════════════════

    private void loadAndShowList() {
        repairList = micrService.getMicrErrorCheques(currentBatch.getId());

        listBatchBadge.setValue(safe(batchId));
        listPendingBadge.setValue(repairList.size() + " Pending");

        micrRows.getChildren().clear();
        int idx = 1;
        for (OutwardCheque cheque : repairList) {
            micrRows.appendChild(buildListRow(idx++, cheque));
        }
        checkProceedButton();
    }

    private Row buildListRow(int idx, final OutwardCheque cheque) {
        Row row = new Row();
        row.appendChild(new Label(String.valueOf(idx)));

        Label chqNoLbl = new Label(safe(cheque.getChequeNo()));
        chqNoLbl.setSclass("mono");
        row.appendChild(chqNoLbl);

        row.appendChild(new Label(safe(cheque.getBankCode())));

        Label micrLbl = new Label(safe(cheque.getMicrCode()));
        micrLbl.setSclass("mono");
        row.appendChild(micrLbl);

        row.appendChild(new Label(cheque.getAmount() != null
            ? "₹" + moneyFmt.format(cheque.getAmount()) : "—"));

        Label issueBadge = new Label("MICR Error");
        issueBadge.setSclass("badge b-pend");
        row.appendChild(issueBadge);

        Button repairBtn = new Button("Repair");
        repairBtn.setSclass("btn bp btn-sm");
        repairBtn.addEventListener(Events.ON_CLICK,
            new EventListener<Event>() {
                @Override public void onEvent(Event e) {
                    openRepairView(cheque);
                }
            });
        row.appendChild(repairBtn);
        return row;
    }

    private void checkProceedButton() {
        boolean allDone = micrService.isAllRepaired(currentBatch.getId());
        proceedBtn.setDisabled(!allDone);
        proceedBtn.setSclass(allDone
            ? "btn bp btn-lg"
            : "btn bp btn-lg btn-disabled");
        proceedHint.setVisible(!allDone);
    }

    // ════════════════════════════════════════════════════
    //  Open Repair View
    // ════════════════════════════════════════════════════

    private void openRepairView(OutwardCheque cheque) {
        for (int i = 0; i < repairList.size(); i++) {
            if (repairList.get(i).getId().equals(cheque.getId())) {
                currentIndex = i;
                break;
            }
        }
        showView("repair");
        loadRepairView(repairList.get(currentIndex));
    }

    // ════════════════════════════════════════════════════
    //  Load Repair View
    // ════════════════════════════════════════════════════

    private void loadRepairView(OutwardCheque cheque) {

        repairBatchBadge.setValue(safe(batchId));
        repairIssueBadge.setValue("MICR ERROR");
        recLabel.setValue((currentIndex + 1) + " of " + repairList.size());

        loadChequeImages(cheque);

        String micr = safe(cheque.getMicrCode());
        termChqNo.setValue(safe(cheque.getChequeNo()));
        termMicrCode.setValue(micr.length() > 6 ? micr.substring(6) : micr);

        chqFront.setVisible(true);
        chqBack.setVisible(false);
        tabFrontBtn.setSclass("chq-tab active");
        tabBackBtn.setSclass("chq-tab");

        // Fill form fields from stored cheque data
        chqNoBox.setValue(safe(cheque.getChequeNo()));
        cityCodeBox.setValue(safe(cheque.getCityCode()));
        bankCodeBox.setValue(safe(cheque.getBankCode()));
        branchCodeBox.setValue(safe(cheque.getBranchCode()));
        baseNumberBox.setValue(safe(cheque.getBaseNumber()));
        transCodeBox.setValue(safe(cheque.getTransactionCode()));
        repairRemarksBox.setValue("");

        // Colour the fields yellow/blue and record whether errors exist
        currentChequeHasErrors = detectAndHighlightErrors(cheque);

        rejectPanel.setVisible(false);

        // Apply initial button state with the just-loaded values.
        // Yellow fields still contain the original WRONG values →
        // saveNextBtn starts DISABLED.
        // prevBtn and nextBtn also start DISABLED when errors exist.
        applySaveButtonState(
            safe(cheque.getChequeNo()),
            safe(cheque.getCityCode()),
            safe(cheque.getBankCode()),
            safe(cheque.getBranchCode()),
            safe(cheque.getBaseNumber()),
            safe(cheque.getTransactionCode()));
    }

    private void loadChequeImages(OutwardCheque cheque) {
        String fp = cheque.getFrontImagePath();
        String bp = cheque.getBackImagePath();
        try {
            frontImage.setSrc(fp != null && !fp.trim().isEmpty()
                ? "/imageServlet?path="
                  + URLEncoder.encode(fp.trim(), "UTF-8") : "");
            backImage.setSrc(bp != null && !bp.trim().isEmpty()
                ? "/imageServlet?path="
                  + URLEncoder.encode(bp.trim(), "UTF-8") : "");
        } catch (UnsupportedEncodingException e) {
            frontImage.setSrc("");
            backImage.setSrc("");
        }
    }

    // ════════════════════════════════════════════════════
    //  MICR Field Highlighting
    //  Returns true if ANY field is yellow (has errors).
    //  Return value stored in currentChequeHasErrors.
    // ════════════════════════════════════════════════════

    /**
     * Colours each MICR textbox yellow or blue based on whether the
     * stored value matches the expected value from the raw MICR code.
     *
     * Returns true if at least one field is yellow (has an error).
     * This return value is used to decide whether Prev/Next should be locked.
     */
    private boolean detectAndHighlightErrors(OutwardCheque cheque) {
        String micr = cheque.getMicrCode();

        // If MICR code is missing or too short — all fields are suspect
        if (micr == null || micr.trim().length() < 23) {
            setYellow(cityCodeBox);
            setYellow(bankCodeBox);
            setYellow(branchCodeBox);
            setYellow(baseNumberBox);
            setYellow(transCodeBox);
            return true; // errors exist
        }

        boolean hasError = false;
        hasError |= highlightField(cityCodeBox,   cheque.getCityCode(),        micr.substring(6,  9));
        hasError |= highlightField(bankCodeBox,   cheque.getBankCode(),        micr.substring(9,  12));
        hasError |= highlightField(branchCodeBox, cheque.getBranchCode(),      micr.substring(12, 15));
        hasError |= highlightField(baseNumberBox, cheque.getBaseNumber(),      micr.substring(15, 21));
        hasError |= highlightField(transCodeBox,  cheque.getTransactionCode(), micr.substring(21, 23));

        return hasError;
    }

    /**
     * Highlights a single field yellow (wrong) or blue (correct).
     * Returns true if the field is yellow (mismatched).
     */
    private boolean highlightField(Textbox box, String stored, String expected) {
        boolean isWrong = !safe(stored).equals(safe(expected));
        if (isWrong) setYellow(box);
        else         setBlue(box);
        return isWrong;
    }

    private void setYellow(Textbox box) { box.setSclass("fi mono field-changed"); }
    private void setBlue(Textbox box)   { box.setSclass("fi mono field-ocr");     }

    // ════════════════════════════════════════════════════
    //  Tab Switching
    // ════════════════════════════════════════════════════

    @Listen("onClick = #tabFrontBtn")
    public void onShowFront() {
        chqFront.setVisible(true);  chqBack.setVisible(false);
        tabFrontBtn.setSclass("chq-tab active"); tabBackBtn.setSclass("chq-tab");
    }

    @Listen("onClick = #tabBackBtn")
    public void onShowBack() {
        chqFront.setVisible(false); chqBack.setVisible(true);
        tabFrontBtn.setSclass("chq-tab"); tabBackBtn.setSclass("chq-tab active");
    }

    // ════════════════════════════════════════════════════
    //  Prev / Next Navigation
    //  These buttons are disabled when currentChequeHasErrors = true
    //  AND the maker has not yet made any valid changes.
    //  The @Listen methods still exist so ZK can wire them — but the
    //  buttons will be physically disabled in the UI when locked.
    // ════════════════════════════════════════════════════

    @Listen("onClick = #prevBtn")
    public void onPrev() {
        if (repairList == null || repairList.isEmpty()) return;
        currentIndex = (currentIndex - 1 + repairList.size()) % repairList.size();
        loadRepairView(repairList.get(currentIndex));
    }

    @Listen("onClick = #nextBtn")
    public void onNext() {
        if (repairList == null || repairList.isEmpty()) return;
        currentIndex = (currentIndex + 1) % repairList.size();
        loadRepairView(repairList.get(currentIndex));
    }

    // ════════════════════════════════════════════════════
    //  Real-time Field Listeners — one per field
    //
    //  onChanging fires on EVERY keystroke.
    //  InputEvent.getValue() = the value INCLUDING the latest
    //  typed character (the textbox.getValue() itself is one
    //  keystroke behind during onChanging).
    //  So: pass e.getValue() for the active field,
    //       .getValue() for all others.
    //
    //  onChange fires when focus leaves the field.
    //  All .getValue() are up-to-date at that point.
    // ════════════════════════════════════════════════════

    // ── chqNoBox ──
    @Listen("onChanging = #chqNoBox")
    public void onChqNoChanging(InputEvent e) {
        applySaveButtonState(
            e.getValue(),
            cityCodeBox.getValue(), bankCodeBox.getValue(),
            branchCodeBox.getValue(), baseNumberBox.getValue(),
            transCodeBox.getValue());
    }

    // ── cityCodeBox ──
    @Listen("onChanging = #cityCodeBox")
    public void onCityChanging(InputEvent e) {
        applySaveButtonState(
            chqNoBox.getValue(),
            e.getValue(), bankCodeBox.getValue(),
            branchCodeBox.getValue(), baseNumberBox.getValue(),
            transCodeBox.getValue());
    }

    // ── bankCodeBox ──
    @Listen("onChanging = #bankCodeBox")
    public void onBankChanging(InputEvent e) {
        applySaveButtonState(
            chqNoBox.getValue(),
            cityCodeBox.getValue(), e.getValue(),
            branchCodeBox.getValue(), baseNumberBox.getValue(),
            transCodeBox.getValue());
    }

    // ── branchCodeBox ──
    @Listen("onChanging = #branchCodeBox")
    public void onBranchChanging(InputEvent e) {
        applySaveButtonState(
            chqNoBox.getValue(),
            cityCodeBox.getValue(), bankCodeBox.getValue(),
            e.getValue(), baseNumberBox.getValue(),
            transCodeBox.getValue());
    }

    // ── baseNumberBox ──
    @Listen("onChanging = #baseNumberBox")
    public void onBaseChanging(InputEvent e) {
        applySaveButtonState(
            chqNoBox.getValue(),
            cityCodeBox.getValue(), bankCodeBox.getValue(),
            branchCodeBox.getValue(), e.getValue(),
            transCodeBox.getValue());
    }

    // ── transCodeBox ──
    @Listen("onChanging = #transCodeBox")
    public void onTcChanging(InputEvent e) {
        applySaveButtonState(
            chqNoBox.getValue(),
            cityCodeBox.getValue(), bankCodeBox.getValue(),
            branchCodeBox.getValue(), baseNumberBox.getValue(),
            e.getValue());
    }

    // onChange — fires on focus-out: all .getValue() are current ──
    @Listen("onChange = #chqNoBox; onChange = #cityCodeBox; "
          + "onChange = #bankCodeBox; onChange = #branchCodeBox; "
          + "onChange = #baseNumberBox; onChange = #transCodeBox")
    public void onAnyFieldChange() {
        applySaveButtonState(
            chqNoBox.getValue(), cityCodeBox.getValue(),
            bankCodeBox.getValue(), branchCodeBox.getValue(),
            baseNumberBox.getValue(), transCodeBox.getValue());
    }

    // ════════════════════════════════════════════════════
    //  Save Button State Engine
    //  Called on every keystroke and focus-out.
    //  Controls saveNextBtn, prevBtn, and nextBtn together.
    //
    //  saveNextBtn — enabled only when all fields are valid length
    //                AND all yellow fields have been changed.
    //
    //  prevBtn / nextBtn — enabled only when:
    //    - the current cheque has NO yellow fields (no errors), OR
    //    - the maker has already typed something different in at least
    //      one yellow field (they are actively working on it and can
    //      save first — but navigation is still blocked until they save).
    //
    //  Simpler rule for Prev/Next:
    //    Disabled when currentChequeHasErrors = true AND canSave = false.
    //    Once the maker fixes enough to enable Save, they must save first,
    //    then navigate. Navigation is never allowed mid-fix.
    // ════════════════════════════════════════════════════

    /**
     * Evaluates whether the Save / Prev / Next buttons should be
     * enabled or disabled. Updates button styles accordingly.
     *
     * @param chqNo   current value in chqNoBox     (may be from InputEvent)
     * @param city    current value in cityCodeBox
     * @param bank    current value in bankCodeBox
     * @param branch  current value in branchCodeBox
     * @param base    current value in baseNumberBox
     * @param tc      current value in transCodeBox
     */
    private void applySaveButtonState(String chqNo, String city, String bank,
                                       String branch, String base, String tc) {
        boolean canSave = canEnableSave(chqNo, city, bank, branch, base, tc);

        // ── Save & Next button ──
        saveNextBtn.setDisabled(!canSave);
        saveNextBtn.setSclass(canSave
            ? "btn bp btn-lg w100"
            : "btn bp btn-lg w100 btn-disabled");

        // ── Prev and Next navigation buttons ──
        // Locked when the cheque has MICR errors and the maker hasn't
        // completed fixing them yet (canSave is still false).
        // If the cheque has no errors at all (currentChequeHasErrors = false),
        // navigation is always allowed.
        boolean navAllowed = !currentChequeHasErrors;
        prevBtn.setDisabled(!navAllowed);
        nextBtn.setDisabled(!navAllowed);

        prevBtn.setSclass(navAllowed ? "btn bo btn-sm" : "btn bo btn-sm btn-disabled");
        nextBtn.setSclass(navAllowed ? "btn bo btn-sm" : "btn bo btn-sm btn-disabled");
    }

    /**
     * Returns true only when both conditions are satisfied:
     *
     * Condition 1 — Correct length:
     *   chequeNo = 6, cityCode = 3, bankCode = 3,
     *   branchCode = 3, baseNumber = 6, transCode = 2
     *
     * Condition 2 — Yellow fields corrected:
     *   For every field that was yellow (stored value ≠ expected from micrCode),
     *   the current typed value must differ from the original wrong stored value.
     *   Blue fields (stored == expected) are not checked.
     */
    private boolean canEnableSave(String chqNo, String city, String bank,
                                   String branch, String base, String tc) {

        if (repairList == null || currentIndex >= repairList.size()) {
            return false;
        }

        // ── Condition 1: All fields have correct exact length ──
        if (len(chqNo)  != 6) return false;
        if (len(city)   != 3) return false;
        if (len(bank)   != 3) return false;
        if (len(branch) != 3) return false;
        if (len(base)   != 6) return false;
        if (len(tc)     != 2) return false;

        // ── Condition 2: Yellow fields must have been changed ──
        OutwardCheque cheque = repairList.get(currentIndex);
        String micrCode = cheque.getMicrCode();

        if (micrCode == null || micrCode.trim().length() < 23) {
            // micrCode not available — can't do mismatch check.
            // Allow save if all lengths are correct (Condition 1 already passed).
            return true;
        }

        String expCity   = micrCode.substring(6,  9);
        String expBank   = micrCode.substring(9,  12);
        String expBranch = micrCode.substring(12, 15);
        String expBase   = micrCode.substring(15, 21);
        String expTc     = micrCode.substring(21, 23);

        // isYellowAndUnchanged: field WAS yellow AND still has original wrong value
        if (isYellowAndUnchanged(cheque.getCityCode(),        expCity,   city))   return false;
        if (isYellowAndUnchanged(cheque.getBankCode(),        expBank,   bank))   return false;
        if (isYellowAndUnchanged(cheque.getBranchCode(),      expBranch, branch)) return false;
        if (isYellowAndUnchanged(cheque.getBaseNumber(),      expBase,   base))   return false;
        if (isYellowAndUnchanged(cheque.getTransactionCode(), expTc,     tc))     return false;

        // Both conditions passed
        return true;
    }

    /**
     * Returns true if a field is yellow (was wrong) AND still has its
     * original wrong value (user has not edited it yet).
     *
     * @param storedWrong original wrong value stored in DB
     * @param expected    correct value from micrCode decomposition
     * @param currentVal  value currently showing in the textbox
     */
    private boolean isYellowAndUnchanged(String storedWrong,
                                          String expected,
                                          String currentVal) {
        boolean wasYellow = !safe(storedWrong).equals(safe(expected));
        boolean unchanged = safe(currentVal).equals(safe(storedWrong));
        return wasYellow && unchanged;
    }

    // ════════════════════════════════════════════════════
    //  Save and Next
    // ════════════════════════════════════════════════════

    @Listen("onClick = #saveNextBtn")
    public void onSaveNext() {
        OutwardCheque cheque  = repairList.get(currentIndex);

        String chequeNo = chqNoBox.getValue();
        String city     = cityCodeBox.getValue();
        String bank     = bankCodeBox.getValue();
        String branch   = branchCodeBox.getValue();
        String base     = baseNumberBox.getValue();
        String tc       = transCodeBox.getValue();
        String remarks  = repairRemarksBox.getValue();

        // Safety check (button should already be disabled if these fail)
        if (!canEnableSave(chequeNo, city, bank, branch, base, tc)) {
            Clients.showNotification(
                "Please correct all highlighted fields to the exact required length.",
                "warning", null, "top_center", 3000);
            return;
        }

        boolean ok = micrService.saveRepair(
            cheque.getId(),
            chequeNo.trim(),
            city.trim(), bank.trim(), branch.trim(),
            base.trim(), tc.trim(),
            remarks, currentMakerId);

        if (!ok) {
            Clients.showNotification(
                "Failed to save repair. Please try again.",
                "error", null, "top_center", 2500);
            return;
        }

        Clients.showNotification(
            "Cheque " + chequeNo.trim() + " repaired.",
            "info", null, "top_center", 2000);

        repairList.remove(currentIndex);

        if (micrService.isAllRepaired(currentBatch.getId())) {
            if (!"REFER_BACK".equals(currentBatch.getStatus())) {
                micrService.markBatchEntryDone(currentBatch.getId());
                Clients.showNotification(
                    "All MICR repairs complete. Batch ready for Account Entry.",
                    "info", null, "top_center", 3000);
            } else {
                Clients.showNotification(
                    "All referred MICR cheques fixed. Go to View Batches "
                  + "to re-submit this batch to the Checker.",
                    "info", null, "top_center", 4000);
                System.out.println("MicrRepairComposer → REFER_BACK batch "
                    + currentBatch.getBatchId()
                    + " — all MICR referrals fixed, awaiting maker re-submit.");
            }
            goBackToList();
            return;
        }

        if (repairList.isEmpty()) {
            goBackToList();
        } else {
            if (currentIndex >= repairList.size()) {
                currentIndex = repairList.size() - 1;
            }
            loadRepairView(repairList.get(currentIndex));
        }
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

        OutwardCheque cheque  = repairList.get(currentIndex);
        String        reason  = rejectReasonBox.getSelectedItem()
                                               .getValue().toString();
        String        remarks = rejectRemarksBox.getValue();

        boolean ok = micrService.rejectCheque(
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

        repairList.remove(currentIndex);

        if (micrService.isAllRepaired(currentBatch.getId())) {
            if (!"REFER_BACK".equals(currentBatch.getStatus())) {
                micrService.markBatchEntryDone(currentBatch.getId());
                Clients.showNotification(
                    "All repairs complete. Batch ready for Account Entry.",
                    "info", null, "top_center", 3000);
            } else {
                Clients.showNotification(
                    "All referred MICR cheques resolved. Go to View Batches "
                  + "to re-submit this batch to the Checker.",
                    "info", null, "top_center", 4000);
                System.out.println("MicrRepairComposer → REFER_BACK batch "
                    + currentBatch.getBatchId()
                    + " — all MICR referrals resolved, awaiting maker re-submit.");
            }
        }

        goBackToList();
    }

    // ════════════════════════════════════════════════════
    //  Navigation
    // ════════════════════════════════════════════════════

    @Listen("onClick = #backToListBtn")
    public void goBackToList() {
        showView("list");
        loadAndShowList();
    }

    @Listen("onClick = #backToBatchesBtn")
    public void onBackToBatches() {
        if (cameFromSidebar) {
            loadBatchSelectView();
        } else {
            Executions.sendRedirect("/outward/batchUpload/batchUpload.zul");
        }
    }

    @Listen("onClick = #proceedBtn")
    public void onProceed() {
        Executions.sendRedirect(
            "/outward/acctAmount/acctAmount.zul?batchId=" + batchId);
    }

    // ════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════

    private String  safe(String s)    { return s != null ? s.trim() : ""; }
    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private int     len(String s)     { return s != null ? s.trim().length() : 0; }
}