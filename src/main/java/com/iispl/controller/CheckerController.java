/*Ramana Created this class and added the buttons and 
 * service class business logic and dao database (dummy data in CheckerDaoImpl class)*/
package com.iispl.controller;

import com.iispl.entity.CheckerBatch;
import com.iispl.entity.CheckerCheque;
import com.iispl.model.UserModel;
import com.iispl.service.CheckerService;
import com.iispl.serviceImpl.CheckerServiceImpl;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * CheckerController.java
 * Package: com.iispl.controller
 *
 * Controls checkerDashboard.zul.
 *
 * Panels:
 *   panelDashboard   — landing summary
 *   panelBatchMgmt   — Batch Management table
 *   panelMicrRepair  — MICR Repair exception queue
 *   panelBatchList   — Checker batch queue (default panel)
 *   panelChequeList  — Cheques inside a batch
 *   panelChequeDetail— Single cheque spot-check
 *   panelReports     — Reports
 *   panelAudit       — Audit log
 *
 * FIXES APPLIED:
 *   1. Removed @Wire for labels that don't exist in checkerDashboard.zul:
 *        dashClearingDate, dashSessionRef, dashBatchRef, dashBatchStatus,
 *        dashBatchType, dashFlowBatchId, dashFlowIqa, dashFlowMakerDone,
 *        divHvAlert, lblHvAlertCount, lblHvAlertAmount,
 *        dashInwardTotal, dashHvQueue, dashVhvQueue, dashInwardCleared,
 *        dashAutoReturned, lblDetailChequeNumImg
 *   2. Removed calls to setValue() on those null wires in populateDashboard()
 *      and populateChequeDetail().
 *   3. CheckerBatch entity now has all fields used in ZUL template.
 */
public class CheckerController extends SelectorComposer<Component> {

    // ── Service ───────────────────────────────────────────────────────
    private final CheckerService checkerService = new CheckerServiceImpl();

    // ── Audit entry inner class ───────────────────────────────────────
    public static class AuditEntry {
        private final String time;
        private final String type;
        private final String message;

        public AuditEntry(String type, String message) {
            this.time    = new SimpleDateFormat("HH:mm:ss").format(new Date());
            this.type    = type;
            this.message = message;
        }

        public String getTime()    { return time; }
        public String getType()    { return type; }
        public String getMessage() { return message; }
    }

    // ── State ─────────────────────────────────────────────────────────
    private String       activeBatchId  = null;
    private CheckerCheque activeCheque  = null;
    private String       activeMicrChequeId = null;
    private String       activeMicrVerifyId = null;
    private UserModel    currentUser;
    private int          returnWindowSeconds = 2 * 60 * 60;
    private final List<AuditEntry> auditLog = new ArrayList<>();

    // ================================================================
    // Wired — All panels
    // ================================================================
    @Wire("#panelDashboard")
    private Div panelDashboard;
    @Wire("#panelBatchMgmt")
    private Div panelBatchMgmt;
    @Wire("#panelMicrRepair")
    private Div panelMicrRepair;
    @Wire("#panelBatchList")
    private Div panelBatchList;
    @Wire("#panelChequeList")
    private Div panelChequeList;
    @Wire("#panelChequeDetail")
    private Div panelChequeDetail;
    @Wire("#panelReports")
    private Div panelReports;
    @Wire("#panelAudit")
    private Div panelAudit;

    // ================================================================
    // Wired — Dashboard
    // FIX: Only wire labels that actually exist in checkerDashboard.zul.
    //      dashClearingDate, dashSessionRef, dashBatchRef, dashBatchStatus,
    //      dashBatchType, dashFlowBatchId, dashFlowIqa, dashFlowMakerDone,
    //      divHvAlert, lblHvAlertCount, lblHvAlertAmount, dashInwardTotal,
    //      dashHvQueue, dashVhvQueue, dashInwardCleared, dashAutoReturned
    //      DO NOT exist in the ZUL — removed to fix NPE.
    // ================================================================
    @Wire("#dashRoleName")
    private Label dashRoleName;
    @Wire("#dashTotalCheques")
    private Label dashTotalCheques;
    @Wire("#dashTotalBatches")
    private Label dashTotalBatches;
    @Wire("#dashPending")
    private Label dashPending;
    // dashApproved exists in ZUL but was missing from controller — added here
    @Wire("#dashApproved")
    private Label dashApproved;

    // ================================================================
    // Wired — Batch Management panel
    // FIX: bmDispatchedBatches and bmTotalAmount and lblBmSessionId exist
    //      in ZUL but were not wired in controller — added below.
    // ================================================================
    @Wire("#bmTotalBatches")
    private Label bmTotalBatches;
    @Wire("#bmApprovedBatches")
    private Label bmApprovedBatches;
    @Wire("#bmPendingBatches")
    private Label bmPendingBatches;
    @Wire("#bmTotalCheques")
    private Label bmTotalCheques;
    @Wire("#bmTotalAmount")
    private Label bmTotalAmount;
    @Wire("#bmDispatchedBatches")
    private Label bmDispatchedBatches;
    @Wire("#bmBatchBadge")
    private Label bmBatchBadge;
    @Wire("#lblBmSessionId")
    private Label lblBmSessionId;
    @Wire("#gridBatchMgmt")
    private Grid gridBatchMgmt;
    @Wire("#divBatchMgmtChequeDetail")
    private Div divBatchMgmtChequeDetail;
    @Wire("#lblBmSelectedBatch")
    private Label lblBmSelectedBatch;
    @Wire("#gridBatchMgmtCheques")
    private Grid gridBatchMgmtCheques;

