// File: java/com/iispl/composer/outward/CheckerQueueComposer.java

package com.iispl.composer.outward;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.entity.outward.OutwardMicrRepair;
import com.iispl.service.CheckerService;
import com.iispl.serviceImpl.CheckerServiceImpl;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/outward/CheckerQueueComposer.java
 * Purpose : Drives the Checker Queue screen.
 *
 * Image loading:
 *   Real scanned cheque images are served via ImageServlet.
 *   URL pattern: /imageServlet?path=/opt/cts/images/outward/<batchId>/images/xxx.jpg
 *   The frontImagePath and backImagePath columns in outward_cheque
 *   hold the full filesystem path set during batch upload.
 */
public class CheckerQueueComposer extends SelectorComposer<Component> {

    private final CheckerService checkerService = new CheckerServiceImpl();

    // ── Page State ────────────────────────────────────────────────
    private Long                checkerId;
    private Long                currentBatchDbId;
    private String              currentBatchIdCache;   // batch ID cached for modals (survives status change)
    private List<OutwardCheque> chequeList  = new ArrayList<>();
    private int                 chequeIndex = 0;

    private final DecimalFormat     moneyFmt = new DecimalFormat("#,##0.00");
    private final DateTimeFormatter dateFmt  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Section Control ───────────────────────────────────────────
    @Wire private Div cqEmptyState;
    @Wire private Div cqBatchView;
    @Wire private Div cqProcessView;

    // ── Batch List ────────────────────────────────────────────────
    @Wire private Textbox cqSearchBox;
    @Wire private Listbox cqStatusFilter;
    @Wire private Rows    cqBatchRows;

    // ── Process View Top Bar ──────────────────────────────────────
    @Wire private Label cqBatchBadge;
    @Wire private Div   cqProgressFill;
    @Wire private Label cqProgressText;
    @Wire private Label cqRecLabel;

    // ── Left Panel — Real Image Components ───────────────────────
    @Wire private Div   cqFrontPanel;
    @Wire private Div   cqBackPanel;
    @Wire private Div   cqFrontTab;
    @Wire private Div   cqBackTab;
    @Wire private Image cqFrontImage;      // ZK Image component — src set to ImageServlet URL
    @Wire private Image cqBackImage;       // ZK Image component — src set to ImageServlet URL
    @Wire private Div   cqFrontNoImage;    // Shown when front image path is null in DB
    @Wire private Div   cqBackNoImage;     // Shown when back image path is null in DB
    @Wire private Div   cqChangedFields;
    @Wire private Div   cqChangedList;

    // ── Right Panel ───────────────────────────────────────────────
    @Wire private Label   cqStatusBadge;
    @Wire private Label   cqDetailChequeNo;
    @Wire private Label   cqDetailAmount;
    @Wire private Label   cqDetailHolder;
    @Wire private Label   cqDetailIqa;
    @Wire private Label   cqDetailMicrRepaired;

    @Wire private Textbox cqDeAccNo;
    @Wire private Textbox cqDeAmt;
    @Wire private Textbox cqDeDate;
    @Wire private Textbox cqDeHolder;
    @Wire private Textbox cqDePayee;
    @Wire private Textbox cqDeWords;

    // ── Action Panels ─────────────────────────────────────────────
    @Wire private Div     cqNormalActions;
    @Wire private Button  cqBtnPass;
    @Wire private Button  cqBtnReject;
    @Wire private Button  cqBtnRefer;
    @Wire private Div     cqPassConfirm;
    @Wire private Label   cqPassConfirmNo;
    @Wire private Div     cqRejectForm;
    @Wire private Listbox cqRejectReason;
    @Wire private Textbox cqRejectRemarks;

    // ── Modals ────────────────────────────────────────────────────
    @Wire private Div     cqReferModal;
    @Wire private Listbox cqReferReason;
    @Wire private Listbox cqReferModule;
    @Wire private Textbox cqReferRemarks;
    @Wire private Div     cqHoldModal;
    @Wire private Label   cqHoldMessage;
    @Wire private Div     cqReadyModal;
    @Wire private Label   cqReadyBatchId;
    @Wire private Label   cqReadyPassCount;
    @Wire private Label   cqReadyRejectCount;

