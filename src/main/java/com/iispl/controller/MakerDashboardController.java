package com.iispl.controller;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;

import com.iispl.entity.OutwardCheque;
import com.iispl.dto.UserModel;
import com.iispl.service.MakerDashboardService;

/**
 * MakerDashboardController.java
 * ZK MVC Composer — controls maker/makerDashboard.zul
 *
 * Package  : com.iispl.controller
 * Pattern  : ZK MVC (SelectorComposer)
 * Role     : Maker (Data Entry)
 * Data     : In-memory — backed by MakerDashboardService
 */
public class MakerDashboardController extends SelectorComposer<Component> {

    // ── Section panels ───────────────────────────────────────────────
    @Wire("#sectionDashboard")         private Div sectionDashboard;
    @Wire("#sectionCreateBatch")       private Div sectionCreateBatch;
    @Wire("#sectionLoadCheques")       private Div sectionLoadCheques;
    @Wire("#sectionMakerQueue")        private Div sectionMakerQueue;
    @Wire("#sectionMakerBatchDetail")  private Div sectionMakerBatchDetail;
    @Wire("#sectionMakerChequeDetail") private Div sectionMakerChequeDetail;
    @Wire("#sectionMicrRepair")        private Div sectionMicrRepair;
    @Wire("#sectionReports")           private Div sectionReports;

    // ── Dashboard labels ─────────────────────────────────────────────
    @Wire("#welcomeUserName")          private Label welcomeUserName;
    @Wire("#welcomeDate")              private Label welcomeDate;
    @Wire("#statTotalBatches")         private Label statTotalBatches;
    @Wire("#statTotalCheques")         private Label statTotalCheques;
    @Wire("#statPendingMaker")         private Label statPendingMaker;
    @Wire("#statSubmittedChecker")     private Label statSubmittedChecker;
    @Wire("#dashBatchSummaryContent")  private Div   dashBatchSummaryContent;

    // ── Timeline dots ────────────────────────────────────────────────
    @Wire("#tlDot1")  private Div   tlDot1;
    @Wire("#tlDot2")  private Div   tlDot2;
    @Wire("#tlDot3")  private Div   tlDot3;
    @Wire("#tlDot4")  private Div   tlDot4;
    @Wire("#tlTime1") private Label tlTime1;
    @Wire("#tlTime2") private Label tlTime2;
    @Wire("#tlTime3") private Label tlTime3;
    @Wire("#tlTime4") private Label tlTime4;

    // ── Create Batch ─────────────────────────────────────────────────
    @Wire("#txtBatchId")     private Textbox txtBatchId;
    @Wire("#batchCount")     private Label   batchCount;
    @Wire("#batchTableBody") private Div     batchTableBody;

    // ── Load Cheques ─────────────────────────────────────────────────
    @Wire("#lstSelectBatch")        private Listbox lstSelectBatch;
    @Wire("#btnMoveToMaker")        private org.zkoss.zul.Button btnMoveToMaker;
    @Wire("#uploadResultAlert")     private Div   uploadResultAlert;
    @Wire("#uploadResultText")      private Div   uploadResultText;
    @Wire("#loadedChequeCount")     private Label loadedChequeCount;
    @Wire("#loadedChequeTableBody") private Div   loadedChequeTableBody;

    // ── Maker Queue ──────────────────────────────────────────────────
    @Wire("#makerQueueEmptyAlert") private Div   makerQueueEmptyAlert;
    @Wire("#makerBatchListCard")   private Div   makerBatchListCard;
    @Wire("#makerBatchCount")      private Label makerBatchCount;
    @Wire("#makerBatchTableBody")  private Div   makerBatchTableBody;

    // ── Maker Batch Detail ───────────────────────────────────────────
    @Wire("#batchDetailBreadcrumb")   private Label batchDetailBreadcrumb;
    @Wire("#batchDetailTitle")        private Label batchDetailTitle;
    @Wire("#batchDetailId")           private Label batchDetailId;
    @Wire("#batchDetailTotal")        private Label batchDetailTotal;
    @Wire("#batchDetailAmount")       private Label batchDetailAmount;
    @Wire("#batchDetailIqaFail")      private Label batchDetailIqaFail;
    @Wire("#batchStatTotal")          private Label batchStatTotal;
    @Wire("#batchStatDone")           private Label batchStatDone;
    @Wire("#batchStatPending")        private Label batchStatPending;
    @Wire("#batchStatStatus")         private Label batchStatStatus;
    @Wire("#batchSubmitToCheckerBar") private Div   batchSubmitToCheckerBar;
    @Wire("#makerChequeTableBody")    private Div   makerChequeTableBody;
    @Wire("#txtMakerSearch")          private Textbox txtMakerSearch;

    // ── Cheque Detail ────────────────────────────────────────────────
    @Wire("#chequeDetailBatchCrumb") private Label chequeDetailBatchCrumb;
    @Wire("#chequeDetailNumCrumb")   private Label chequeDetailNumCrumb;
    @Wire("#chequeNavMeta")          private Label chequeNavMeta;
    @Wire("#btnPrevCheque")          private org.zkoss.zul.Button btnPrevCheque;
    @Wire("#btnNextCheque")          private org.zkoss.zul.Button btnNextCheque;