    // ================================================================
    // Wired — MICR Repair panel
    // ================================================================
    @Wire("#micrTotal")
    private Label micrTotal;
    @Wire("#micrPending")
    private Label micrPending;
    @Wire("#micrRepaired")
    private Label micrRepaired;
    @Wire("#micrVerified")
    private Label micrVerified;
    @Wire("#micrPendingBadge")
    private Label micrPendingBadge;
    @Wire("#gridMicrRepair")
    private Grid gridMicrRepair;
    @Wire("#gridMicrVerify")
    private Grid gridMicrVerify;
    @Wire("#micrAwaitingBadge")
    private Label micrAwaitingBadge;
    @Wire("#divMicrRepairForm")
    private Div divMicrRepairForm;
    @Wire("#lblMicrRepairChequeNum")
    private Label lblMicrRepairChequeNum;
    @Wire("#micrFldMicrCode")
    private Textbox micrFldMicrCode;
    @Wire("#micrFldChequeNum")
    private Textbox micrFldChequeNum;
    @Wire("#micrFldBankName")
    private Textbox micrFldBankName;
    @Wire("#micrFldIfsc")
    private Textbox micrFldIfsc;
    @Wire("#micrFldRemarks")
    private Textbox micrFldRemarks;
    @Wire("#divMicrVerifyPanel")
    private Div divMicrVerifyPanel;
    @Wire("#lblMicrVerifyChequeNum")
    private Label lblMicrVerifyChequeNum;
    @Wire("#micrVfyMicrCode")
    private Textbox micrVfyMicrCode;
    @Wire("#micrVfyChequeNum")
    private Textbox micrVfyChequeNum;
    @Wire("#micrVfyRemarks")
    private Textbox micrVfyRemarks;

    // ================================================================
    // Wired — Checker Batch List panel
    // ================================================================
    @Wire("#gridBatchList")
    private Grid gridBatchList;
    @Wire("#lblBatchCount")
    private Label lblBatchCount;

    // ================================================================
    // Wired — Checker Cheque List panel
    // ================================================================
    @Wire("#lblBreadcrumbBatchId")
    private Label lblBreadcrumbBatchId;
    @Wire("#lblActiveBatchId")
    private Label lblActiveBatchId;
    @Wire("#lblBatchBannerId")
    private Label lblBatchBannerId;
    @Wire("#lblBatchTotalChq")
    private Label lblBatchTotalChq;
    @Wire("#lblBatchAmount")
    private Label lblBatchAmount;
    @Wire("#lblStatTotal")
    private Label lblStatTotal;
    @Wire("#lblStatAmount")
    private Label lblStatAmount;
    @Wire("#lblApprovedCount")
    private Label lblApprovedCount;
    @Wire("#lblRejectedCount")
    private Label lblRejectedCount;
    @Wire("#lblPendingCount")
    private Label lblPendingCount;
    @Wire("#lblApprovalSummary")
    private Label lblApprovalSummary;
    @Wire("#lblChequeListBatchId")
    private Label lblChequeListBatchId;
    @Wire("#divBatchApprovedBanner")
    private Div divBatchApprovedBanner;
    @Wire("#lblApprovedBatchId")
    private Label lblApprovedBatchId;
    @Wire("#divBatchActions")
    private Div divBatchActions;
    @Wire("#gridChequeList")
    private Grid gridChequeList;
    @Wire("#divReturnRemarks")
    private Div divReturnRemarks;
    @Wire("#txtReturnRemarks")
    private Textbox txtReturnRemarks;

    // ================================================================
    // Wired — Cheque Detail panel
    // FIX: Removed @Wire("#lblDetailChequeNumImg") — this label does NOT
    //      exist in checkerDashboard.zul, causing the NPE error.
    //      All other labels below do exist in the ZUL.
    // ================================================================
    @Wire("#lblDetailBatchId")
    private Label lblDetailBatchId;
    @Wire("#lblDetailChequeNum")
    private Label lblDetailChequeNum;
    @Wire("#lblDetailPosition")
    private Label lblDetailPosition;
    @Wire("#lblDetailStatus")
    private Label lblDetailStatus;
    @Wire("#btnBackToList")
    private Button btnBackToList;
    @Wire("#btnPrevCheque")
    private Button btnPrevCheque;
    @Wire("#btnNextCheque")
    private Button btnNextCheque;
    @Wire("#divHighValueAlert")
    private Div divHighValueAlert;
    @Wire("#fldTransactionId")
    private Textbox fldTransactionId;
    @Wire("#fldChequeNumber")
    private Textbox fldChequeNumber;
    @Wire("#fldMicrCode")
    private Textbox fldMicrCode;
    @Wire("#fldIfscCode")
    private Textbox fldIfscCode;
    @Wire("#fldBankName")
    private Textbox fldBankName;
    @Wire("#fldBranchName")
    private Textbox fldBranchName;
    @Wire("#fldDrawerName")
    private Textbox fldDrawerName;
    @Wire("#fldChequeStatus")
    private Textbox fldChequeStatus;
    @Wire("#fldDrawerAcc")
    private Textbox fldDrawerAcc;
    @Wire("#fldAmount")
    private Textbox fldAmount;
    @Wire("#fldAmountWords")
    private Label fldAmountWords;
    @Wire("#fldChequeDate")
    private Textbox fldChequeDate;
    @Wire("#fldPayee")
    private Textbox fldPayee;
    @Wire("#fldDepositorAcc")
    private Textbox fldDepositorAcc;
    @Wire("#fldMakerFlag")
    private Textbox fldMakerFlag;
    @Wire("#fldMakerRemarks")
    private Textbox fldMakerRemarks;
    @Wire("#divDecisionPending")
    private Div divDecisionPending;
    @Wire("#divDecisionDone")
    private Div divDecisionDone;
    @Wire("#lblDecisionResult")
    private Label lblDecisionResult;
    @Wire("#txtCheckerRemarks")
    private Textbox txtCheckerRemarks;
    @Wire("#btnApproveNext")
    private Button btnApproveNext;
    @Wire("#btnReturnFile")
    private Button btnReturnFile;
    @Wire("#btnEscalate")
    private Button btnEscalate;
    @Wire("#lblImgBank")
    private Label lblImgBank;
    @Wire("#lblImgChequeNum")
    private Label lblImgChequeNum;
    @Wire("#lblImgPayee")
    private Label lblImgPayee;
    @Wire("#lblImgDate")
    private Label lblImgDate;
    @Wire("#lblImgAmount")
    private Label lblImgAmount;
    @Wire("#lblImgSig")
    private Label lblImgSig;
    @Wire("#lblImgMicr")
    private Label lblImgMicr;
    @Wire("#lblBackBank")
    private Label lblBackBank;
    @Wire("#lblBackAcc")
    private Label lblBackAcc;
    @Wire("#lblBackDate")
    private Label lblBackDate;

