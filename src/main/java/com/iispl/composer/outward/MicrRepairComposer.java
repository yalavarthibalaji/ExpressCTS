package com.iispl.composer.outward;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
import org.zkoss.zul.Messagebox;
import com.iispl.service.MakerOutwardService;
import com.iispl.serviceImpl.MakerOutwardServiceImpl;
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

public class MicrRepairComposer extends SelectorComposer<Component> {

    private final MicrRepairService micrService = new MicrRepairServiceImpl();
    private final DecimalFormat     moneyFmt    = new DecimalFormat("#,##0.00");

    // ── Four views ──
    @Wire private Div emptyStateView;
    @Wire private Div batchSelectView;
    @Wire private Div listView;
    @Wire private Div repairView;

    // ── Batch Select View ──
    @Wire private Rows batchSelectRows;

    // ── Pagination ──
    @Wire private Div    batchPager;
    @Wire private Button btnPrevPage;
    @Wire private Button btnNextPage;
    @Wire private Label  batchPagerInfo;

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

    // ── Navigation buttons ──
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

    // ── Save & Next Button ──
    @Wire private Button saveNextBtn;

    // ── Reject Panel ──
    @Wire private Div     rejectPanel;
    @Wire private Listbox rejectReasonBox;
    @Wire private Textbox rejectRemarksBox;

    // ── State ──
    private List<OutwardCheque> repairList;
    private int                 currentIndex        = 0;
    private OutwardBatch        currentBatch;
    private String              batchId;
    private Long                currentMakerId;
    private String              currentMakerName;
    private final MakerOutwardService makerOutwardService = new MakerOutwardServiceImpl();
    private boolean             cameFromSidebar     = false;
    private boolean             currentChequeHasErrors = false;

    // ── Pagination State ──
    private static final int    PAGE_SIZE        = 1;
    private int                 batchPage        = 0;
    private List<OutwardBatch>  batchDisplayList = new ArrayList<>();

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