    // ════════════════════════════════════════════════════════════════
    //  Page Init
    // ════════════════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        if (!("CHECKER_OUTWARD".equals(dto.getRoleCode()))) {
            Executions.sendRedirect(SessionUtil.getDashboardUrlFor(dto.getRoleCode()));
            return;
        }

        checkerId = dto.getUserId();

        String batchIdParam = Executions.getCurrent().getParameter("batchId");
        List<OutwardBatch> queueBatches = checkerService.getCheckerQueueBatches();

        if (queueBatches.isEmpty() && batchIdParam == null) {
            showEmptyState();
            return;
        }

        showBatchListView();
        renderBatchTable(queueBatches);

        if (batchIdParam != null && !batchIdParam.trim().isEmpty()) {
            try {
                openBatch(Long.parseLong(batchIdParam.trim()));
            } catch (NumberFormatException e) {
                System.err.println("CheckerQueueComposer → invalid batchId param: "
                        + batchIdParam);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  View Switching
    // ════════════════════════════════════════════════════════════════

    private void showEmptyState() {
        cqEmptyState.setVisible(true);
        cqBatchView.setVisible(false);
        cqProcessView.setVisible(false);
    }

    private void showBatchListView() {
        cqEmptyState.setVisible(false);
        cqBatchView.setVisible(true);
        cqProcessView.setVisible(false);
    }

    private void showProcessView() {
        cqEmptyState.setVisible(false);
        cqBatchView.setVisible(false);
        cqProcessView.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════════
    //  Batch List Rendering
    // ════════════════════════════════════════════════════════════════

    private void renderBatchTable(List<OutwardBatch> allBatches) {
        String search  = cqSearchBox.getValue() == null ? ""
                       : cqSearchBox.getValue().trim().toLowerCase();
        String statusF = getSelectedValue(cqStatusFilter);

        List<OutwardBatch> filtered = new ArrayList<>();
        for (OutwardBatch b : allBatches) {
            boolean matchSearch = search.isEmpty()
                    || b.getBatchId().toLowerCase().contains(search)
                    || (b.getSubmittedBy() != null
                        && b.getSubmittedBy().getFullName().toLowerCase().contains(search));
            boolean matchStatus = statusF.isEmpty() || statusF.equals(b.getStatus());
            if (matchSearch && matchStatus) filtered.add(b);
        }

        cqBatchRows.getChildren().clear();

        if (filtered.isEmpty()) {
            Row emptyRow = new Row();
            Label lbl = new Label("No batches found.");
            lbl.setStyle("color:var(--tm); font-size:13px; padding:16px;");
            emptyRow.appendChild(lbl);
            cqBatchRows.appendChild(emptyRow);
            return;
        }

        for (OutwardBatch batch : filtered) {
            Row row = new Row();

            Label batchIdLbl = new Label(batch.getBatchId());
            batchIdLbl.setSclass("mono fw6");
            row.appendChild(batchIdLbl);

            row.appendChild(new Label(String.valueOf(batch.getChequeCount())));

            Label verifiedLbl = new Label(String.valueOf(countVerified(batch)));
            verifiedLbl.setSclass("txt-s fw6");
            row.appendChild(verifiedLbl);

            Label referredLbl = new Label(String.valueOf(countReferred(batch)));
            referredLbl.setSclass("txt-w fw6");
            row.appendChild(referredLbl);

            String makerName = batch.getSubmittedBy() != null
                    ? batch.getSubmittedBy().getFullName() : "—";
            row.appendChild(new Label(makerName));

            String submittedAt = batch.getSubmittedAt() != null
                    ? batch.getSubmittedAt().format(dateFmt) : "—";
            Label submittedLbl = new Label(submittedAt);
            submittedLbl.setSclass("mono");
            row.appendChild(submittedLbl);

            Label statusLbl = new Label(formatStatusLabel(batch.getStatus()));
            statusLbl.setSclass("badge " + getStatusBadgeClass(batch.getStatus()));
            row.appendChild(statusLbl);

            row.appendChild(buildActionCell(batch));
            cqBatchRows.appendChild(row);
        }
    }

    private Component buildActionCell(OutwardBatch batch) {
        String status = batch.getStatus();
        if ("SUBMITTED".equals(status) || "CHECKER_IN_PROGRESS".equals(status)) {
            Button btn = new Button("▶ Process");
            btn.setSclass("btn bp btn-sm");
            final Long id = batch.getId();
            btn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override public void onEvent(Event e) { openBatch(id); }
            });
            return btn;
        } else if ("CHECKER_HOLD".equals(status)) {
            Label lbl = new Label("Waiting for Maker");
            lbl.setStyle("font-size:12px; color:var(--tm);");
            return lbl;
        } else if ("CHECKER_APPROVED".equals(status)) {
            Button btn = new Button("📤 Export");
            btn.setSclass("btn bs btn-sm");
            btn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override public void onEvent(Event e) {
                    Executions.sendRedirect("/outward/demExport/demExport.zul");
                }
            });
            return btn;
        } else {
            Label lbl = new Label("—");
            lbl.setStyle("color:var(--tm);");
            return lbl;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Filter Events
    // ════════════════════════════════════════════════════════════════

    @Listen("onChange = #cqSearchBox; onChanging = #cqSearchBox")
    public void onSearchChanged() {
        renderBatchTable(checkerService.getCheckerQueueBatches());
    }

    @Listen("onChange = #cqStatusFilter")
    public void onStatusFilterChanged() {
        renderBatchTable(checkerService.getCheckerQueueBatches());
    }

    // ════════════════════════════════════════════════════════════════
    //  Open Batch
    // ════════════════════════════════════════════════════════════════

    private void openBatch(Long batchDbId) {
        boolean opened = checkerService.openBatch(batchDbId, checkerId);
        if (!opened) {
            System.err.println("CheckerQueueComposer → openBatch failed for batchDbId="
                    + batchDbId);
            return;
        }

        currentBatchDbId = batchDbId;
        OutwardBatch openedBatch = findBatchById(batchDbId);
        currentBatchIdCache = (openedBatch != null) ? openedBatch.getBatchId() : "—";
        chequeList = checkerService.getChequesForBatch(batchDbId);

        if (chequeList == null || chequeList.isEmpty()) {
            System.err.println("CheckerQueueComposer → no cheques for batchDbId=" + batchDbId);
            return;
        }

        chequeIndex = findFirstUnactionedIndex();

        cqBatchBadge.setValue("Batch: " + currentBatchIdCache);

        showProcessView();
        showFrontTab();
        renderChequeView();
        updateProgress();
    }

    // ════════════════════════════════════════════════════════════════
    //  Back to Queue
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #cqBackBtn")
    public void onBackToQueue() {
        currentBatchDbId    = null;
        currentBatchIdCache = null;
        chequeList       = new ArrayList<>();
        chequeIndex      = 0;
        List<OutwardBatch> batches = checkerService.getCheckerQueueBatches();
        if (batches.isEmpty()) {
            showEmptyState();
        } else {
            showBatchListView();
            renderBatchTable(batches);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Navigation
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #cqPrevBtn")
    public void onPrevCheque() {
        if (chequeList.isEmpty()) return;
        chequeIndex = Math.max(0, chequeIndex - 1);
        showFrontTab();
        renderChequeView();
    }

    @Listen("onClick = #cqNextBtn")
    public void onNextCheque() {
        if (chequeList.isEmpty()) return;
        chequeIndex = Math.min(chequeList.size() - 1, chequeIndex + 1);
        showFrontTab();
        renderChequeView();
    }

    // ════════════════════════════════════════════════════════════════
    //  Tab Toggle
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #cqFrontTab")
    public void onFrontTabClick() { showFrontTab(); }

    @Listen("onClick = #cqBackTab")
    public void onBackTabClick() {
        cqFrontPanel.setVisible(false);
        cqBackPanel.setVisible(true);
        cqFrontTab.setSclass("chq-img-tab");
        cqBackTab.setSclass("chq-img-tab active");
    }

    private void showFrontTab() {
        cqFrontPanel.setVisible(true);
        cqBackPanel.setVisible(false);
        cqFrontTab.setSclass("chq-img-tab active");
        cqBackTab.setSclass("chq-img-tab");
    }

    // ════════════════════════════════════════════════════════════════
    //  Cheque View Rendering
    // ════════════════════════════════════════════════════════════════

    private void renderChequeView() {
        if (chequeList == null || chequeList.isEmpty()) return;
        OutwardCheque cheque = chequeList.get(chequeIndex);

        // ── LEFT PANEL: Load real images from DB paths ────────────
        loadChequeImages(cheque);

        // ── MICR Changed Fields Panel ─────────────────────────────
        List<OutwardMicrRepair> repairs = new ArrayList<>();
        try {
            List<OutwardMicrRepair> loaded = cheque.getMicrRepairs();
            if (loaded != null) repairs = loaded;
        } catch (Exception e) {
            System.err.println("CheckerQueueComposer → getMicrRepairs failed: "
                    + e.getMessage());
        }

        if (!repairs.isEmpty()) {
            cqChangedFields.setVisible(true);
            cqChangedList.getChildren().clear();
            for (OutwardMicrRepair repair : repairs) {
                Div line = new Div();
                line.setSclass("cq-changed-row");
                Label fieldLbl = new Label(formatFieldName(repair.getFieldName()) + ": ");
                fieldLbl.setSclass("cq-changed-field");
                Label oldLbl   = new Label(nvl(repair.getOldValue()));
                oldLbl.setSclass("mono");
                Label arrowLbl = new Label(" → ");
                arrowLbl.setSclass("cq-changed-arrow");
                Label newLbl   = new Label(nvl(repair.getNewValue()));
                newLbl.setSclass("mono cq-changed-new");
                line.appendChild(fieldLbl);
                line.appendChild(oldLbl);
                line.appendChild(arrowLbl);
                line.appendChild(newLbl);
                cqChangedList.appendChild(line);
            }
        } else {
            cqChangedFields.setVisible(false);
            cqChangedList.getChildren().clear();
        }

        // ── RIGHT PANEL: Cheque Details ───────────────────────────
        String amtDisplay = cheque.getAmount() != null
                ? "₹ " + moneyFmt.format(cheque.getAmount()) : "—";

        cqDetailChequeNo.setValue(nvl(cheque.getChequeNo()));
        cqDetailAmount.setValue(amtDisplay);
        cqDetailHolder.setValue(nvl(cheque.getAccountHolder()));

        boolean iqaFail = "FAIL".equalsIgnoreCase(cheque.getIqaStatus());
        cqDetailIqa.setValue(iqaFail ? "FAIL" : "PASS");
        cqDetailIqa.setSclass(iqaFail ? "badge b-iqa-fail" : "badge b-iqa-pass");

        boolean wasRepaired = !repairs.isEmpty();
        cqDetailMicrRepaired.setValue(wasRepaired ? "Yes — Fields Changed" : "No Repair");
        cqDetailMicrRepaired.setSclass(wasRepaired ? "badge b-pend" : "badge b-pass");

        // ── RIGHT PANEL: Data Entry Fields ────────────────────────
        String chequeDateStr = cheque.getChequeDate() != null
                ? cheque.getChequeDate().format(dateFmt) : "";

        cqDeAccNo.setValue(nvl(cheque.getAccountNo()));
        cqDeAmt.setValue(cheque.getAmount() != null
                ? moneyFmt.format(cheque.getAmount()) : "");
        cqDeDate.setValue(chequeDateStr);
        cqDeHolder.setValue(nvl(cheque.getAccountHolder()));
        cqDePayee.setValue(nvl(cheque.getPayeeName()));
        cqDeWords.setValue(nvl(cheque.getAmountInWords()));

        // ── Status Badge ──────────────────────────────────────────
        updateStatusBadge(cheque.getStatus());
        cqRecLabel.setValue("Record " + (chequeIndex + 1) + " of " + chequeList.size());

        // ── Action Button State ───────────────────────────────────
        boolean alreadyActioned = isActioned(cheque.getStatus());
        cqBtnPass.setDisabled(alreadyActioned);
        cqBtnReject.setDisabled(alreadyActioned);
        cqBtnRefer.setDisabled(alreadyActioned);
        if (alreadyActioned) {
            cqBtnPass.setSclass("btn bs cq-action-disabled");
            cqBtnReject.setSclass("btn bd cq-action-disabled");
            cqBtnRefer.setSclass("btn bw cq-action-disabled");
        } else {
            cqBtnPass.setSclass("btn bs");
            cqBtnReject.setSclass("btn bd");
            cqBtnRefer.setSclass("btn bw");
        }

        cqNormalActions.setVisible(true);
        cqPassConfirm.setVisible(false);
        cqRejectForm.setVisible(false);
    }

    // ════════════════════════════════════════════════════════════════
    //  Load Real Cheque Images via ImageServlet
    //  Pattern reused from MicrRepairComposer.loadChequeImages()
    // ════════════════════════════════════════════════════════════════

    /**
     * Sets the ZK Image component src to the ImageServlet URL for this cheque.
     *
     * URL format: /imageServlet?path=<URL-encoded filesystem path>
     * Example:    /imageServlet?path=%2Fopt%2Fcts%2Fimages%2Foutward%2FB-2026-0603-001%2F...
     *
     * If the path is null or blank → shows the "image not available" fallback div.
     * If the path is set → shows the image and hides the fallback div.
     */
    private void loadChequeImages(OutwardCheque cheque) {
        String frontPath = cheque.getFrontImagePath();
        String backPath  = cheque.getBackImagePath();

        // ── Front Image ──────────────────────────────────────────
        if (frontPath != null && !frontPath.trim().isEmpty()) {
            try {
                String encoded = URLEncoder.encode(frontPath.trim(), "UTF-8");
                cqFrontImage.setSrc("/imageServlet?path=" + encoded);
            } catch (UnsupportedEncodingException e) {
                // Fallback: use path without encoding
                cqFrontImage.setSrc("/imageServlet?path=" + frontPath.trim());
            }
            cqFrontImage.setVisible(true);
            cqFrontNoImage.setVisible(false);
            System.out.println("CheckerQueueComposer → front image set: " + frontPath);
        } else {
            // No front image path in DB — show placeholder
            cqFrontImage.setSrc("");
            cqFrontImage.setVisible(false);
            cqFrontNoImage.setVisible(true);
            System.out.println("CheckerQueueComposer → front image path null for chequeId="
                    + cheque.getId());
        }

        // ── Back Image ───────────────────────────────────────────
        if (backPath != null && !backPath.trim().isEmpty()) {
            try {
                String encoded = URLEncoder.encode(backPath.trim(), "UTF-8");
                cqBackImage.setSrc("/imageServlet?path=" + encoded);
            } catch (UnsupportedEncodingException e) {
                cqBackImage.setSrc("/imageServlet?path=" + backPath.trim());
            }
            cqBackImage.setVisible(true);
            cqBackNoImage.setVisible(false);
            System.out.println("CheckerQueueComposer → back image set: " + backPath);
        } else {
            cqBackImage.setSrc("");
            cqBackImage.setVisible(false);
            cqBackNoImage.setVisible(true);
            System.out.println("CheckerQueueComposer → back image path null for chequeId="
                    + cheque.getId());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Progress Bar
    // ════════════════════════════════════════════════════════════════

    private void updateProgress() {
        if (chequeList.isEmpty()) return;
        int total = chequeList.size(), actioned = 0;
        for (OutwardCheque c : chequeList) {
            if (isActioned(c.getStatus())) actioned++;
        }
        cqProgressFill.setStyle("width:" + (int)((actioned * 100.0) / total) + "%");
        cqProgressText.setValue(actioned + "/" + total);
    }

    // ════════════════════════════════════════════════════════════════
    //  PASS Action
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #cqBtnPass")
    public void onPassClick() {
        if (chequeList.isEmpty()) return;
        cqPassConfirmNo.setValue(nvl(chequeList.get(chequeIndex).getChequeNo()));
        cqPassConfirm.setVisible(true);
        cqNormalActions.setVisible(false);
        cqRejectForm.setVisible(false);
    }

    @Listen("onClick = #cqCancelPassBtn")
    public void onCancelPass() {
        cqPassConfirm.setVisible(false);
        cqNormalActions.setVisible(true);
    }

    @Listen("onClick = #cqConfirmPassBtn")
    public void onConfirmPass() {
        if (chequeList.isEmpty()) return;
        OutwardCheque cheque = chequeList.get(chequeIndex);
        boolean success = checkerService.passCheque(
                cheque.getId(), checkerId, currentBatchDbId);
        if (!success) return;
        cheque.setStatus("CHECKER_PASSED");
        cqPassConfirm.setVisible(false);
        cqNormalActions.setVisible(true);
        updateStatusBadge("CHECKER_PASSED");
        disableActionButtons();
        updateProgress();
        int nextIdx = findNextUnactionedIndex(chequeIndex);
        if (nextIdx >= 0) { chequeIndex = nextIdx; showFrontTab(); renderChequeView(); }
        checkBatchCompletion();
    }

    // ════════════════════════════════════════════════════════════════
    //  REJECT Action
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #cqBtnReject")
    public void onRejectClick() {
        cqRejectReason.setSelectedIndex(0);
        cqRejectRemarks.setValue("");
        cqRejectForm.setVisible(true);
        cqNormalActions.setVisible(false);
        cqPassConfirm.setVisible(false);
    }

    @Listen("onClick = #cqCancelRejectBtn")
    public void onCancelReject() {
        cqRejectForm.setVisible(false);
        cqNormalActions.setVisible(true);
    }

    @Listen("onClick = #cqConfirmRejectBtn")
    public void onConfirmReject() {
        if (chequeList.isEmpty()) return;
        String reasonCode = getSelectedValue(cqRejectReason);
        if (reasonCode.isEmpty()) { cqRejectReason.setSclass("fs cq-field-error"); return; }
        cqRejectReason.setSclass("fs");
        String remarks = cqRejectRemarks.getValue() != null
                ? cqRejectRemarks.getValue().trim() : "";
        OutwardCheque cheque = chequeList.get(chequeIndex);
        boolean success = checkerService.rejectCheque(
                cheque.getId(), reasonCode, remarks, checkerId, currentBatchDbId);
        if (!success) return;
        cheque.setStatus("CHECKER_REJECTED");
        cqRejectForm.setVisible(false);
        cqNormalActions.setVisible(true);
        updateStatusBadge("CHECKER_REJECTED");
        disableActionButtons();
        updateProgress();
        int nextIdx = findNextUnactionedIndex(chequeIndex);
        if (nextIdx >= 0) { chequeIndex = nextIdx; showFrontTab(); renderChequeView(); }
        checkBatchCompletion();
    }

    // ════════════════════════════════════════════════════════════════
    //  REFER Action
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #cqBtnRefer")
    public void onReferClick() {
        cqReferReason.setSelectedIndex(0);
        cqReferModule.setSelectedIndex(0);
        cqReferRemarks.setValue("");
        cqReferModal.setSclass("modal-ov open");
    }

    @Listen("onClick = #cqReferModalCloseBtn; onClick = #cqCancelReferBtn")
    public void onCancelRefer() {
        cqReferModal.setSclass("modal-ov");
    }

    @Listen("onClick = #cqConfirmReferBtn")
    public void onConfirmRefer() {
        if (chequeList.isEmpty()) return;
        String reasonCode = getSelectedValue(cqReferReason);
        if (reasonCode.isEmpty()) { cqReferReason.setSclass("fs cq-field-error"); return; }
        cqReferReason.setSclass("fs");
        String module = getSelectedValue(cqReferModule);
        if (module.isEmpty()) { cqReferModule.setSclass("fs cq-field-error"); return; }
        cqReferModule.setSclass("fs");
        String remarks = cqReferRemarks.getValue() != null
                ? cqReferRemarks.getValue().trim() : "";
        OutwardCheque cheque = chequeList.get(chequeIndex);
        boolean success = checkerService.referCheque(
                cheque.getId(), reasonCode, remarks, checkerId, currentBatchDbId);
        if (!success) return;
        cheque.setStatus("CHECKER_REFERRED");
        cqReferModal.setSclass("modal-ov");
        updateStatusBadge("CHECKER_REFERRED");
        disableActionButtons();
        updateProgress();
        int nextIdx = findNextUnactionedIndex(chequeIndex);
        if (nextIdx >= 0) { chequeIndex = nextIdx; showFrontTab(); renderChequeView(); }
        checkBatchCompletion();
    }

    // ════════════════════════════════════════════════════════════════
    //  Batch Completion Check
    // ════════════════════════════════════════════════════════════════

    private void checkBatchCompletion() {
        if (chequeList == null || chequeList.isEmpty()) return;
        boolean allActioned = true;
        boolean anyReferred = false;
        int passCount = 0, rejectCount = 0;
        for (OutwardCheque c : chequeList) {
            if (!isActioned(c.getStatus())) { allActioned = false; break; }
            if ("CHECKER_REFERRED".equals(c.getStatus())) anyReferred = true;
            if ("CHECKER_PASSED".equals(c.getStatus()))   passCount++;
            if ("CHECKER_REJECTED".equals(c.getStatus())) rejectCount++;
        }
        if (!allActioned) return;
        if (anyReferred) showHoldModal();
        else             showReadyModal(passCount, rejectCount);
    }

    // ════════════════════════════════════════════════════════════════
    //  Hold Modal
    // ════════════════════════════════════════════════════════════════

    private void showHoldModal() {
        int referCount = 0, passCount = 0;
        for (OutwardCheque c : chequeList) {
            if ("CHECKER_REFERRED".equals(c.getStatus())) referCount++;
            if ("CHECKER_PASSED".equals(c.getStatus()))   passCount++;
        }
        // Use cached batch id — batch may now be CHECKER_HOLD and still in queue,
        // but cache is safe either way.
        cqHoldMessage.setValue("Batch " + currentBatchIdCache
            + " has " + referCount + " referred cheque(s) and " + passCount
            + " passed cheque(s). Referred cheques sent back to Maker.");
        cqHoldModal.setSclass("modal-ov open");
    }

    @Listen("onClick = #cqHoldOkBtn")
    public void onHoldOk() { cqHoldModal.setSclass("modal-ov"); onBackToQueue(); }

    // ════════════════════════════════════════════════════════════════
    //  Ready to Export Modal
    // ════════════════════════════════════════════════════════════════

    private void showReadyModal(int passCount, int rejectCount) {
        // Use cached batch id — by this point the batch has been auto-approved
        // (CHECKER_APPROVED) and is NO LONGER returned by getCheckerQueueBatches(),
        // so a fresh findBatchById(currentBatchDbId) lookup would return null.
        cqReadyBatchId.setValue(currentBatchIdCache);
        cqReadyPassCount.setValue(String.valueOf(passCount));
        cqReadyRejectCount.setValue(String.valueOf(rejectCount));
        cqReadyModal.setSclass("modal-ov open");
    }

    @Listen("onClick = #cqStayInQueueBtn")
    public void onStayInQueue() { cqReadyModal.setSclass("modal-ov"); onBackToQueue(); }

    @Listen("onClick = #cqGoToExportBtn")
    public void onGoToExport() {
        cqReadyModal.setSclass("modal-ov");
        Executions.sendRedirect("/outward/demExport/demExport.zul");
    }

    // ════════════════════════════════════════════════════════════════
    //  Logout
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() { SessionUtil.logout(); }

    // ════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ════════════════════════════════════════════════════════════════

    private boolean isActioned(String status) {
        return "CHECKER_PASSED".equals(status)
            || "CHECKER_REJECTED".equals(status)
            || "CHECKER_REFERRED".equals(status);
    }

    private int findFirstUnactionedIndex() {
        for (int i = 0; i < chequeList.size(); i++) {
            if (!isActioned(chequeList.get(i).getStatus())) return i;
        }
        return 0;
    }

    private int findNextUnactionedIndex(int fromIndex) {
        for (int i = fromIndex + 1; i < chequeList.size(); i++) {
            if (!isActioned(chequeList.get(i).getStatus())) return i;
        }
        return -1;
    }

    private int countVerified(OutwardBatch batch) {
        int count = 0;
        for (OutwardCheque c : checkerService.getChequesForBatch(batch.getId())) {
            if ("CHECKER_PASSED".equals(c.getStatus())
                    || "CHECKER_REJECTED".equals(c.getStatus())) count++;
        }
        return count;
    }

    private int countReferred(OutwardBatch batch) {
        int count = 0;
        for (OutwardCheque c : checkerService.getChequesForBatch(batch.getId())) {
            if ("CHECKER_REFERRED".equals(c.getStatus())) count++;
        }
        return count;
    }

    private OutwardBatch findBatchById(Long batchDbId) {
        if (batchDbId == null) return null;
        for (OutwardBatch b : checkerService.getCheckerQueueBatches()) {
            if (batchDbId.equals(b.getId())) return b;
        }
        return null;
    }

    private void updateStatusBadge(String status) {
        cqStatusBadge.setValue(formatStatusLabel(status));
        cqStatusBadge.setSclass("badge " + getStatusBadgeClass(status));
    }

    private void disableActionButtons() {
        cqBtnPass.setDisabled(true);   cqBtnPass.setSclass("btn bs cq-action-disabled");
        cqBtnReject.setDisabled(true); cqBtnReject.setSclass("btn bd cq-action-disabled");
        cqBtnRefer.setDisabled(true);  cqBtnRefer.setSclass("btn bw cq-action-disabled");
    }

    private String getSelectedValue(Listbox listbox) {
        Listitem sel = listbox.getSelectedItem();
        if (sel == null) return "";
        Object val = sel.getValue();
        return val != null ? val.toString().trim() : "";
    }

    private String formatStatusLabel(String status) {
        if (status == null) return "—";
        switch (status) {
            case "SUBMITTED":           return "Pending";
            case "CHECKER_IN_PROGRESS": return "In Progress";
            case "CHECKER_HOLD":        return "Hold";
            case "CHECKER_APPROVED":    return "Ready to Export";
            case "CHECKER_PASSED":      return "Passed";
            case "CHECKER_REJECTED":    return "Rejected";
            case "CHECKER_REFERRED":    return "Referred";
            default:                    return status;
        }
    }

    private String getStatusBadgeClass(String status) {
        if (status == null) return "b-grey";
        switch (status) {
            case "SUBMITTED":           return "b-pend";
            case "CHECKER_IN_PROGRESS": return "b-info";
            case "CHECKER_HOLD":        return "b-ref";
            case "CHECKER_APPROVED":    return "b-pass";
            case "CHECKER_PASSED":      return "b-pass";
            case "CHECKER_REJECTED":    return "b-fail";
            case "CHECKER_REFERRED":    return "b-ref";
            default:                    return "b-grey";
        }
    }

    private String formatFieldName(String fieldName) {
        if (fieldName == null) return "—";
        switch (fieldName) {
            case "cheque_no":        return "Cheque No.";
            case "city_code":        return "City Code";
            case "bank_code":        return "Bank Code";
            case "branch_code":      return "Branch Code";
            case "base_number":      return "Base Number";
            case "transaction_code": return "Transaction Code";
            default:                 return fieldName;
        }
    }

    private String nvl(String value) {
        return (value == null || value.trim().isEmpty()) ? "—" : value;
    }
}