    // Alerts
    @Wire("#alertIqaFail")      private Div   alertIqaFail;
    @Wire("#alertIqaPass")      private Div   alertIqaPass;
    @Wire("#alertHighValue")    private Div   alertHighValue;
    @Wire("#alertStale")        private Div   alertStale;
    @Wire("#alertBounced")      private Div   alertBounced;
    @Wire("#iqaRemarksText")    private Label iqaRemarksText;
    @Wire("#hvAmountLabel")     private Label hvAmountLabel;
    @Wire("#staleDateLabel")    private Label staleDateLabel;
    @Wire("#bounceReasonLabel") private Label bounceReasonLabel;

    // Cheque image fields
    @Wire("#frontImgLabel")    private Label frontImgLabel;
    @Wire("#cdBank")           private Label cdBank;
    @Wire("#cdNum")            private Label cdNum;
    @Wire("#cdPayee")          private Label cdPayee;
    @Wire("#cdAmtWords")       private Label cdAmtWords;
    @Wire("#cdDate")           private Label cdDate;
    @Wire("#cdAmount")         private Label cdAmount;
    @Wire("#cdSig")            private Label cdSig;
    @Wire("#cdMicr")           private Label cdMicr;
    @Wire("#cdDepositorAcc")   private Label cdDepositorAcc;
    @Wire("#iqaFailCorner")      private Div   iqaFailCorner;
    @Wire("#iqaFailCornerLabel") private Div   iqaFailCornerLabel;

    // Form fields (read-only)
    @Wire("#mfTransactionId") private Textbox mfTransactionId;
    @Wire("#mfChequeNumber")  private Textbox mfChequeNumber;
    @Wire("#mfMicrCode")      private Textbox mfMicrCode;
    @Wire("#mfIfscCode")      private Textbox mfIfscCode;
    @Wire("#mfBankName")      private Textbox mfBankName;
    @Wire("#mfBranchName")    private Textbox mfBranchName;
    @Wire("#mfDrawerAccNo")   private Textbox mfDrawerAccNo;

    // Form fields (maker editable)
    @Wire("#mfAmtFig")       private Textbox mfAmtFig;
    @Wire("#mfAmtWordsHint") private Label   mfAmtWordsHint;
    @Wire("#mfChequeDate")   private org.zkoss.zul.Datebox mfChequeDate;
    @Wire("#mfPayeeName")    private Textbox mfPayeeName;
    @Wire("#mfDepositorAcc") private Textbox mfDepositorAcc;
    @Wire("#mfFlag")         private Listbox mfFlag;
    @Wire("#mfRemarks")      private Textbox mfRemarks;

    // Action bar
    @Wire("#chequeActionBar")      private Div   chequeActionBar;
    @Wire("#chequeStatusBadge")    private Label chequeStatusBadge;
    @Wire("#chequeSubmittedAlert") private Div   chequeSubmittedAlert;
    @Wire("#btnSaveAndNext")       private org.zkoss.zul.Button btnSaveAndNext;

    // Reports
    @Wire("#reportContent") private Div reportContent;

    // ── State ────────────────────────────────────────────────────────
    private UserModel            currentUser;
    private MakerDashboardService service;
    private String               activeBatchId;
    private String               activeChequeId;

