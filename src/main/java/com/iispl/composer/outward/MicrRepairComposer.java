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
 *
 * Bugs fixed in this version:
 *   1. Inline onClick Java code removed → proper @Listen handlers
 *   2. Actual cheque images loaded via ImageServlet
 *   3. Cheque Number is now editable (Textbox, not Label)
 *   4. Tab active state properly toggled on Front/Back switch
 *   5. All 4 views managed via showView() helper
 *   6. cameFromSidebar flag for correct Back navigation
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

    // ── Empty State View ──
    // Button handled via @Listen below

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

    // ── Left Panel — Actual Images ──
    @Wire private Div    chqFront;
    @Wire private Div    chqBack;
    @Wire private Button tabFrontBtn;
    @Wire private Button tabBackBtn;
    @Wire private Image  frontImage;        // ZK Image component for front scan
    @Wire private Image  backImage;         // ZK Image component for back scan
    @Wire private Label  termChqNo;
    @Wire private Label  termMicrCode;

    // ── Right Panel — MICR Entry Fields (all Textbox) ──
    @Wire private Textbox chqNoBox;         // FIX: editable textbox (was Label)
    @Wire private Textbox cityCodeBox;
    @Wire private Textbox bankCodeBox;
    @Wire private Textbox branchCodeBox;
    @Wire private Textbox baseNumberBox;
    @Wire private Textbox transCodeBox;
    @Wire private Textbox repairRemarksBox;

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
            // Came from batch upload → skip batch selection
            cameFromSidebar = false;
            currentBatch    = micrService.getBatch(batchId.trim());

            if (currentBatch == null) {
                Clients.showNotification(
                    "Batch not found: " + batchId,
                    "error", null, "top_center", 3000);
                Executions.sendRedirect("/outward/batchUpload/batchUpload.zul");
                return;
            }
            showView("list");
            loadAndShowList();

        } else {
            // Came from sidebar → show batch selection
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
    //  FIX: was inline onClick Java code in ZUL (caused parse error)
    //       now properly handled by @Listen
    // ════════════════════════════════════════════════════

    @Listen("onClick = #goToBatchUploadBtn")
    public void onGoToBatchUpload() {
        Executions.sendRedirect("/outward/batchUpload/batchUpload.zul");
    }

    // ════════════════════════════════════════════════════
    //  Batch Select View (sidebar access)
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
        row.appendChild(new Label(
            b.getActualAmount() != null
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

        row.appendChild(new Label(
            cheque.getAmount() != null
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

        // ── FIX: Load actual cheque images via ImageServlet ──
        loadChequeImages(cheque);

        // Terminal MICR band display
        String micr = safe(cheque.getMicrCode());
        termChqNo.setValue(safe(cheque.getChequeNo()));
        termMicrCode.setValue(micr.length() > 6 ? micr.substring(6) : micr);

        // Reset tabs — show front first
        chqFront.setVisible(true);
        chqBack.setVisible(false);
        tabFrontBtn.setSclass("chq-tab active");
        tabBackBtn.setSclass("chq-tab");

        // ── Right Panel ──
        // FIX: chqNoBox is now Textbox (editable), not Label
        chqNoBox.setValue(safe(cheque.getChequeNo()));
        cityCodeBox.setValue(safe(cheque.getCityCode()));
        bankCodeBox.setValue(safe(cheque.getBankCode()));
        branchCodeBox.setValue(safe(cheque.getBranchCode()));
        baseNumberBox.setValue(safe(cheque.getBaseNumber()));
        transCodeBox.setValue(safe(cheque.getTransactionCode()));
        repairRemarksBox.setValue("");

        // Highlight mismatched fields yellow
        highlightMismatchFields(cheque);

        rejectPanel.setVisible(false);
    }

    /**
     * Sets the actual cheque image URLs on the ZK Image components.
     * Images are served via ImageServlet from the filesystem.
     *
     * Image path example:
     *   /opt/cts/images/outward/B-2026-0603-001/images/cheque002_front.png
     *
     * Served via:
     *   /imageServlet?path=/opt/cts/images/outward/B-2026-0603-001/images/...
     */
    private void loadChequeImages(OutwardCheque cheque) {
        String frontPath = cheque.getFrontImagePath();
        String backPath  = cheque.getBackImagePath();

        if (frontPath != null && !frontPath.trim().isEmpty()) {
            try {
                String encodedPath = URLEncoder.encode(
                    frontPath.trim(), "UTF-8");
                frontImage.setSrc("/imageServlet?path=" + encodedPath);
            } catch (UnsupportedEncodingException e) {
                frontImage.setSrc("/imageServlet?path=" + frontPath.trim());
            }
        } else {
            frontImage.setSrc("");
        }

        if (backPath != null && !backPath.trim().isEmpty()) {
            try {
                String encodedPath = URLEncoder.encode(
                    backPath.trim(), "UTF-8");
                backImage.setSrc("/imageServlet?path=" + encodedPath);
            } catch (UnsupportedEncodingException e) {
                backImage.setSrc("/imageServlet?path=" + backPath.trim());
            }
        } else {
            backImage.setSrc("");
        }
    }

    // ════════════════════════════════════════════════════
    //  MICR Field Highlighting
    // ════════════════════════════════════════════════════

    private void highlightMismatchFields(OutwardCheque cheque) {
        String micr = cheque.getMicrCode();

        if (micr == null || micr.trim().length() < 23) {
            setYellow(cityCodeBox);
            setYellow(bankCodeBox);
            setYellow(branchCodeBox);
            setYellow(baseNumberBox);
            setYellow(transCodeBox);
            return;
        }

        String expCity   = micr.substring(6,  9);
        String expBank   = micr.substring(9,  12);
        String expBranch = micr.substring(12, 15);
        String expBase   = micr.substring(15, 21);
        String expTc     = micr.substring(21, 23);

        highlightField(cityCodeBox,   cheque.getCityCode(),        expCity);
        highlightField(bankCodeBox,   cheque.getBankCode(),        expBank);
        highlightField(branchCodeBox, cheque.getBranchCode(),      expBranch);
        highlightField(baseNumberBox, cheque.getBaseNumber(),      expBase);
        highlightField(transCodeBox,  cheque.getTransactionCode(), expTc);
    }

    private void highlightField(Textbox box, String stored, String expected) {
        if (!safe(stored).equals(safe(expected))) {
            setYellow(box);
        } else {
            setBlue(box);
        }
    }

    private void setYellow(Textbox box) {
        box.setSclass("fi mono field-changed");
    }

    private void setBlue(Textbox box) {
        box.setSclass("fi mono field-ocr");
    }

    // ════════════════════════════════════════════════════
    //  Tab Switching — FIX: both buttons toggled together
    // ════════════════════════════════════════════════════

    @Listen("onClick = #tabFrontBtn")
    public void onShowFront() {
        chqFront.setVisible(true);
        chqBack.setVisible(false);
        tabFrontBtn.setSclass("chq-tab active");
        tabBackBtn.setSclass("chq-tab");
    }

    @Listen("onClick = #tabBackBtn")
    public void onShowBack() {
        chqFront.setVisible(false);
        chqBack.setVisible(true);
        tabFrontBtn.setSclass("chq-tab");
        tabBackBtn.setSclass("chq-tab active");
    }

    // ════════════════════════════════════════════════════
    //  Prev / Next Navigation
    // ════════════════════════════════════════════════════

    @Listen("onClick = #prevBtn")
    public void onPrev() {
        if (repairList == null || repairList.isEmpty()) return;
        currentIndex = (currentIndex - 1 + repairList.size())
                        % repairList.size();
        loadRepairView(repairList.get(currentIndex));
    }

    @Listen("onClick = #nextBtn")
    public void onNext() {
        if (repairList == null || repairList.isEmpty()) return;
        currentIndex = (currentIndex + 1) % repairList.size();
        loadRepairView(repairList.get(currentIndex));
    }

    // ════════════════════════════════════════════════════
    //  Save and Next
    //  FIX: chequeNo now read from chqNoBox (editable textbox)
    // ════════════════════════════════════════════════════

    @Listen("onClick = #saveNextBtn")
    public void onSaveNext() {
        OutwardCheque cheque = repairList.get(currentIndex);

        // FIX: use chqNoBox.getValue() — chequeNo is now editable
        String chequeNo = chqNoBox.getValue();
        String city     = cityCodeBox.getValue();
        String bank     = bankCodeBox.getValue();
        String branch   = branchCodeBox.getValue();
        String base     = baseNumberBox.getValue();
        String tc       = transCodeBox.getValue();
        String remarks  = repairRemarksBox.getValue();

        if (isBlank(chequeNo) || isBlank(city) || isBlank(bank)
                || isBlank(branch) || isBlank(base) || isBlank(tc)) {
            Clients.showNotification(
                "All MICR fields are required.",
                "warning", null, "top_center", 2500);
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
            micrService.markBatchEntryDone(currentBatch.getId());
            Clients.showNotification(
                "All MICR repairs complete. Batch ready for Account Entry.",
                "info", null, "top_center", 3000);
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
            micrService.markBatchEntryDone(currentBatch.getId());
            Clients.showNotification(
                "All repairs complete. Batch ready for Account Entry.",
                "info", null, "top_center", 3000);
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

    private String safe(String s) {
        return s != null ? s.trim() : "";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}