    // ================================================================
    // Wired — Reports & Audit
    // ================================================================
    @Wire("#rptBatchCount")
    private Label rptBatchCount;
    @Wire("#rptApproved")
    private Label rptApproved;
    @Wire("#rptRejected")
    private Label rptRejected;
    @Wire("#rptAmount")
    private Label rptAmount;
    @Wire("#gridAuditLog")
    private Grid gridAuditLog;
    @Wire("#lblAuditCount")
    private Label lblAuditCount;

    // ================================================================
    // Wired — Bottom bar
    // ================================================================
    @Wire("#barTotal")
    private Label barTotal;
    @Wire("#barPending")
    private Label barPending;
    @Wire("#barApproved")
    private Label barApproved;
    @Wire("#barRejected")
    private Label barRejected;
    @Wire("#lblBottomStatus")
    private Label lblBottomStatus;

    // ================================================================
    // Lifecycle
    // ================================================================
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        Object sessionUser = Sessions.getCurrent().getAttribute("loggedInUser");
        if (sessionUser == null) {
            Executions.sendRedirect("/zul/login.zul");
            return;
        }
        currentUser = (UserModel) sessionUser;

        addAudit("SYSTEM", "Login: " + currentUser.getRoleLabel() + " (" + currentUser.getUserId() + ")");