    // Date formatter for LocalDate display
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    // ════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ════════════════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        currentUser = (UserModel) Sessions.getCurrent().getAttribute("loggedInUser");
        if (currentUser == null) {
            Executions.sendRedirect("/zul/login.zul");
            return;
        }
        service = new MakerDashboardService();
        initDashboard();
    }

    // ════════════════════════════════════════════════════════════════
    // DASHBOARD INIT
    // ════════════════════════════════════════════════════════════════

    private void initDashboard() {
        welcomeUserName.setValue(currentUser.getRoleLabel());
        welcomeDate.setValue("Clearing Date: "
            + new SimpleDateFormat("dd-MMM-yyyy").format(new Date()));
        refreshStats();
        renderDashboardTimeline();
        renderDashboardBatchSummary();
    }

    private void refreshStats() {
        statTotalBatches.setValue(String.valueOf(service.getTotalBatches()));
        statTotalCheques.setValue(String.valueOf(service.getTotalCheques()));
        statPendingMaker.setValue(String.valueOf(service.getPendingMakerCount()));
        statSubmittedChecker.setValue(String.valueOf(service.getSubmittedToCheckerCount()));
    }

    private void renderDashboardTimeline() {
        setTimelineDot(tlDot1, tlTime1, service.isStep1Done(), "Batch created today");
        setTimelineDot(tlDot2, tlTime2, service.isStep2Done(),
            service.getTotalCheques() + " cheques loaded");
        setTimelineDot(tlDot3, tlTime3, false, "—");
        setTimelineDot(tlDot4, tlTime4, service.isStep4Active(),
            service.getSubmittedToCheckerCount() + " submitted, "
            + service.getPendingMakerCount() + " pending");
    }

    private void setTimelineDot(Div dot, Label label, boolean done, String text) {
        dot.setSclass("cts-tl-dot " + (done ? "done" : "pending"));
        label.setValue(text);
    }

    private void renderDashboardBatchSummary() {
        List<String[]> batches = service.getBatchSummaryRows();
        if (batches.isEmpty()) {
            dashBatchSummaryContent.setVisible(true);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"cts-table-wrap\"><table class=\"cts-table\">")
          .append("<thead><tr><th>Batch ID</th><th>Cheques</th><th>Done</th><th>Status</th></tr></thead><tbody>");
        for (String[] row : batches) {
            sb.append("<tr>")
              .append("<td class=\"mono\">").append(esc(row[0])).append("</td>")
              .append("<td>").append(esc(row[1])).append("</td>")
              .append("<td style=\"color:#16a34a;font-weight:600;\">").append(esc(row[2])).append("</td>")
              .append("<td>").append(badgeHtml(row[3])).append("</td>")
              .append("</tr>");
        }
        sb.append("</tbody></table></div>");
        replaceHtml(dashBatchSummaryContent, sb.toString());
    }

    // ════════════════════════════════════════════════════════════════
    // NAVIGATION EVENTS
    // ════════════════════════════════════════════════════════════════

    @Listen("onNavDashboard = #sectionDashboard, #sectionCreateBatch, "
          + "#sectionLoadCheques, #sectionMakerQueue, #sectionMakerBatchDetail, "
          + "#sectionMakerChequeDetail, #sectionMicrRepair, #sectionReports")
    public void onNavDashboard(Event e) {
        refreshStats(); renderDashboardTimeline(); renderDashboardBatchSummary();
        showSection(sectionDashboard);
    }

    @Listen("onNavCreateBatch = #sectionDashboard, #sectionCreateBatch, "
          + "#sectionLoadCheques, #sectionMakerQueue, #sectionMakerBatchDetail, "
          + "#sectionMakerChequeDetail, #sectionMicrRepair, #sectionReports")
    public void onNavCreateBatch(Event e) {
        populateCreateBatchSection();
        showSection(sectionCreateBatch);
    }

    @Listen("onNavLoadCheques = #sectionDashboard, #sectionCreateBatch, "
          + "#sectionLoadCheques, #sectionMakerQueue, #sectionMakerBatchDetail, "
          + "#sectionMakerChequeDetail, #sectionMicrRepair, #sectionReports")
    public void onNavLoadCheques(Event e) {
        populateLoadChequesSection();
        showSection(sectionLoadCheques);
    }

    @Listen("onNavMakerQueue = #sectionDashboard, #sectionCreateBatch, "
          + "#sectionLoadCheques, #sectionMakerQueue, #sectionMakerBatchDetail, "
          + "#sectionMakerChequeDetail, #sectionMicrRepair, #sectionReports")
    public void onNavMakerQueue(Event e) {
        activeBatchId = null; activeChequeId = null;
        populateMakerBatchList();
        showSection(sectionMakerQueue);
    }

    @Listen("onNavMicrRepair = #sectionDashboard, #sectionCreateBatch, "
          + "#sectionLoadCheques, #sectionMakerQueue, #sectionMakerBatchDetail, "
          + "#sectionMakerChequeDetail, #sectionMicrRepair, #sectionReports")
    public void onNavMicrRepair(Event e) {
        populateMicrRepairSection();
        showSection(sectionMicrRepair);
    }

    @Listen("onNavReports = #sectionDashboard, #sectionCreateBatch, "
          + "#sectionLoadCheques, #sectionMakerQueue, #sectionMakerBatchDetail, "
          + "#sectionMakerChequeDetail, #sectionMicrRepair, #sectionReports")
    public void onNavReports(Event e) { showSection(sectionReports); }

    // ════════════════════════════════════════════════════════════════
    // CREATE BATCH
    // ════════════════════════════════════════════════════════════════

    private void populateCreateBatchSection() {
        txtBatchId.setValue(service.generateNextBatchId());
        renderBatchTable();
    }

    @Listen("onClick = #btnCreateBatch")
    public void onCreateBatch(Event e) {
        String batchId = txtBatchId.getValue();
        String session = getListboxValue(
            (Listbox) getSelf().getFellowIfAny("lstSession"), "MORNING");
        String route   = getListboxValue(
            (Listbox) getSelf().getFellowIfAny("lstRoute"), "MAKER_CHECKER");
        service.createBatch(batchId, session, route);
        renderBatchTable();
        refreshStats();
        showToast("Batch " + batchId + " created.", "success");
        txtBatchId.setValue(service.generateNextBatchId());
    }

    @Listen("onClick = #btnClearBatchForm")
    public void onClearBatchForm(Event e) {
        txtBatchId.setValue(service.generateNextBatchId());
    }

    private void renderBatchTable() {
        List<String[]> rows = service.getBatchTableRows();
        batchCount.setValue(rows.size() + " batch" + (rows.size() == 1 ? "" : "es"));
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String[] r : rows) {
            sb.append("<tr>")
              .append("<td style=\"color:#ADADAD;font-size:11px;\">").append(i++).append("</td>")
              .append("<td><span class=\"mono\" style=\"color:#2563c8;font-weight:700;\">").append(esc(r[0])).append("</span></td>")
              .append("<td>").append(esc(r[1])).append("</td>")
              .append("<td>").append(esc(r[2])).append("</td>")
              .append("<td>").append(esc(r[3])).append("</td>")
              .append("<td>").append(esc(r[4])).append("</td>")
              .append("<td>").append(badgeHtml(r[5])).append("</td>")
              .append("<td><button class=\"cts-btn cts-btn-outline cts-btn-sm\">Load Cheques →</button></td>")
              .append("</tr>");
        }
        replaceHtml(batchTableBody, sb.toString());
    }

    // ════════════════════════════════════════════════════════════════
    // LOAD CHEQUES
    // ════════════════════════════════════════════════════════════════

    private void populateLoadChequesSection() {
        lstSelectBatch.getItems().clear();
        Listitem blank = new Listitem("— Select a batch —", "");
        lstSelectBatch.appendChild(blank);
        for (String[] b : service.getBatchTableRows()) {
            lstSelectBatch.appendChild(new Listitem(b[0] + " (" + b[2] + ")", b[0]));
        }
        uploadResultAlert.setVisible(false);
        btnMoveToMaker.setDisabled(true);
        renderLoadedChequeTable();
    }

    @Listen("onUpload = #xmlUpload")
    public void onXmlUpload(org.zkoss.zk.ui.event.UploadEvent e) {
        org.zkoss.util.media.Media media = e.getMedia();
        if (media == null) { showToast("No file received.", "error"); return; }
        String batchId = selectedBatchId();
        if (batchId.isEmpty()) { showToast("Please select a batch first.", "warning"); return; }

        int count = service.loadChequesFromXml(batchId, media);
        uploadResultAlert.setVisible(true);
        replaceHtml(uploadResultText,
            "✅ " + count + " cheques loaded from '" + esc(media.getName())
            + "' into batch " + esc(batchId) + ".");
        btnMoveToMaker.setDisabled(false);
        renderLoadedChequeTable();
        refreshStats();
    }

    @Listen("onClick = #btnMoveToMaker")
    public void onMoveToMaker(Event e) {
        String batchId = selectedBatchId();
        if (batchId.isEmpty()) { showToast("Select a batch first.", "warning"); return; }
        int moved = service.moveChequesToMakerQueue(batchId);
        showToast(moved + " IQA-passed cheques moved to Maker Queue.", "success");
        refreshStats();
        btnMoveToMaker.setDisabled(true);
    }

    private void renderLoadedChequeTable() {
        List<OutwardCheque> list = service.getAllLoadedCheques();
        loadedChequeCount.setValue(list.size() + " cheques");
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (OutwardCheque c : list) {
            sb.append("<tr>")
              .append("<td style=\"color:#ADADAD;font-size:11px;\">").append(i++).append("</td>")
              .append("<td class=\"mono\">").append(esc(c.getChequeNumber())).append("</td>")
              .append("<td style=\"font-size:12px;\">").append(esc(c.getBankName())).append("</td>")
              .append("<td>").append(esc(c.getDrawerName())).append("</td>")
              .append("<td class=\"mono\" style=\"font-weight:700;color:#2563c8;\">")
                .append(fmtAmt(c.getAmountInFigures() != null ? c.getAmountInFigures() : 0L)).append("</td>")
              .append("<td>").append(formatDate(c.getChequeDate())).append("</td>")
              .append("<td>").append(iqaBadge(c.getIqaStatus())).append("</td>")
              .append("</tr>");
        }
        replaceHtml(loadedChequeTableBody, sb.toString());
    }

    private String selectedBatchId() {
        if (lstSelectBatch.getSelectedItem() == null) return "";
        Object v = lstSelectBatch.getSelectedItem().getValue();
        return v != null ? v.toString() : "";
    }

    // ════════════════════════════════════════════════════════════════
    // MAKER QUEUE — BATCH LIST
    // ════════════════════════════════════════════════════════════════

    private void populateMakerBatchList() {
        List<String[]> batches = service.getMakerBatchRows();
        if (batches.isEmpty()) {
            makerQueueEmptyAlert.setVisible(true);
            makerBatchListCard.setVisible(false);
        } else {
            makerQueueEmptyAlert.setVisible(false);
            makerBatchListCard.setVisible(true);
            makerBatchCount.setValue(batches.size() + " batch" + (batches.size() == 1 ? "" : "es"));
            StringBuilder sb = new StringBuilder();
            int i = 1;
            for (String[] r : batches) {
                int total = parseInt(r[1]);
                int done  = parseInt(r[6]);
                int pct   = total > 0 ? (done * 100 / total) : 0;
                sb.append("<tr class=\"cts-tr-clickable\">")
                  .append("<td style=\"color:#ADADAD;font-size:11px;\">").append(i++).append("</td>")
                  .append("<td><span class=\"mono\" style=\"color:#2563c8;font-weight:700;\">").append(esc(r[0])).append("</span></td>")
                  .append("<td>").append(r[1]).append("</td>")
                  .append("<td class=\"mono\">").append(esc(r[2])).append("</td>")
                  .append("<td><span style=\"color:#16a34a;font-weight:600;\">").append(r[3]).append(" PASS</span>")
                    .append(" / <span style=\"color:#dc2626;font-weight:600;\">").append(r[4]).append(" FAIL</span></td>")
                  .append("<td>").append(badgeHtml(r[5])).append("</td>")
                  .append("<td>")
                    .append("<div style=\"font-size:12px;\">").append(done).append("/").append(total).append(" done</div>")
                    .append("<div class=\"cts-progress-track\"><div class=\"cts-progress-fill\" style=\"width:").append(pct).append("%;\"></div></div>")
                  .append("</td>")
                  .append("<td>");
                if ("CHECKER_PENDING".equals(r[5]) || "APPROVED".equals(r[5])) {
                    sb.append("<span style=\"color:#16a34a;font-size:12px;\">✓ Submitted to Checker</span>");
                } else {
                    sb.append("<button class=\"cts-btn cts-btn-primary cts-btn-sm\" ")
                      .append("onClick=\"zk.Widget.$('$makerDashboard').fire('onOpenMakerBatch','").append(r[0]).append("')\">")
                      .append("Open Batch →</button>");
                }
                sb.append("</td></tr>");
            }
            replaceHtml(makerBatchTableBody, sb.toString());
        }
    }

    @Listen("onOpenMakerBatch = div#sectionMakerQueue")
    public void onOpenMakerBatch(Event e) {
        activeBatchId = String.valueOf(e.getData());
        service.loadBatchIntoMakerQueue(activeBatchId);
        populateMakerBatchDetailSection();
        showSection(sectionMakerBatchDetail);
    }

    // ════════════════════════════════════════════════════════════════
    // MAKER BATCH DETAIL
    // ════════════════════════════════════════════════════════════════

    private void populateMakerBatchDetailSection() {
        String[] meta = service.getBatchMeta(activeBatchId);
        if (meta == null) return;
        batchDetailBreadcrumb.setValue(meta[0]);
        batchDetailTitle.setValue(meta[0]);
        batchDetailId.setValue(meta[0]);
        batchDetailTotal.setValue(meta[1]);
        batchDetailAmount.setValue(meta[2]);
        batchDetailIqaFail.setValue(meta[3]);
        refreshBatchDetailStats();
        renderBatchChequeTable("");
    }

    private void refreshBatchDetailStats() {
        int[] counts = service.getBatchChequeCounts(activeBatchId);
        batchStatTotal.setValue(String.valueOf(counts[0]));
        batchStatDone.setValue(String.valueOf(counts[1]));
        batchStatPending.setValue(String.valueOf(counts[2]));
        batchStatStatus.setValue(service.getBatchStatus(activeBatchId).replace("_", " "));
        batchSubmitToCheckerBar.setVisible(counts[1] > 0);
    }

    @Listen("onChanging = #txtMakerSearch")
    public void onMakerSearch(Event e) {
        renderBatchChequeTable(txtMakerSearch.getValue());
    }

    private void renderBatchChequeTable(String search) {
        List<OutwardCheque> list = service.getChequesForBatch(activeBatchId, search);
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (OutwardCheque c : list) {
            boolean isDone  = "done".equals(c.getMakerStatus());
            long    amt     = c.getAmountInFigures() != null ? c.getAmountInFigures() : 0L;
            boolean isHV    = amt >= 500_000;
            boolean iqaFail = "FAIL".equals(c.getIqaStatus());

            sb.append("<tr class=\"cts-tr-clickable\">")
              .append("<td style=\"color:#ADADAD;font-size:11px;\">").append(i++).append("</td>")
              .append("<td class=\"mono\">").append(esc(c.getChequeNumber()))
                .append(iqaFail ? "<br/><span class=\"cts-badge cts-badge-orange\" style=\"font-size:10px;\">⚠ IQA FAIL</span>" : "")
              .append("</td>")
              .append("<td style=\"font-size:12px;\">").append(esc(c.getBankName())).append("</td>")
              .append("<td>").append(esc(c.getDrawerName())).append("</td>")
              .append("<td class=\"mono\" style=\"font-weight:700;color:#2563c8;\">")
                .append(fmtAmt(amt))
                .append(isHV ? "<br/><span class=\"cts-badge cts-badge-yellow\" style=\"font-size:10px;\">⚠ HV</span>" : "")
              .append("</td>")
              .append("<td>").append(formatDate(c.getChequeDate())).append("</td>")
              .append("<td>").append(isDone
                    ? "<span class=\"cts-badge cts-badge-green\">✓ Submitted</span>"
                    : "<span class=\"cts-badge cts-badge-yellow\">Pending</span>")
              .append("</td>")
              .append("<td><button class=\"cts-btn cts-btn-outline cts-btn-sm\" ")
              // We use getChequeId() — the unique ID field on OutwardCheque
              .append("onClick=\"zk.Widget.$('$makerDashboard').fire('onOpenMakerCheque','").append(c.getChequeId()).append("')\">")
              .append("Open ›</button></td>")
              .append("</tr>");
        }
        replaceHtml(makerChequeTableBody, sb.toString());
    }

    @Listen("onBackToMakerBatchList = #sectionMakerBatchDetail")
    public void onBackToMakerBatchList(Event e) {
        activeBatchId = null;
        populateMakerBatchList();
        showSection(sectionMakerQueue);
    }

    @Listen("onClick = #btnSubmitBatchToChecker")
    public void onSubmitBatchToChecker(Event e) {
        int count = service.submitBatchToChecker(activeBatchId);
        if (count < 0) { showToast("No submitted cheques in this batch yet.", "warning"); return; }
        showToast("Batch " + activeBatchId + " submitted to Checker (" + count + " cheques).", "success");
        refreshStats();
        activeBatchId = null;
        populateMakerBatchList();
        showSection(sectionMakerQueue);
    }

    // ════════════════════════════════════════════════════════════════
    // MAKER CHEQUE DETAIL
    // ════════════════════════════════════════════════════════════════

    @Listen("onOpenMakerCheque = div#sectionMakerBatchDetail, div#sectionMakerChequeDetail")
    public void onOpenMakerCheque(Event e) {
        activeChequeId = String.valueOf(e.getData());
        populateChequeDetailForm();
        showSection(sectionMakerChequeDetail);
    }

    private void populateChequeDetailForm() {
        OutwardCheque c = service.getChequeById(activeChequeId);
        if (c == null) return;

        boolean done      = "done".equals(c.getMakerStatus());
        boolean isIqaFail = "FAIL".equals(c.getIqaStatus());
        long    amt       = c.getAmountInFigures() != null ? c.getAmountInFigures() : 0L;
        boolean isHV      = amt >= 500_000;
        boolean isStale   = isStaleLocalDate(c.getChequeDate());

        // Breadcrumb — batchId is stored in makerUserId in in-memory mode
        chequeDetailBatchCrumb.setValue(c.getMakerUserId() != null ? c.getMakerUserId() : "—");
        chequeDetailNumCrumb.setValue(c.getChequeNumber());

        // Navigation
        int[] nav = service.getChequeNavIndex(activeBatchId, activeChequeId);
        chequeNavMeta.setValue("Cheque " + (nav[0] + 1) + " of " + nav[2] + " in batch");
        btnPrevCheque.setDisabled(nav[0] == 0);
        btnNextCheque.setDisabled(nav[0] >= nav[2] - 1);

        // Alerts
        alertIqaFail.setVisible(isIqaFail);
        alertIqaPass.setVisible(!isIqaFail);
        alertHighValue.setVisible(isHV);
        alertStale.setVisible(isStale);
        alertBounced.setVisible(false); // no bounced flag on OutwardCheque

        if (isIqaFail) iqaRemarksText.setValue(c.getMakerRemarks() != null ? c.getMakerRemarks() : "IQA failed");
        if (isHV)      hvAmountLabel.setValue("₹" + fmtAmt(amt));
        if (isStale)   staleDateLabel.setValue(formatDate(c.getChequeDate()));

        // Cheque image area
        frontImgLabel.setValue(isIqaFail
            ? "⚠ Front Image — IQA FAILED — " + c.getChequeNumber()
            : "Front Image — " + c.getChequeNumber());
        iqaFailCorner.setVisible(isIqaFail);
        iqaFailCornerLabel.setVisible(isIqaFail);
        cdBank.setValue(safe(c.getBankName()));
        cdNum.setValue("Cheque No: " + c.getChequeNumber() + " | Branch: " + safe(c.getBranchName()));
        cdPayee.setValue(c.getPayeeName() != null ? c.getPayeeName() : "[verify from physical]");
        cdAmtWords.setValue(c.getAmountInWords() != null ? c.getAmountInWords() : "[verify from physical]");
        cdDate.setValue(formatDate(c.getChequeDate()));
        cdAmount.setValue(amt > 0 ? "₹" + fmtAmt(amt) : "[verify]");
        cdSig.setValue(c.getDrawerName() != null ? c.getDrawerName().split(" ")[0] : "—");
        cdMicr.setValue(isIqaFail
            ? "|" + c.getChequeNumber() + "| ███████ |████████████|"
            : "|" + c.getChequeNumber() + "| " + safe(c.getMicrCode()) + "| " + safe(c.getDrawerAccountNumber()) + "|");
        cdDepositorAcc.setValue(safe(c.getDepositorAccountNumber()));

        // Read-only form fields
        mfTransactionId.setValue(safe(c.getTransactionId()));
        mfChequeNumber.setValue(safe(c.getChequeNumber()));
        mfMicrCode.setValue(safe(c.getMicrCode()));
        mfIfscCode.setValue(safe(c.getIfscCode()));
        mfBankName.setValue(safe(c.getBankName()));
        mfBranchName.setValue(safe(c.getBranchName()));
        mfDrawerAccNo.setValue(safe(c.getDrawerAccountNumber()));

        // Editable form fields
        mfAmtFig.setValue(amt > 0 ? String.valueOf(amt) : "");
        mfAmtWordsHint.setValue(c.getAmountInWords() != null ? c.getAmountInWords() : "—");
        mfPayeeName.setValue(c.getPayeeName() != null ? c.getPayeeName() : "");
        mfDepositorAcc.setValue(c.getDepositorAccountNumber() != null ? c.getDepositorAccountNumber() : "");
        mfRemarks.setValue(c.getMakerRemarks() != null ? c.getMakerRemarks() : "");

        // Lock fields if already submitted
        mfAmtFig.setReadonly(done);
        mfPayeeName.setReadonly(done);
        mfDepositorAcc.setReadonly(done);
        mfRemarks.setReadonly(done);
        mfFlag.setDisabled(done);

        // Action bar
        chequeActionBar.setVisible(!done);
        chequeSubmittedAlert.setVisible(done);
        chequeStatusBadge.setValue(done ? "✓ Submitted" : "Pending");
        chequeStatusBadge.setStyle(done
            ? "background:#dcfce7;color:#16a34a;"
            : "background:#fef9c3;color:#ca8a04;");
        btnSaveAndNext.setVisible(!done && nav[0] < nav[2] - 1);
    }

    @Listen("onBackToBatchDetail = #sectionMakerChequeDetail")
    public void onBackToBatchDetail(Event e) {
        activeChequeId = null;
        populateMakerBatchDetailSection();
        showSection(sectionMakerBatchDetail);
    }

    @Listen("onPrevCheque = #sectionMakerChequeDetail")
    public void onPrevCheque(Event e) {
        String prev = service.getPrevChequeId(activeBatchId, activeChequeId);
        if (prev != null) { activeChequeId = prev; populateChequeDetailForm(); }
    }

    @Listen("onNextCheque = #sectionMakerChequeDetail")
    public void onNextCheque(Event e) {
        String next = service.getNextChequeId(activeBatchId, activeChequeId);
        if (next != null) { activeChequeId = next; populateChequeDetailForm(); }
    }

    @Listen("onRemark1 = #sectionMakerChequeDetail")
    public void onRemark1(Event e) { mfRemarks.setValue("All fields verified. Data matches physical cheque."); }
    @Listen("onRemark2 = #sectionMakerChequeDetail")
    public void onRemark2(Event e) { mfRemarks.setValue("Amount in figures does not match amount in words. Flagged for checker review."); }
    @Listen("onRemark3 = #sectionMakerChequeDetail")
    public void onRemark3(Event e) { mfRemarks.setValue("Cheque date is older than 3 months. Marked as stale."); }
    @Listen("onRemark4 = #sectionMakerChequeDetail")
    public void onRemark4(Event e) { mfRemarks.setValue("IQA failed — all data entered manually from physical cheque."); }

    @Listen("onMakerSubmit = #sectionMakerChequeDetail")
    public void onMakerSubmit(Event e) {
        saveChequeFromForm();
        showToast("Cheque submitted to Checker.", "success");
        activeChequeId = null;
        populateMakerBatchDetailSection();
        showSection(sectionMakerBatchDetail);
    }

    @Listen("onMakerSaveAndNext = #sectionMakerChequeDetail")
    public void onMakerSaveAndNext(Event e) {
        String nextId = service.getNextChequeId(activeBatchId, activeChequeId);
        saveChequeFromForm();
        showToast("Cheque saved. Opening next...", "success");
        if (nextId != null) { activeChequeId = nextId; populateChequeDetailForm(); }
        else { activeChequeId = null; populateMakerBatchDetailSection(); showSection(sectionMakerBatchDetail); }
    }

    @Listen("onReturnCheque = #sectionMakerChequeDetail")
    public void onReturnCheque(Event e) {
        service.markChequeReturned(activeChequeId, mfRemarks.getValue());
        showToast("Cheque sent to Return.", "info");
        activeChequeId = null;
        populateMakerBatchDetailSection();
        showSection(sectionMakerBatchDetail);
    }

    private void saveChequeFromForm() {
        String amtStr = mfAmtFig.getValue();
        long   amt    = amtStr.isEmpty() ? 0 : Long.parseLong(amtStr.replace(",", "").trim());
        // flag is no longer stored separately — we put it in remarks prefix if needed
        String flag   = mfFlag.getSelectedItem() != null
                      ? (String) mfFlag.getSelectedItem().getValue() : "";
        String remarks = mfRemarks.getValue();
        if (!flag.isEmpty()) {
            remarks = "[" + flag + "] " + remarks;
        }
        service.saveMakerChequeData(activeChequeId,
            amt, mfPayeeName.getValue(), mfDepositorAcc.getValue(),
            flag, remarks);
    }

    // ════════════════════════════════════════════════════════════════
    // MICR REPAIR
    // ════════════════════════════════════════════════════════════════

    private void populateMicrRepairSection() {
        List<String[]> rows = service.getMicrRepairRows();
        Label countLbl = (Label) getSelf().getFellowIfAny("micrRepairCount");
        if (countLbl != null) countLbl.setValue(rows.size() + " items");
        Div tbody = (Div) getSelf().getFellowIfAny("micrRepairTableBody");
        if (tbody == null) return;
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String[] r : rows) {
            sb.append("<tr><td>").append(i++).append("</td>")
              .append("<td class=\"mono\">").append(esc(r[0])).append("</td>")
              .append("<td>").append(esc(r[1])).append("</td>")
              .append("<td class=\"mono\">").append(esc(r[2])).append("</td>")
              .append("<td style=\"color:#dc2626;\">").append(esc(r[3])).append("</td>")
              .append("<td><input style=\"padding:5px 8px;border:1.5px solid #E3E7EA;border-radius:5px;font-size:12px;width:130px;\" placeholder=\"Enter correct MICR...\"/></td>")
              .append("<td><button class=\"cts-btn cts-btn-primary cts-btn-sm\">Save</button></td>")
              .append("</tr>");
        }
        replaceHtml(tbody, sb.toString());
    }

    // ════════════════════════════════════════════════════════════════
    // REPORTS
    // ════════════════════════════════════════════════════════════════

    @Listen("onReportBatchSummary = #sectionReports")
    public void onReportBatchSummary(Event e) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3 style=\"font-size:14px;font-weight:700;color:#1a3a6e;margin-bottom:14px;\">📋 Batch Summary Report</h3>");
        sb.append("<div class=\"cts-table-wrap\"><table class=\"cts-table\">");
        sb.append("<thead><tr><th>Batch ID</th><th>Date</th><th>Cheques</th><th>Amount</th><th>Session</th><th>Status</th></tr></thead><tbody>");
        for (String[] r : service.getBatchTableRows()) {
            sb.append("<tr><td class=\"mono\">").append(esc(r[0])).append("</td>")
              .append("<td>").append(esc(r[1])).append("</td>")
              .append("<td>").append(esc(r[4])).append("</td>")
              .append("<td class=\"mono\">—</td>")
              .append("<td>").append(esc(r[2])).append("</td>")
              .append("<td>").append(badgeHtml(r[5])).append("</td></tr>");
        }
        sb.append("</tbody></table></div>");
        replaceHtml(reportContent, sb.toString());
    }

    @Listen("onReportMakerActivity = #sectionReports")
    public void onReportMakerActivity(Event e) {
        replaceHtml(reportContent,
            "<div class=\"cts-alert cts-alert-info\"><span class=\"cts-alert-icon\">ℹ️</span>"
          + "<div>Maker Activity — <strong>"
          + service.getSubmittedToCheckerCount()
          + "</strong> cheques submitted to Checker in this session.</div></div>");
    }

    @Listen("onReportIqa = #sectionReports")
    public void onReportIqa(Event e) {
        replaceHtml(reportContent,
            "<div class=\"cts-alert cts-alert-info\"><span class=\"cts-alert-icon\">ℹ️</span>"
          + "<div>IQA Report — "
          + service.getMicrRepairRows().size() + " cheques with IQA FAIL.</div></div>");
    }

    // ════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════

    private void showSection(Div target) {
        Div[] all = {
            sectionDashboard, sectionCreateBatch, sectionLoadCheques,
            sectionMakerQueue, sectionMakerBatchDetail, sectionMakerChequeDetail,
            sectionMicrRepair, sectionReports
        };
        for (Div d : all) {
            if (d != null) d.setSclass("cts-section" + (d == target ? " active" : ""));
        }
    }

    private void showToast(String msg, String type) {
        String icon = "success".equals(type) ? "✅" : "warning".equals(type) ? "⚠️"
                    : "error".equals(type)   ? "❌" : "ℹ️";
        String js =
            "var t=document.createElement('div');"
          + "t.className='cts-toast cts-toast-" + type + "';"
          + "t.innerHTML='" + icon + " " + esc(msg).replace("'", "\\'") + "';"
          + "var c=document.getElementById('toastContainer');"
          + "if(!c){c=document.createElement('div');c.id='toastContainer';"
          + "c.className='cts-toast-container';document.body.appendChild(c);}"
          + "c.appendChild(t);setTimeout(function(){t.remove();},3200);";
        org.zkoss.zk.ui.util.Clients.evalJavaScript(js);
    }

    private void replaceHtml(Div container, String html) {
        if (container == null) return;
        container.getChildren().clear();
        if (html != null && !html.isEmpty())
            container.appendChild(new org.zkoss.zul.Html(html));
    }

    // Formats LocalDate to dd-MMM-yyyy string for display
    private String formatDate(LocalDate date) {
        if (date == null) return "—";
        return date.format(DATE_FMT);
    }

    // Checks if a LocalDate is older than 90 days (stale cheque)
    private boolean isStaleLocalDate(LocalDate date) {
        if (date == null) return false;
        return date.isBefore(LocalDate.now().minusDays(90));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String fmtAmt(long amt) {
        return NumberFormat.getNumberInstance(new Locale("en", "IN")).format(amt);
    }

    private static String badgeHtml(String status) {
        if (status == null) return "—";
        String cls = "cts-badge-gray";
        if (status.contains("PROGRESS") || status.contains("PENDING")) cls = "cts-badge-orange";
        else if (status.contains("APPROVED") || status.contains("DONE")
              || status.contains("DISPATCHED") || status.contains("CHECKER")) cls = "cts-badge-green";
        else if (status.contains("REJECT") || status.contains("FAIL")) cls = "cts-badge-red";
        return "<span class=\"cts-badge " + cls + "\">" + esc(status.replace("_", " ")) + "</span>";
    }

    private static String iqaBadge(String status) {
        return "PASS".equals(status)
            ? "<span class=\"cts-badge cts-badge-green\">PASS</span>"
            : "<span class=\"cts-badge cts-badge-red\">FAIL</span>";
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception ex) { return 0; }
    }

    private static String getListboxValue(Listbox lb, String def) {
        if (lb == null || lb.getSelectedItem() == null) return def;
        Object v = lb.getSelectedItem().getValue();
        return v != null ? v.toString() : def;
    }
}