        currentMakerId   = dto.getUserId();
        currentMakerName = dto.getFullName();

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
        // Hide pager when leaving batch select view
        if (!"batchSelect".equals(view)) {
            batchPager.setVisible(false);
        }
    }

    // ════════════════════════════════════════════════════
    //  Topbar
    // ════════════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() { SessionUtil.logout(); }

    // ════════════════════════════════════════════════════
    //  Batch Select View — with pagination
    // ════════════════════════════════════════════════════

    private void loadBatchSelectView() {
        List<OutwardBatch> batches =
            micrService.getBatchesNeedingRepair(currentMakerId);

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

        int idx = fromIndex + 1;
        for (OutwardBatch b : pageData) {
            batchSelectRows.appendChild(buildBatchSelectRow(idx++, b));
        }

        batchPager.setVisible(totalPages > 1);
        batchPagerInfo.setValue("Page " + (batchPage + 1) + " of " + totalPages
                + "  (" + totalItems + " batches)");
        btnPrevPage.setDisabled(batchPage == 0);
        btnNextPage.setDisabled(batchPage >= totalPages - 1);
    }

    // ════════════════════════════════════════════════════
    //  Pagination Listeners
    // ════════════════════════════════════════════════════

    @Listen("onClick = #btnPrevPage")
    public void onPrevPage() {
        if (batchPage > 0) {
            batchPage--;
            renderBatchPage();
        }
    }

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

        chqFront.setVisible(true);
        chqBack.setVisible(false);
        tabFrontBtn.setSclass("chq-tab active");
        tabBackBtn.setSclass("chq-tab");

        chqNoBox.setValue(safe(cheque.getChequeNo()));
        cityCodeBox.setValue(safe(cheque.getCityCode()));
        bankCodeBox.setValue(safe(cheque.getBankCode()));
        branchCodeBox.setValue(safe(cheque.getBranchCode()));
        baseNumberBox.setValue(safe(cheque.getBaseNumber()));
        transCodeBox.setValue(safe(cheque.getTransactionCode()));
        repairRemarksBox.setValue("");

        currentChequeHasErrors = detectAndHighlightErrors(cheque);

        rejectPanel.setVisible(false);

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
    // ════════════════════════════════════════════════════

    private boolean detectAndHighlightErrors(OutwardCheque cheque) {
        String micr = cheque.getMicrCode();
        if (micr == null || micr.trim().length() < 23) {
            setYellow(cityCodeBox);
            setYellow(bankCodeBox);
            setYellow(branchCodeBox);
            setYellow(baseNumberBox);
            setYellow(transCodeBox);
            return true;
        }

        boolean hasError = false;
        hasError |= highlightField(cityCodeBox,   cheque.getCityCode(),        micr.substring(6,  9));
        hasError |= highlightField(bankCodeBox,   cheque.getBankCode(),        micr.substring(9,  12));
        hasError |= highlightField(branchCodeBox, cheque.getBranchCode(),      micr.substring(12, 15));
        hasError |= highlightField(baseNumberBox, cheque.getBaseNumber(),      micr.substring(15, 21));
        hasError |= highlightField(transCodeBox,  cheque.getTransactionCode(), micr.substring(21, 23));

        return hasError;
    }

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
    //  Prev / Next Navigation (cheque list)
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
    //  Real-time Field Listeners
    // ════════════════════════════════════════════════════

    @Listen("onChanging = #chqNoBox")
    public void onChqNoChanging(InputEvent e) {
        applySaveButtonState(e.getValue(),
            cityCodeBox.getValue(), bankCodeBox.getValue(),
            branchCodeBox.getValue(), baseNumberBox.getValue(),
            transCodeBox.getValue());
    }

    @Listen("onChanging = #cityCodeBox")
    public void onCityChanging(InputEvent e) {
        applySaveButtonState(chqNoBox.getValue(),
            e.getValue(), bankCodeBox.getValue(),
            branchCodeBox.getValue(), baseNumberBox.getValue(),
            transCodeBox.getValue());
    }

    @Listen("onChanging = #bankCodeBox")
    public void onBankChanging(InputEvent e) {
        applySaveButtonState(chqNoBox.getValue(),
            cityCodeBox.getValue(), e.getValue(),
            branchCodeBox.getValue(), baseNumberBox.getValue(),
            transCodeBox.getValue());
    }

    @Listen("onChanging = #branchCodeBox")
    public void onBranchChanging(InputEvent e) {
        applySaveButtonState(chqNoBox.getValue(),
            cityCodeBox.getValue(), bankCodeBox.getValue(),
            e.getValue(), baseNumberBox.getValue(),
            transCodeBox.getValue());
    }

    @Listen("onChanging = #baseNumberBox")
    public void onBaseChanging(InputEvent e) {
        applySaveButtonState(chqNoBox.getValue(),
            cityCodeBox.getValue(), bankCodeBox.getValue(),
            branchCodeBox.getValue(), e.getValue(),
            transCodeBox.getValue());
    }

    @Listen("onChanging = #transCodeBox")
    public void onTcChanging(InputEvent e) {
        applySaveButtonState(chqNoBox.getValue(),
            cityCodeBox.getValue(), bankCodeBox.getValue(),
            branchCodeBox.getValue(), baseNumberBox.getValue(),
            e.getValue());
    }

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
    // ════════════════════════════════════════════════════

    private void applySaveButtonState(String chqNo, String city, String bank,
                                       String branch, String base, String tc) {
        boolean canSave = canEnableSave(chqNo, city, bank, branch, base, tc);

        saveNextBtn.setDisabled(!canSave);
        saveNextBtn.setSclass(canSave
            ? "btn bp btn-lg w100"
            : "btn bp btn-lg w100 btn-disabled");

        boolean navAllowed = !currentChequeHasErrors;
        prevBtn.setDisabled(!navAllowed);
        nextBtn.setDisabled(!navAllowed);
        prevBtn.setSclass(navAllowed ? "btn bo btn-sm" : "btn bo btn-sm btn-disabled");
        nextBtn.setSclass(navAllowed ? "btn bo btn-sm" : "btn bo btn-sm btn-disabled");
    }

    private boolean canEnableSave(String chqNo, String city, String bank,
                                   String branch, String base, String tc) {
        if (repairList == null || currentIndex >= repairList.size()) return false;

        if (len(chqNo)  != 6) return false;
        if (len(city)   != 3) return false;
        if (len(bank)   != 3) return false;
        if (len(branch) != 3) return false;
        if (len(base)   != 6) return false;
        if (len(tc)     != 2) return false;

        OutwardCheque cheque  = repairList.get(currentIndex);
        String        micrCode = cheque.getMicrCode();

        if (micrCode == null || micrCode.trim().length() < 23) return true;

        String expCity   = micrCode.substring(6,  9);
        String expBank   = micrCode.substring(9,  12);
        String expBranch = micrCode.substring(12, 15);
        String expBase   = micrCode.substring(15, 21);
        String expTc     = micrCode.substring(21, 23);

        if (isYellowAndUnchanged(cheque.getCityCode(),        expCity,   city))   return false;
        if (isYellowAndUnchanged(cheque.getBankCode(),        expBank,   bank))   return false;
        if (isYellowAndUnchanged(cheque.getBranchCode(),      expBranch, branch)) return false;
        if (isYellowAndUnchanged(cheque.getBaseNumber(),      expBase,   base))   return false;
        if (isYellowAndUnchanged(cheque.getTransactionCode(), expTc,     tc))     return false;

        return true;
    }

    private boolean isYellowAndUnchanged(String storedWrong, String expected,
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
                int remaining = makerOutwardService.countActiveReferrals(currentBatch.getId());
                if (remaining == 0) {
                    showResubmitPopup();
                } else {
                    Clients.showNotification(
                        remaining + " referred cheque(s) still pending in Data Entry module.",
                        "info", null, "top_center", 3000);
                    goBackToList();
                }
                return;
            }
            goBackToList();
            return;
        }

        if (repairList.isEmpty()) {
            goBackToList();
        } else {
            if (currentIndex >= repairList.size()) currentIndex = repairList.size() - 1;
            loadRepairView(repairList.get(currentIndex));
        }
    }

    // ════════════════════════════════════════════════════
    //  Reject Panel
    // ════════════════════════════════════════════════════

    @Listen("onClick = #rejectTriggerBtn")
    public void onRejectTrigger() {
        rejectPanel.setVisible(true);
        if (rejectReasonBox.getItemCount() > 0) rejectReasonBox.setSelectedIndex(0);
        rejectRemarksBox.setValue("");
    }

    @Listen("onClick = #cancelRejectBtn")
    public void onCancelReject() { rejectPanel.setVisible(false); }

    @Listen("onClick = #confirmRejectBtn")
    public void onConfirmReject() {
        if (rejectReasonBox.getSelectedItem() == null
                || isBlank(rejectReasonBox.getSelectedItem().getValue().toString())) {
            Clients.showNotification(
                "Please select a rejection reason.",
                "warning", null, "top_center", 2500);
            return;
        }

        OutwardCheque cheque  = repairList.get(currentIndex);
        String        reason  = rejectReasonBox.getSelectedItem().getValue().toString();
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
                int remaining = makerOutwardService.countActiveReferrals(currentBatch.getId());
                if (remaining == 0) {
                    showResubmitPopup();
                    return;
                } else {
                    Clients.showNotification(
                        remaining + " referred cheque(s) still pending in other module.",
                        "info", null, "top_center", 3000);
                }
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

    @Listen("onClick = #goToBatchUploadBtn")
    public void goToBatchUpload() {
        Executions.sendRedirect("/outward/batchUpload/batchUpload.zul");
    }

    // ════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════

    private String  safe(String s)    { return s != null ? s.trim() : ""; }
    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private int     len(String s)     { return s != null ? s.trim().length() : 0; }

    // ════════════════════════════════════════════════════
    //  Resubmit Popup
    // ════════════════════════════════════════════════════

    private void showResubmitPopup() {
        String bid = currentBatch != null ? currentBatch.getBatchId() : "";
        Messagebox.show(
            "All referred cheques in batch " + bid + " have been fixed.\n\n"
            + "Resubmit to Checker now?",
            "Resubmit to Checker",
            Messagebox.YES | Messagebox.NO,
            Messagebox.QUESTION,
            event -> {
                if (Messagebox.ON_YES.equals(event.getName())) doResubmit();
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
        goBackToList();
    }
}