        // Default view: checker batch list
        showBatchListPanel();
    }

    // ================================================================
    // Panel switching — hideAll first, then show one
    // ================================================================

    private void hideAllPanels() {
        panelDashboard.setVisible(false);
        panelBatchMgmt.setVisible(false);
        panelMicrRepair.setVisible(false);
        panelBatchList.setVisible(false);
        panelChequeList.setVisible(false);
        panelChequeDetail.setVisible(false);
        panelReports.setVisible(false);
        panelAudit.setVisible(false);
    }

    private void showDashboardPanel() {
        setNavActive("navDashboard");
        hideAllPanels();
        activeBatchId = null;
        activeCheque  = null;
        panelDashboard.setVisible(true);
        populateDashboard();
    }

    private void showBatchMgmtPanel() {
        setNavActive("navBatchMgmt");
        hideAllPanels();
        activeBatchId = null;
        activeCheque  = null;
        panelBatchMgmt.setVisible(true);
        divBatchMgmtChequeDetail.setVisible(false);
        loadBatchMgmt();
    }

    private void showMicrRepairPanel() {
        setNavActive("navMicrRepair");
        hideAllPanels();
        activeBatchId      = null;
        activeCheque       = null;
        activeMicrChequeId = null;
        activeMicrVerifyId = null;
        panelMicrRepair.setVisible(true);
        divMicrRepairForm.setVisible(false);
        divMicrVerifyPanel.setVisible(false);
        loadMicrRepair();
    }

    private void showBatchListPanel() {
        setNavActive("navChecker");
        hideAllPanels();
        activeBatchId = null;
        activeCheque  = null;
        panelBatchList.setVisible(true);
        loadBatchList();
        refreshBottomBar();
    }

    private void showChequeListView(String batchId) {
        activeBatchId = batchId;
        activeCheque  = null;
        hideAllPanels();
        panelChequeList.setVisible(true);
        divReturnRemarks.setVisible(false);
        loadChequeList();
        refreshBottomBar();
    }

    private void showChequeDetailView(CheckerCheque cheque) {
        activeCheque = cheque;
        hideAllPanels();
        panelChequeDetail.setVisible(true);
        populateChequeDetail();
    }

    private void showReportsPanel() {
        setNavActive("navReports");
        hideAllPanels();
        panelReports.setVisible(true);
        populateReports();
    }

    private void showAuditPanel() {
        setNavActive("navAudit");
        hideAllPanels();
        panelAudit.setVisible(true);
        loadAuditLog();
    }

    // ================================================================
    // Data loaders — Dashboard
    // FIX: Only set labels that exist in the ZUL (dashRoleName, dashTotalCheques,
    //      dashTotalBatches, dashPending, dashApproved).
    //      Removed all setValue() calls for non-existent labels.
    // ================================================================
    private void populateDashboard() {
        dashRoleName.setValue(currentUser.getRoleLabel());

        List<CheckerBatch> batches = checkerService.getAllCheckerBatches();

        int totalCheques = 0;
        int totalPending = 0;
        int totalApproved = 0;

        for (CheckerBatch b : batches) {
            totalCheques  += b.getTotalCheques();
            totalPending  += b.getPendingCount();
            totalApproved += b.getApprovedCount();
        }

        dashTotalCheques.setValue(String.valueOf(totalCheques));
        dashTotalBatches.setValue(String.valueOf(batches.size()));
        dashPending.setValue(String.valueOf(totalPending));
        dashApproved.setValue(String.valueOf(totalApproved));
    }

    // ================================================================
    // Data loaders — Batch Management
    // FIX: Added setValue() for bmTotalAmount, bmDispatchedBatches,
    //      lblBmSessionId which exist in ZUL but were not being set.
    // ================================================================
    private void loadBatchMgmt() {
        List<CheckerBatch> batches = checkerService.getAllCheckerBatches();

        int    approved      = 0;
        int    pending       = 0;
        int    dispatched    = 0;
        int    totalCheques  = 0;
        long   totalAmount   = 0;

        for (CheckerBatch b : batches) {
            totalCheques += b.getTotalCheques();
            totalAmount  += b.getTotalAmount();
            String st = b.getStatus();
            if ("APPROVED".equals(st))   approved++;
            else if ("DISPATCHED".equals(st)) dispatched++;
            else pending++;
        }

        bmTotalBatches.setValue(String.valueOf(batches.size()));
        bmApprovedBatches.setValue(String.valueOf(approved));
        bmPendingBatches.setValue(String.valueOf(pending));
        bmDispatchedBatches.setValue(String.valueOf(dispatched));
        bmTotalCheques.setValue(String.valueOf(totalCheques));
        bmTotalAmount.setValue(formatAmount(totalAmount));
        bmBatchBadge.setValue(batches.size() + " batch(es)");
        lblBmSessionId.setValue(new SimpleDateFormat("dd-MMM-yyyy").format(new Date()));

        gridBatchMgmt.setModel(new ListModelList<>(batches));
    }

    // ================================================================
    // Data loaders — MICR Repair
    // ================================================================
    private void loadMicrRepair() {
        List<CheckerCheque> micrCheques = checkerService.getMicrRepairCheques();

        int pending = 0, repaired = 0, verified = 0;
        List<CheckerCheque> pendingList = new ArrayList<>();
        List<CheckerCheque> verifyList  = new ArrayList<>();

        for (CheckerCheque c : micrCheques) {
            String st = safe(c.getCheckerStatus());
            if ("approved".equals(st)) {
                verified++;
            } else if ("repaired".equals(st)) {
                repaired++;
                verifyList.add(c);
            } else {
                pending++;
                pendingList.add(c);
            }
        }

        micrTotal.setValue(String.valueOf(micrCheques.size()));
        micrPending.setValue(String.valueOf(pending));
        micrRepaired.setValue(String.valueOf(repaired));
        micrVerified.setValue(String.valueOf(verified));
        micrPendingBadge.setValue(pending + " pending");
        micrAwaitingBadge.setValue(repaired + " awaiting");

        gridMicrRepair.setModel(new ListModelList<>(pendingList));
        gridMicrVerify.setModel(new ListModelList<>(verifyList));
    }

    // ================================================================
    // Data loaders — Checker Batch List
    // ================================================================
    private void loadBatchList() {
        List<CheckerBatch> batches = checkerService.getAllCheckerBatches();
        lblBatchCount.setValue(batches.size() + " batch(es)");
        gridBatchList.setModel(new ListModelList<>(batches));
    }

    // ================================================================
    // Data loaders — Checker Cheque List
    // ================================================================
    private void loadChequeList() {
        List<CheckerCheque> cheques = checkerService.getChequesByBatchId(activeBatchId);

        lblBreadcrumbBatchId.setValue(activeBatchId);
        lblActiveBatchId.setValue(activeBatchId);
        lblBatchBannerId.setValue(activeBatchId);
        lblBatchTotalChq.setValue(String.valueOf(cheques.size()));
        lblChequeListBatchId.setValue(activeBatchId);

        long total = 0, approved = 0, rejected = 0, pending = 0;
        for (CheckerCheque c : cheques) {
            total += c.getAmountInFigures();
            switch (safe(c.getCheckerStatus())) {
                case "approved": approved++; break;
                case "rejected": rejected++; break;
                default:         pending++;  break;
            }
        }

        String fmtTotal = formatAmount(total);
        lblBatchAmount.setValue(fmtTotal);
        lblStatTotal.setValue(String.valueOf(cheques.size()));
        lblStatAmount.setValue(fmtTotal);
        lblApprovedCount.setValue(String.valueOf(approved));
        lblRejectedCount.setValue(String.valueOf(rejected));
        lblPendingCount.setValue(String.valueOf(pending));
        lblApprovalSummary.setValue(cheques.size() + " cheques · " + fmtTotal);

        boolean batchApproved = isBatchApproved(activeBatchId);
        divBatchApprovedBanner.setVisible(batchApproved);
        divBatchActions.setVisible(!batchApproved);
        if (batchApproved) {
            lblApprovedBatchId.setValue(activeBatchId);
        }

        gridChequeList.setModel(new ListModelList<>(cheques));
    }

    // ================================================================
    // Data loaders — Cheque Detail
    // FIX: Removed lblDetailChequeNumImg.setValue() — that label does NOT
    //      exist in checkerDashboard.zul. Removed @Wire for it too.
    //      Everything else remains the same.
    // ================================================================
    private void populateChequeDetail() {
        if (activeCheque == null) return;

        List<CheckerCheque> batch = checkerService.getChequesByBatchId(activeBatchId);
        int idx  = findIndex(batch, activeCheque.getChequeId());
        int size = batch.size();

        lblDetailBatchId.setValue(activeBatchId);
        lblDetailChequeNum.setValue("Cheque #" + safe(activeCheque.getChequeNumber()));
        // NOTE: lblDetailChequeNumImg was removed — it does not exist in the ZUL.
        lblDetailPosition.setValue("Cheque " + (idx + 1) + " of " + size + " in batch");
        lblDetailStatus.setValue(statusLabel(activeCheque.getCheckerStatus()));
        lblDetailStatus.setSclass(statusSclass(activeCheque.getCheckerStatus()));

        btnBackToList.setLabel("\u2190 Back to " + safe(activeBatchId));
        btnPrevCheque.setDisabled(idx <= 0);
        btnNextCheque.setDisabled(idx >= size - 1);
        divHighValueAlert.setVisible(activeCheque.isHighValue());

        fldTransactionId.setValue(safe(activeCheque.getTransactionId()));
        fldChequeNumber.setValue(safe(activeCheque.getChequeNumber()));
        fldMicrCode.setValue(safe(activeCheque.getMicrCode()));
        fldIfscCode.setValue(safe(activeCheque.getIfscCode()));
        fldBankName.setValue(safe(activeCheque.getBankName()));
        fldBranchName.setValue(safe(activeCheque.getBranchName()));
        fldDrawerName.setValue(safe(activeCheque.getDrawerName()));
        fldChequeStatus.setValue(safe(activeCheque.getChequeStatus()));
        fldDrawerAcc.setValue(safe(activeCheque.getDrawerAccountNumber()));
        fldAmount.setValue(formatAmount(activeCheque.getAmountInFigures()));
        fldAmountWords.setValue(safe(activeCheque.getAmountInWords()));
        fldChequeDate.setValue(safe(activeCheque.getChequeDate()));
        fldPayee.setValue(safe(activeCheque.getPayeeName()));
        fldDepositorAcc.setValue(safe(activeCheque.getDepositorAccount()));
        fldMakerFlag.setValue(
            safe(activeCheque.getMakerFlag()).isEmpty() ? "— No flag —" : activeCheque.getMakerFlag()
        );
        fldMakerRemarks.setValue(
            safe(activeCheque.getMakerRemarks()).isEmpty() ? "—" : activeCheque.getMakerRemarks()
        );

        boolean decided = "approved".equals(activeCheque.getCheckerStatus())
                       || "rejected".equals(activeCheque.getCheckerStatus());
        divDecisionPending.setVisible(!decided);
        divDecisionDone.setVisible(decided);

        if (decided) {
            lblDecisionResult.setValue(
                "approved".equals(activeCheque.getCheckerStatus())
                    ? "✅ Spot-check Passed."
                    : "⚠ Exception flagged. Remarks: " + safe(activeCheque.getCheckerRemarks())
            );
        } else {
            txtCheckerRemarks.setValue("");
            btnApproveNext.setDisabled(idx >= size - 1);
        }

        // Populate the cheque image simulation labels
        lblImgBank.setValue(safe(activeCheque.getBankName()));
        lblImgChequeNum.setValue(
            "No: " + activeCheque.getChequeNumber() + "  |  " + safe(activeCheque.getBranchName())
        );
        lblImgPayee.setValue(safe(activeCheque.getPayeeName()));
        lblImgDate.setValue(safe(activeCheque.getChequeDate()));
        lblImgAmount.setValue(formatAmount(activeCheque.getAmountInFigures()));

        String dn = activeCheque.getDrawerName();
        lblImgSig.setValue(dn != null && dn.contains(" ") ? dn.split(" ")[0] : safe(dn));
        lblImgMicr.setValue(
            "|" + safe(activeCheque.getChequeNumber()) + "|  "
            + safe(activeCheque.getMicrCode()) + "|  "
            + safe(activeCheque.getDrawerAccountNumber()) + "|"
        );
        lblBackBank.setValue(safe(activeCheque.getBankName()));
        lblBackAcc.setValue("A/C: " + safe(activeCheque.getDrawerAccountNumber()));
        lblBackDate.setValue("Date: " + safe(activeCheque.getChequeDate()));
    }

    // ================================================================
    // Data loaders — Reports & Audit
    // ================================================================
    private void populateReports() {
        // getReportStats() queries ALL batches — including approved ones
        // that no longer appear in getAllCheckerBatches() (which only returns pending).
        // Returns: [totalBatches, approvedBatches, rejectedCheques, totalApprovedAmount]
        long[] stats = checkerService.getReportStats();
        rptBatchCount.setValue(String.valueOf(stats[0]));
        rptApproved.setValue(String.valueOf(stats[1]));
        rptRejected.setValue(String.valueOf(stats[2]));
        rptAmount.setValue(formatAmount(stats[3]));
    }

    private void loadAuditLog() {
        lblAuditCount.setValue(auditLog.size() + " entries");
        updateSidebarAuditCount();
        gridAuditLog.setModel(new ListModelList<>(auditLog));
    }

    private void refreshBottomBar() {
        List<CheckerBatch> batches = checkerService.getAllCheckerBatches();
        int total = 0, approved = 0, rejected = 0, pending = 0;
        for (CheckerBatch b : batches) {
            total    += b.getTotalCheques();
            approved += b.getApprovedCount();
            rejected += b.getRejectedCount();
            pending  += b.getPendingCount();
        }
        barTotal.setValue(String.valueOf(total));
        barPending.setValue(String.valueOf(pending));
        barApproved.setValue(String.valueOf(approved));
        barRejected.setValue(String.valueOf(rejected));
        lblBottomStatus.setValue("Checker Queue \u2014 " + approved + " of " + total + " done");
    }

    private void addAudit(String type, String message) {
        // Step 1: Add to in-memory list (for immediate UI display)
        auditLog.add(0, new AuditEntry(type, message));
        updateSidebarAuditCount();

        // Step 2: Persist to Supabase audit_logs table
        // Derive batchRef and chequeRef from context if available
        String batchRef  = activeBatchId;
        String chequeRef = (activeCheque != null) ? activeCheque.getChequeId() : null;
        String userId    = (currentUser != null) ? currentUser.getUserId() : "system";
        String userRole  = (currentUser != null) ? currentUser.getRoleId()  : "CHECKER";

        // Map simple type to action_code (matches audit_logs.action_code column)
        String actionCode;
        switch (type) {
            case "APPROVE": actionCode = "CHECKER_APPROVE"; break;
            case "REJECT":  actionCode = "CHECKER_REJECT";  break;
            case "BATCH":   actionCode = "BATCH_ACTION";    break;
            default:        actionCode = "SYSTEM";           break;
        }

        checkerService.insertAuditLog(type, actionCode, message, userId, userRole, batchRef, chequeRef);
    }

    // ================================================================
    // Event Handlers — Timer
    // ================================================================
    @Listen("onTimerTick = #chkApp")
    public void onTimerTick(Event e) {
        if (returnWindowSeconds > 0) returnWindowSeconds--;
        // Timer display is handled by header.zul / HeaderController.
    }

    // ================================================================
    // Event Handlers — Sidebar nav
    // ================================================================
    @Listen("onNavDashboard = #chkApp")
    public void onNavDashboard(Event e) {
        showDashboardPanel();
    }

    @Listen("onNavBatchMgmt = #chkApp")
    public void onNavBatchMgmt(Event e) {
        showBatchMgmtPanel();
    }

    @Listen("onNavMicrRepair = #chkApp")
    public void onNavMicrRepair(Event e) {
        showMicrRepairPanel();
    }

    @Listen("onNavChecker = #chkApp")
    public void onNavChecker(Event e) {
        showBatchListPanel();
    }

    @Listen("onNavReports = #chkApp")
    public void onNavReports(Event e) {
        showReportsPanel();
    }

    @Listen("onNavAudit = #chkApp")
    public void onNavAudit(Event e) {
        showAuditPanel();
    }

    // ================================================================
    // Event Handlers — Search
    // ================================================================
    @Listen("onSearch = #chkApp")
    public void onSearch(Event e) {
        String kw = e.getData() == null ? "" : e.getData().toString().trim().toLowerCase();
        if (kw.isEmpty()) return;

        if (activeBatchId != null) {
            List<CheckerCheque> all = checkerService.getChequesByBatchId(activeBatchId);
            List<CheckerCheque> out = new ArrayList<>();
            for (CheckerCheque c : all) {
                if (safe(c.getChequeNumber()).toLowerCase().contains(kw)
                        || safe(c.getDrawerName()).toLowerCase().contains(kw)
                        || safe(c.getPayeeName()).toLowerCase().contains(kw)
                        || safe(c.getBankName()).toLowerCase().contains(kw)) {
                    out.add(c);
                }
            }
            hideAllPanels();
            panelChequeList.setVisible(true);
            gridChequeList.setModel(new ListModelList<>(out));
        } else {
            List<CheckerBatch> all = checkerService.getAllCheckerBatches();
            List<CheckerBatch> out = new ArrayList<>();
            for (CheckerBatch b : all) {
                if (safe(b.getBatchId()).toLowerCase().contains(kw)) {
                    out.add(b);
                }
            }
            hideAllPanels();
            panelBatchList.setVisible(true);
            gridBatchList.setModel(new ListModelList<>(out));
        }
        addAudit("SYSTEM", "Search: \"" + kw + "\"");
    }

    // ================================================================
    // Event Handlers — Batch Management
    // ================================================================
    @Listen("onViewBatchCheques = #chkApp")
    public void onViewBatchCheques(Event e) {
        String batchId = (String) e.getData();
        if (batchId == null) return;
        List<CheckerCheque> cheques = checkerService.getChequesByBatchId(batchId);
        lblBmSelectedBatch.setValue(batchId);
        gridBatchMgmtCheques.setModel(new ListModelList<>(cheques));
        divBatchMgmtChequeDetail.setVisible(true);
        addAudit("SYSTEM", "Batch Mgmt — viewed cheques of: " + batchId);
    }

    @Listen("onClick = #btnBmCloseCheques")
    public void onBmCloseCheques() {
        divBatchMgmtChequeDetail.setVisible(false);
    }

    @Listen("onVerifyBatch = #chkApp")
    public void onVerifyBatch(Event e) {
        String batchId = (String) e.getData();
        if (batchId == null) return;
        try {
            checkerService.approveBatch(batchId);
            addAudit("APPROVE", "Batch Mgmt — Batch " + batchId + " VERIFIED.");
            Messagebox.show("Batch " + batchId + " verified and approved.",
                "Verified", Messagebox.OK, Messagebox.INFORMATION);
            loadBatchMgmt();
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

    // ================================================================
    // Event Handlers — MICR Repair form
    // ================================================================
    @Listen("onRepairCheque = #chkApp")
    public void onRepairCheque(Event e) {
        String chequeId = (String) e.getData();
        if (chequeId == null) return;
        CheckerCheque c = checkerService.getChequeById(chequeId);
        if (c == null) return;
        activeMicrChequeId = chequeId;

        lblMicrRepairChequeNum.setValue(safe(c.getChequeNumber()));
        micrFldMicrCode.setValue(safe(c.getMicrCode()));
        micrFldChequeNum.setValue(safe(c.getChequeNumber()));
        micrFldBankName.setValue(safe(c.getBankName()));
        micrFldIfsc.setValue(safe(c.getIfscCode()));
        micrFldRemarks.setValue("");

        divMicrRepairForm.setVisible(true);
        divMicrVerifyPanel.setVisible(false);
    }

    @Listen("onClick = #btnCloseMicrRepairForm")
    public void onCloseMicrRepairForm() {
        divMicrRepairForm.setVisible(false);
        activeMicrChequeId = null;
    }

    @Listen("onClick = #btnSubmitMicrRepair")
    public void onSubmitMicrRepair() {
        if (activeMicrChequeId == null) return;
        String remarks = micrFldRemarks.getValue().trim();
        if (remarks.isEmpty()) {
            Messagebox.show("Repair remarks are required.", "Required", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        checkerService.submitMicrRepair(activeMicrChequeId,
            micrFldMicrCode.getValue().trim(),
            micrFldChequeNum.getValue().trim(),
            micrFldBankName.getValue().trim(),
            micrFldIfsc.getValue().trim(),
            remarks);
        addAudit("SYSTEM", "MICR Repair submitted for cheque: " + activeMicrChequeId);
        divMicrRepairForm.setVisible(false);
        activeMicrChequeId = null;
        loadMicrRepair();
    }

    @Listen("onClick = #btnReturnToDepositor")
    public void onReturnToDepositor() {
        if (activeMicrChequeId == null) return;
        String remarks = micrFldRemarks.getValue().trim();
        if (remarks.isEmpty()) {
            Messagebox.show("Remarks required before returning to depositor.",
                "Required", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        checkerService.rejectCheque(activeMicrChequeId, "Returned to depositor. " + remarks);
        addAudit("REJECT", "MICR cheque returned to depositor: " + activeMicrChequeId);
        divMicrRepairForm.setVisible(false);
        activeMicrChequeId = null;
        loadMicrRepair();
    }

    // ================================================================
    // Event Handlers — MICR Verify panel
    // ================================================================
    @Listen("onVerifyMicrCheque = #chkApp")
    public void onVerifyMicrCheque(Event e) {
        String chequeId = (String) e.getData();
        if (chequeId == null) return;
        CheckerCheque c = checkerService.getChequeById(chequeId);
        if (c == null) return;
        activeMicrVerifyId = chequeId;

        lblMicrVerifyChequeNum.setValue(safe(c.getChequeNumber()));
        micrVfyMicrCode.setValue(safe(c.getMicrCode()));
        micrVfyChequeNum.setValue(safe(c.getChequeNumber()));
        micrVfyRemarks.setValue("");

        divMicrVerifyPanel.setVisible(true);
        divMicrRepairForm.setVisible(false);
    }

    @Listen("onClick = #btnCloseMicrVerify")
    public void onCloseMicrVerify() {
        divMicrVerifyPanel.setVisible(false);
        activeMicrVerifyId = null;
    }

    @Listen("onClick = #btnApproveMicrRepair")
    public void onApproveMicrRepair() {
        if (activeMicrVerifyId == null) return;
        checkerService.approveCheque(activeMicrVerifyId, micrVfyRemarks.getValue().trim());
        addAudit("APPROVE", "MICR Repair verified and approved: " + activeMicrVerifyId);
        Messagebox.show("MICR Repair approved.", "Approved", Messagebox.OK, Messagebox.INFORMATION);
        divMicrVerifyPanel.setVisible(false);
        activeMicrVerifyId = null;
        loadMicrRepair();
    }

    @Listen("onClick = #btnRejectMicrRepair")
    public void onRejectMicrRepair() {
        if (activeMicrVerifyId == null) return;
        String remarks = micrVfyRemarks.getValue().trim();
        if (remarks.isEmpty()) {
            Messagebox.show("Remarks required for rejection.", "Required", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        checkerService.resetMicrChequeStatus(activeMicrVerifyId, remarks);
        addAudit("REJECT", "MICR Repair rejected, returned to Maker: " + activeMicrVerifyId);
        divMicrVerifyPanel.setVisible(false);
        activeMicrVerifyId = null;
        loadMicrRepair();
    }

    // ================================================================
    // Event Handlers — Checker Batch list
    // ================================================================
    @Listen("onOpenBatch = #chkApp")
    public void onOpenBatch(Event e) {
        String batchId = (String) e.getData();
        if (batchId != null && !batchId.isEmpty()) {
            addAudit("SYSTEM", "Opened batch: " + batchId);
            showChequeListView(batchId);
        }
    }

    @Listen("onClick = #btnBackToBatchList")
    public void onBackToBatchList() {
        showBatchListPanel();
    }

    // ================================================================
    // Event Handlers — Batch approve / return
    // ================================================================
    @Listen("onClick = #btnApproveBatch")
    public void onApproveBatch() {
        try {
            checkerService.approveBatch(activeBatchId);
            addAudit("APPROVE", "Batch " + activeBatchId + " APPROVED.");
            Messagebox.show("Batch " + activeBatchId + " APPROVED.",
                "Approved", Messagebox.OK, Messagebox.INFORMATION);
            showBatchListPanel();
        } catch (IllegalStateException ex) {
            Messagebox.show(ex.getMessage(), "Cannot Approve", Messagebox.OK, Messagebox.EXCLAMATION);
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

    @Listen("onClick = #btnReturnBatch")
    public void onReturnBatchClicked() {
        divReturnRemarks.setVisible(true);
        txtReturnRemarks.setValue("");
        txtReturnRemarks.setFocus(true);
    }

    @Listen("onClick = #btnCancelReturn")
    public void onCancelReturn() {
        divReturnRemarks.setVisible(false);
    }

    @Listen("onClick = #btnConfirmReturn")
    public void onConfirmReturn() {
        String remarks = txtReturnRemarks.getValue().trim();
        try {
            checkerService.returnBatchToMaker(activeBatchId, remarks);
            addAudit("REJECT", "Batch " + activeBatchId + " returned to Maker. Remarks: " + remarks);
            Messagebox.show("Batch " + activeBatchId + " returned to Maker.",
                "Returned", Messagebox.OK, Messagebox.INFORMATION);
            showBatchListPanel();
        } catch (IllegalArgumentException ex) {
            Messagebox.show(ex.getMessage(), "Required", Messagebox.OK, Messagebox.EXCLAMATION);
        }
    }

    // ================================================================
    // Event Handlers — Cheque detail
    // ================================================================
    @Listen("onViewCheque = #chkApp")
    public void onViewCheque(Event e) {
        String chequeId = (String) e.getData();
        if (chequeId != null) {
            CheckerCheque c = checkerService.getChequeById(chequeId);
            if (c != null) showChequeDetailView(c);
        }
    }

    @Listen("onClick = #btnBackToList")
    public void onBackToList() {
        showChequeListView(activeBatchId);
    }

    @Listen("onClick = #btnPrevCheque")
    public void onPrevCheque() {
        List<CheckerCheque> batch = checkerService.getChequesByBatchId(activeBatchId);
        int idx = findIndex(batch, activeCheque.getChequeId());
        if (idx > 0) showChequeDetailView(batch.get(idx - 1));
    }

    @Listen("onClick = #btnNextCheque")
    public void onNextCheque() {
        List<CheckerCheque> batch = checkerService.getChequesByBatchId(activeBatchId);
        int idx = findIndex(batch, activeCheque.getChequeId());
        if (idx < batch.size() - 1) showChequeDetailView(batch.get(idx + 1));
    }

    @Listen("onClick = #btnApprove")
    public void onApprove() {
        String remarks = txtCheckerRemarks.getValue().trim();
        checkerService.approveCheque(activeCheque.getChequeId(), remarks);
        addAudit("APPROVE", "Cheque #" + activeCheque.getChequeNumber() + " — PASSED in batch " + activeBatchId);
        activeCheque = checkerService.getChequeById(activeCheque.getChequeId());
        populateChequeDetail();
        refreshChequeListCounts();
    }

    @Listen("onClick = #btnApproveNext")
    public void onApproveNext() {
        String remarks = txtCheckerRemarks.getValue().trim();
        checkerService.approveCheque(activeCheque.getChequeId(), remarks);
        addAudit("APPROVE", "Cheque #" + activeCheque.getChequeNumber() + " — PASSED in batch " + activeBatchId);
        refreshChequeListCounts();
        List<CheckerCheque> batch = checkerService.getChequesByBatchId(activeBatchId);
        int idx = findIndex(batch, activeCheque.getChequeId());
        if (idx < batch.size() - 1) {
            showChequeDetailView(batch.get(idx + 1));
        } else {
            showChequeListView(activeBatchId);
        }
    }

    @Listen("onClick = #btnReject")
    public void onReject() {
        String remarks = txtCheckerRemarks.getValue().trim();
        try {
            checkerService.rejectCheque(activeCheque.getChequeId(), remarks);
            addAudit("REJECT",
                "Cheque #" + activeCheque.getChequeNumber() + " — Exception flagged. Remarks: " + remarks);
            activeCheque = checkerService.getChequeById(activeCheque.getChequeId());
            populateChequeDetail();
            refreshChequeListCounts();
        } catch (IllegalArgumentException ex) {
            Messagebox.show(ex.getMessage(), "Required", Messagebox.OK, Messagebox.EXCLAMATION);
        }
    }

    @Listen("onClick = #btnReturnFile")
    public void onReturnFile() {
        String remarks = txtCheckerRemarks.getValue().trim();
        if (remarks.isEmpty()) {
            Messagebox.show("Please enter remarks before returning the file.",
                "Remarks Required", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        checkerService.rejectCheque(activeCheque.getChequeId(), "Return File Code 28+: " + remarks);
        addAudit("REJECT",
            "Cheque #" + activeCheque.getChequeNumber() + " — Return File (Code 28+). Remarks: " + remarks);
        activeCheque = checkerService.getChequeById(activeCheque.getChequeId());
        populateChequeDetail();
        refreshChequeListCounts();
    }

    @Listen("onClick = #btnEscalate")
    public void onEscalate() {
        String remarks = txtCheckerRemarks.getValue().trim();
        String note = remarks.isEmpty() ? "Escalated to supervisor for review." : "Escalated: " + remarks;
        addAudit("SYSTEM", "Cheque #" + activeCheque.getChequeNumber() + " — " + note);
        Messagebox.show(
            "Cheque #" + activeCheque.getChequeNumber() + " has been escalated for supervisor review.",
            "Escalated", Messagebox.OK, Messagebox.INFORMATION);
    }

    // ================================================================
    // Utilities
    // ================================================================
    private int findIndex(List<CheckerCheque> list, String chequeId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getChequeId().equals(chequeId)) return i;
        }
        return -1;
    }

    private void refreshChequeListCounts() {
        if (activeBatchId == null) return;
        List<CheckerCheque> cheques = checkerService.getChequesByBatchId(activeBatchId);
        long approved = 0, rejected = 0, pending = 0;
        for (CheckerCheque c : cheques) {
            switch (safe(c.getCheckerStatus())) {
                case "approved": approved++; break;
                case "rejected": rejected++; break;
                default:         pending++;  break;
            }
        }
        lblApprovedCount.setValue(String.valueOf(approved));
        lblRejectedCount.setValue(String.valueOf(rejected));
        lblPendingCount.setValue(String.valueOf(pending));
        refreshBottomBar();
    }

    private boolean isBatchApproved(String batchId) {
        for (CheckerBatch b : checkerService.getAllCheckerBatches()) {
            if (b.getBatchId().equals(batchId)) {
                return "APPROVED".equals(b.getStatus());
            }
        }
        return false;
    }

    private String formatAmount(long amount) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("en", "IN"));
        return "\u20B9" + nf.format(amount);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String statusLabel(String s) {
        if ("approved".equals(s)) return "✓ Approved";
        if ("rejected".equals(s)) return "✗ Flagged";
        return "⏳ Pending";
    }

    private String statusSclass(String s) {
        if ("approved".equals(s)) return "cts-badge cts-badge-green";
        if ("rejected".equals(s)) return "cts-badge cts-badge-red";
        return "cts-badge cts-badge-orange";
    }

    /**
     * Updates the "Audit Log (N)" label in checkerSidebar.zul via JavaScript.
     * @Wire cannot cross composer boundaries, so we use JS DOM update instead.
     */
    private void updateSidebarAuditCount() {
        String js = "var el = document.querySelector('.cts-sidebar #lblAuditNav span');"
                  + "if(el) el.textContent = 'Audit Log (" + auditLog.size() + ")';";
        org.zkoss.zk.ui.util.Clients.evalJavaScript(js);
    }

    /**
     * Highlights the active nav item in the sidebar.
     * Uses Selectors.find() to cross the <include> boundary.
     */
    private void setNavActive(String activeId) {
        String[] ids = { "navDashboard", "navBatchMgmt", "navMicrRepair", "navChecker", "navReports", "navAudit" };
        for (String id : ids) {
            java.util.List<org.zkoss.zk.ui.Component> found =
                org.zkoss.zk.ui.select.Selectors.find(getSelf().getPage(), "#" + id);
            if (found != null && !found.isEmpty()) {
                Component c = found.get(0);
                if (c instanceof HtmlBasedComponent) {
                    ((HtmlBasedComponent) c).setSclass(
                        "cts-nav-item" + (id.equals(activeId) ? " active" : "")
                    );
                }
            }
        }
    }
}