package com.iispl.controller;

import com.iispl.entity.MicrRepairEntry;
import com.iispl.entity.OutwardBatch;
import com.iispl.entity.UserModel;
import com.iispl.service.MicrRepairService;
import com.iispl.serviceImpl.MicrRepairServiceImpl;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/*
 * MicrRepairController.java
 * Package  : com.iispl.controller
 * Controls : micrRepairDashboard.zul
 *
 * 4 Panels:
 *   panelBatchList    - landing page — all MICR repair batches with stats
 *   panelEntryList    - entries inside a batch + checker verification table
 *   panelRepairForm   - operator fills corrected MICR data (Panel 3)
 *   panelCheckerPanel - checker sees original vs corrected, approves or rejects (Panel 4)
 */
public class MicrRepairController extends SelectorComposer<Component> {

    private final MicrRepairService micrRepairService = new MicrRepairServiceImpl();

    // ── Session state ────────────────────────────────────────────────
    private UserModel   currentUser;
    private OutwardBatch activeBatch;
    private MicrRepairEntry activeEntry;     // entry open in repair form
    private MicrRepairEntry checkerEntry;    // entry open in checker panel

    // ── Date formatter for display ───────────────────────────────────
    private static final DateTimeFormatter DISPLAY_FMT =
        DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");


    // ════════════════════════════════════════════════════════════════
    // Wired Panels
    // ════════════════════════════════════════════════════════════════

    @Wire("#panelBatchList")    private Div panelBatchList;
    @Wire("#panelEntryList")    private Div panelEntryList;
    @Wire("#panelRepairForm")   private Div panelRepairForm;
    @Wire("#panelCheckerPanel") private Div panelCheckerPanel;


    // ════════════════════════════════════════════════════════════════
    // Panel 1 — Batch List wires
    // ════════════════════════════════════════════════════════════════

    @Wire("#lblTotalBatches")     private Label lblTotalBatches;
    @Wire("#lblPendingBatches")   private Label lblPendingBatches;
    @Wire("#lblCompletedBatches") private Label lblCompletedBatches;
    @Wire("#lblBatchCount")       private Label lblBatchCount;
    @Wire("#gridBatchList")       private Grid  gridBatchList;


    // ════════════════════════════════════════════════════════════════
    // Panel 2 — Entry List wires
    // ════════════════════════════════════════════════════════════════

    @Wire("#lblActiveBatchId")   private Label lblActiveBatchId;
    @Wire("#lblTotalEntries")    private Label lblTotalEntries;
    @Wire("#lblPendingEntries")  private Label lblPendingEntries;
    @Wire("#lblRepairedEntries") private Label lblRepairedEntries;
    @Wire("#lblFlaggedEntries")  private Label lblFlaggedEntries;
    @Wire("#lblPendingBadge")    private Label lblPendingBadge;
    @Wire("#lblCheckerBadge")    private Label lblCheckerBadge;
    @Wire("#gridEntryList")      private Grid  gridEntryList;
    @Wire("#gridCheckerList")    private Grid  gridCheckerList;
    @Wire("#divRequeueBanner")   private Div   divRequeueBanner;
    @Wire("#lblRequeueSummary")  private Label lblRequeueSummary;


    // ════════════════════════════════════════════════════════════════
    // Panel 3 — Repair Form wires
    // ════════════════════════════════════════════════════════════════

    @Wire("#lblRepairBatchId")      private Label   lblRepairBatchId;
    @Wire("#lblFormChequeNumber")   private Label   lblFormChequeNumber;
    @Wire("#lblOriginalChequeNo")   private Label   lblOriginalChequeNo;
    @Wire("#lblOriginalBankName")   private Label   lblOriginalBankName;
    @Wire("#lblOriginalBranchName") private Label   lblOriginalBranchName;
    @Wire("#lblOriginalMicr")       private Label   lblOriginalMicr;
    @Wire("#lblOriginalIfsc")       private Label   lblOriginalIfsc;
    @Wire("#lblOriginalAccount")    private Label   lblOriginalAccount;

    @Wire("#txtCorrectedMicr")      private Textbox txtCorrectedMicr;
    @Wire("#txtCorrectedIfsc")      private Textbox txtCorrectedIfsc;
    @Wire("#txtCorrectedAccount")   private Textbox txtCorrectedAccount;
    @Wire("#txtCorrectedAmount")    private Textbox txtCorrectedAmount;
    @Wire("#txtCorrectedChequeDate")private Textbox txtCorrectedChequeDate;
    @Wire("#cmbFlagReason")         private Combobox cmbFlagReason;
    @Wire("#txtRepairRemarks")      private Textbox txtRepairRemarks;

    @Wire("#divFlagForm")           private Div     divFlagForm;
    @Wire("#txtFlagReason")         private Textbox txtFlagReason;


    // ════════════════════════════════════════════════════════════════
    // Panel 4 — Checker Panel wires
    // ════════════════════════════════════════════════════════════════

    @Wire("#lblCheckerBatchId")         private Label   lblCheckerBatchId;
    @Wire("#lblCheckerChequeNo")        private Label   lblCheckerChequeNo;
    @Wire("#lblCheckerRepairedBy")      private Label   lblCheckerRepairedBy;
    @Wire("#lblCheckerRepairedAt")      private Label   lblCheckerRepairedAt;

    // Comparison table labels
    @Wire("#cmpOrigMicr")    private Label cmpOrigMicr;
    @Wire("#cmpCorrMicr")    private Label cmpCorrMicr;
    @Wire("#cmpMatchMicr")   private Label cmpMatchMicr;

    @Wire("#cmpOrigIfsc")    private Label cmpOrigIfsc;
    @Wire("#cmpCorrIfsc")    private Label cmpCorrIfsc;
    @Wire("#cmpMatchIfsc")   private Label cmpMatchIfsc;

    @Wire("#cmpOrigAccount") private Label cmpOrigAccount;
    @Wire("#cmpCorrAccount") private Label cmpCorrAccount;
    @Wire("#cmpMatchAccount")private Label cmpMatchAccount;

    @Wire("#cmpOrigAmount")  private Label cmpOrigAmount;
    @Wire("#cmpCorrAmount")  private Label cmpCorrAmount;
    @Wire("#cmpMatchAmount") private Label cmpMatchAmount;

    @Wire("#cmpOrigDate")    private Label cmpOrigDate;
    @Wire("#cmpCorrDate")    private Label cmpCorrDate;
    @Wire("#cmpMatchDate")   private Label cmpMatchDate;

    @Wire("#cmpFlagReason")  private Label cmpFlagReason;

    @Wire("#divCheckerMakerRemarks")    private Div   divCheckerMakerRemarks;
    @Wire("#lblCheckerOperatorRemarks") private Label lblCheckerOperatorRemarks;

    @Wire("#divAlreadyVerified")        private Div   divAlreadyVerified;
    @Wire("#lblVerifiedBy")             private Label lblVerifiedBy;
    @Wire("#lblVerifiedAt")             private Label lblVerifiedAt;

    @Wire("#divCheckerActions")         private Div   divCheckerActions;
    @Wire("#txtCheckerRemarks")         private Textbox txtCheckerRemarks;


    // ════════════════════════════════════════════════════════════════
    // Init — runs after ZUL is composed
    // ════════════════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        currentUser = (UserModel) Sessions.getCurrent().getAttribute("loggedInUser");
        if (currentUser == null) {
            Executions.sendRedirect("/zul/login.zul");
            return;
        }

        showBatchListPanel();
    }


    // ════════════════════════════════════════════════════════════════
    // Panel Switching helpers
    // ════════════════════════════════════════════════════════════════

    private void showBatchListPanel() {
        panelBatchList.setVisible(true);
        panelEntryList.setVisible(false);
        panelRepairForm.setVisible(false);
        panelCheckerPanel.setVisible(false);
        loadBatchList();
    }

    private void showEntryListPanel() {
        panelBatchList.setVisible(false);
        panelEntryList.setVisible(true);
        panelRepairForm.setVisible(false);
        panelCheckerPanel.setVisible(false);
        loadEntryList();
    }

    private void showRepairFormPanel() {
        panelBatchList.setVisible(false);
        panelEntryList.setVisible(false);
        panelRepairForm.setVisible(true);
        panelCheckerPanel.setVisible(false);
        populateRepairForm();
    }

    private void showCheckerPanel() {
        panelBatchList.setVisible(false);
        panelEntryList.setVisible(false);
        panelRepairForm.setVisible(false);
        panelCheckerPanel.setVisible(true);
        populateCheckerPanel();
    }


    // ════════════════════════════════════════════════════════════════
    // Panel 1 — Load Batch List
    // ════════════════════════════════════════════════════════════════

    private void loadBatchList() {
        List<OutwardBatch> batches = micrRepairService.getMicrRepairBatches();

        int pending   = 0;
        int completed = 0;
        for (OutwardBatch b : batches) {
            if ("APPROVED".equals(b.getStatus())) {
                completed++;
            } else {
                pending++;
            }
        }

        lblTotalBatches.setValue(String.valueOf(batches.size()));
        lblPendingBatches.setValue(String.valueOf(pending));
        lblCompletedBatches.setValue(String.valueOf(completed));
        lblBatchCount.setValue(batches.size() + " batch(es)");

        gridBatchList.setModel(new ListModelList<>(batches));
    }


    // ════════════════════════════════════════════════════════════════
    // Panel 2 — Load Entry List + Checker List
    // ════════════════════════════════════════════════════════════════

    private void loadEntryList() {
        lblActiveBatchId.setValue(safe(activeBatch.getBatchId()));

        List<MicrRepairEntry> allEntries =
            micrRepairService.getRepairEntriesByBatchId(activeBatch.getId());

        // Split into operator queue and checker queue
        List<MicrRepairEntry> operatorQueue = new ArrayList<>();
        List<MicrRepairEntry> checkerQueue  = new ArrayList<>();

        int pending  = 0;
        int repaired = 0;
        int flagged  = 0;
        int verified = 0;

        for (MicrRepairEntry e : allEntries) {
            String status = safe(e.getRepairStatus());

            if ("REPAIRED".equals(status) && !Boolean.TRUE.equals(e.getCheckerVerified())) {
                // Submitted by operator, waiting for checker
                checkerQueue.add(e);
                repaired++;
            } else if ("FLAGGED".equals(status)) {
                operatorQueue.add(e);
                flagged++;
            } else if (Boolean.TRUE.equals(e.getCheckerVerified())) {
                // Already verified — show in operator list (as done)
                operatorQueue.add(e);
                verified++;
            } else {
                // PENDING
                operatorQueue.add(e);
                pending++;
            }
        }

        // Update stat labels
        lblTotalEntries.setValue(String.valueOf(allEntries.size()));
        lblPendingEntries.setValue(String.valueOf(pending));
        lblRepairedEntries.setValue(String.valueOf(checkerQueue.size()));
        lblFlaggedEntries.setValue(String.valueOf(flagged));
        lblPendingBadge.setValue(pending + " pending");
        lblCheckerBadge.setValue(checkerQueue.size() + " awaiting");

        // Load the two grids
        gridEntryList.setModel(new ListModelList<>(operatorQueue));
        gridCheckerList.setModel(new ListModelList<>(checkerQueue));

        // Show re-queue banner if all entries are resolved
        boolean allDone = allEntries.stream().allMatch(
            e -> Boolean.TRUE.equals(e.getCheckerVerified()) || "FLAGGED".equals(e.getRepairStatus())
        );

        if (!allEntries.isEmpty() && allDone) {
            divRequeueBanner.setVisible(true);
            lblRequeueSummary.setValue(
                verified + " approved, " + flagged + " flagged/returned to depositor."
            );
        } else {
            divRequeueBanner.setVisible(false);
        }
    }


    // ════════════════════════════════════════════════════════════════
    // Panel 3 — Populate Repair Form
    // ════════════════════════════════════════════════════════════════

    private void populateRepairForm() {
        String batchId = activeBatch != null ? safe(activeBatch.getBatchId()) : "";
        lblRepairBatchId.setValue(batchId);

        // Cheque number in page title
        lblFormChequeNumber.setValue(safe(activeEntry.getChequeNumber()));

        // Original values — read only
        lblOriginalChequeNo.setValue(safe(activeEntry.getChequeNumber()));
        lblOriginalMicr.setValue(safe(activeEntry.getOriginalMicrCode()));
        lblOriginalIfsc.setValue(safe(activeEntry.getOriginalIfscCode()));
        lblOriginalAccount.setValue(safe(activeEntry.getOriginalAccountNumber()));

        // Bank name and branch from the linked OutwardCheque
        if (activeEntry.getOutwardCheque() != null) {
            lblOriginalBankName.setValue(safe(activeEntry.getOutwardCheque().getBankName()));
            lblOriginalBranchName.setValue(safe(activeEntry.getOutwardCheque().getBranchName()));
        } else {
            lblOriginalBankName.setValue("—");
            lblOriginalBranchName.setValue("—");
        }

        // Pre-fill corrected fields with original values so operator can see what to fix
        txtCorrectedMicr.setValue(safe(activeEntry.getOriginalMicrCode()));
        txtCorrectedIfsc.setValue(safe(activeEntry.getOriginalIfscCode()));
        txtCorrectedAccount.setValue(safe(activeEntry.getOriginalAccountNumber()));
        txtCorrectedAmount.setValue("");
        txtCorrectedChequeDate.setValue("");
        txtRepairRemarks.setValue("");
        txtFlagReason.setValue("");
        cmbFlagReason.setValue("");

        // Hide the flag/return form by default
        divFlagForm.setVisible(false);
    }


    // ════════════════════════════════════════════════════════════════
    // Panel 4 — Populate Checker Panel
    // ════════════════════════════════════════════════════════════════

    private void populateCheckerPanel() {
        String batchId = activeBatch != null ? safe(activeBatch.getBatchId()) : "";
        lblCheckerBatchId.setValue(batchId);
        lblCheckerChequeNo.setValue(safe(checkerEntry.getChequeNumber()));

        // Who repaired it and when
        lblCheckerRepairedBy.setValue(safe(checkerEntry.getRepairedBy()));

        LocalDateTime repairedAt = checkerEntry.getRepairedAt();
        lblCheckerRepairedAt.setValue(repairedAt != null ? repairedAt.format(DISPLAY_FMT) : "—");

        // Get original values from linked OutwardCheque
        String origMicr    = safe(checkerEntry.getOriginalMicrCode());
        String origIfsc    = safe(checkerEntry.getOriginalIfscCode());
        String origAccount = safe(checkerEntry.getOriginalAccountNumber());
        String origAmount  = checkerEntry.getOutwardCheque() != null
            ? String.valueOf(checkerEntry.getOutwardCheque().getAmountInFigures()) : "—";
        String origDate    = checkerEntry.getOutwardCheque() != null
            ? String.valueOf(checkerEntry.getOutwardCheque().getChequeDate()) : "—";

        // Get corrected values entered by operator
        String corrMicr    = safe(checkerEntry.getCorrectedMicrCode());
        String corrIfsc    = safe(checkerEntry.getCorrectedIfscCode());
        String corrAccount = safe(checkerEntry.getCorrectedAccountNumber());
        String corrAmount  = safe(checkerEntry.getCorrectedAmount());
        String corrDate    = safe(checkerEntry.getCorrectedChequeDate());
        String flagReason  = safe(checkerEntry.getFlagReason());

        // Fill comparison table
        cmpOrigMicr.setValue(origMicr.isEmpty() ? "—" : origMicr);
        cmpCorrMicr.setValue(corrMicr.isEmpty() ? "—" : corrMicr);
        cmpMatchMicr.setValue(matchLabel(origMicr, corrMicr));

        cmpOrigIfsc.setValue(origIfsc.isEmpty() ? "—" : origIfsc);
        cmpCorrIfsc.setValue(corrIfsc.isEmpty() ? "—" : corrIfsc);
        cmpMatchIfsc.setValue(matchLabel(origIfsc, corrIfsc));

        cmpOrigAccount.setValue(origAccount.isEmpty() ? "—" : origAccount);
        cmpCorrAccount.setValue(corrAccount.isEmpty() ? "—" : corrAccount);
        cmpMatchAccount.setValue(matchLabel(origAccount, corrAccount));

        cmpOrigAmount.setValue(origAmount.isEmpty() ? "—" : origAmount);
        cmpCorrAmount.setValue(corrAmount.isEmpty() ? "Unchanged" : corrAmount);
        cmpMatchAmount.setValue(corrAmount.isEmpty() ? "✓ Unchanged" : "⚠ Modified");

        cmpOrigDate.setValue(origDate.isEmpty() ? "—" : origDate);
        cmpCorrDate.setValue(corrDate.isEmpty() ? "Unchanged" : corrDate);
        cmpMatchDate.setValue(corrDate.isEmpty() ? "✓ Unchanged" : "⚠ Modified");

        cmpFlagReason.setValue(flagReason.isEmpty() ? "—" : flagReason);

        // Show operator remarks if any
        String repairRemarks = safe(checkerEntry.getRepairRemarks());
        if (!repairRemarks.isEmpty()) {
            divCheckerMakerRemarks.setVisible(true);
            lblCheckerOperatorRemarks.setValue(repairRemarks);
        } else {
            divCheckerMakerRemarks.setVisible(false);
        }

        // If already verified — show the verified banner, hide action block
        if (Boolean.TRUE.equals(checkerEntry.getCheckerVerified())) {
            divAlreadyVerified.setVisible(true);
            lblVerifiedBy.setValue(safe(checkerEntry.getCheckerVerifiedBy()));
            LocalDateTime verifiedAt = checkerEntry.getCheckerVerifiedAt();
            lblVerifiedAt.setValue(verifiedAt != null ? verifiedAt.format(DISPLAY_FMT) : "—");
            divCheckerActions.setVisible(false);
        } else {
            divAlreadyVerified.setVisible(false);
            divCheckerActions.setVisible(true);
            txtCheckerRemarks.setValue("");
        }
    }


    // ════════════════════════════════════════════════════════════════
    // Sidebar navigation event
    // ════════════════════════════════════════════════════════════════

    @Listen("onNavMicrRepair = #micrApp")
    public void onNavMicrRepair() {
        showBatchListPanel();
    }


    // ════════════════════════════════════════════════════════════════
    // Open Batch — reads batch ID from button custom-attribute
    // ════════════════════════════════════════════════════════════════

    @Listen("onOpenBatch = #micrApp")
    public void onOpenBatch(Event e) {
        Long batchPk = getLongAttribute(e, "batchId");
        if (batchPk == null) return;

        List<OutwardBatch> batches = micrRepairService.getMicrRepairBatches();
        for (OutwardBatch b : batches) {
            if (b.getId().equals(batchPk)) {
                activeBatch = b;
                showEntryListPanel();
                return;
            }
        }
    }


    // ════════════════════════════════════════════════════════════════
    // Open Entry (Repair Form) — reads entry ID from button custom-attribute
    // ════════════════════════════════════════════════════════════════

    @Listen("onOpenEntry = #micrApp")
    public void onOpenEntry(Event e) {
        Long entryId = getLongAttribute(e, "entryId");
        if (entryId == null) return;

        MicrRepairEntry entry = micrRepairService.getRepairEntryById(entryId);
        if (entry == null) return;

        activeEntry = entry;
        showRepairFormPanel();
    }


    // ════════════════════════════════════════════════════════════════
    // Open Checker Panel — reads entry ID from button custom-attribute
    // ════════════════════════════════════════════════════════════════

    @Listen("onOpenCheckerPanel = #micrApp")
    public void onOpenCheckerPanel(Event e) {
        Long entryId = getLongAttribute(e, "entryId");
        if (entryId == null) return;

        MicrRepairEntry entry = micrRepairService.getRepairEntryById(entryId);
        if (entry == null) return;

        checkerEntry = entry;
        showCheckerPanel();
    }


    // ════════════════════════════════════════════════════════════════
    // Back buttons
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnBackToBatchList")
    public void onBackToBatchList() {
        showBatchListPanel();
    }

    @Listen("onClick = #btnBackToEntryList")
    public void onBackToEntryList() {
        showEntryListPanel();
    }

    @Listen("onClick = #btnBackToEntryListFromChecker")
    public void onBackToEntryListFromChecker() {
        showEntryListPanel();
    }


    // ════════════════════════════════════════════════════════════════
    // Submit Repair (Panel 3)
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnSubmitRepair")
    public void onSubmitRepair() {
        String remarks = txtRepairRemarks.getValue().trim();

        if (remarks.isEmpty()) {
            Messagebox.show(
                "Repair remarks are required before submitting.",
                "Required", Messagebox.OK, Messagebox.EXCLAMATION
            );
            return;
        }

        try {
            micrRepairService.submitRepair(
                activeEntry.getId(),
                txtCorrectedMicr.getValue().trim(),
                txtCorrectedIfsc.getValue().trim(),
                txtCorrectedAccount.getValue().trim(),
                txtCorrectedAmount.getValue().trim(),
                txtCorrectedChequeDate.getValue().trim(),
                remarks,
                currentUser.getUserId()
            );

            Messagebox.show(
                "Repair submitted for Cheque " + activeEntry.getChequeNumber()
                + ". It is now in the Checker verification queue.",
                "Repair Submitted",
                Messagebox.OK,
                Messagebox.INFORMATION
            );

            activeEntry = null;
            showEntryListPanel();

        } catch (Exception ex) {
            Messagebox.show(
                "Error: " + ex.getMessage(),
                "Error", Messagebox.OK, Messagebox.ERROR
            );
        }
    }


    // ════════════════════════════════════════════════════════════════
    // Show / Cancel return-to-depositor form
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnShowFlagForm")
    public void onShowFlagForm() {
        divFlagForm.setVisible(true);
        txtFlagReason.setFocus(true);
    }

    @Listen("onClick = #btnCancelFlag")
    public void onCancelFlag() {
        divFlagForm.setVisible(false);
        txtFlagReason.setValue("");
    }


    // ════════════════════════════════════════════════════════════════
    // Submit Flag / Return to Depositor (Panel 3)
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnSubmitFlag")
    public void onSubmitFlag() {
        String reason = txtFlagReason.getValue().trim();

        if (reason.isEmpty()) {
            Messagebox.show(
                "Return reason is required.",
                "Required", Messagebox.OK, Messagebox.EXCLAMATION
            );
            return;
        }

        try {
            micrRepairService.flagEntry(
                activeEntry.getId(),
                reason,
                currentUser.getUserId()
            );

            Messagebox.show(
                "Cheque " + activeEntry.getChequeNumber()
                + " has been flagged and will be returned to the depositor.",
                "Flagged",
                Messagebox.OK,
                Messagebox.INFORMATION
            );

            activeEntry = null;
            showEntryListPanel();

        } catch (Exception ex) {
            Messagebox.show(
                "Error: " + ex.getMessage(),
                "Error", Messagebox.OK, Messagebox.ERROR
            );
        }
    }


    // ════════════════════════════════════════════════════════════════
    // Checker — Approve Repair (Panel 4)
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnApproveRepair")
    public void onApproveRepair() {
        String remarks = txtCheckerRemarks.getValue().trim();

        try {
            // Save checker remarks into the entry and mark verified
            MicrRepairEntry entry = micrRepairService.getRepairEntryById(checkerEntry.getId());
            if (entry == null) {
                Messagebox.show("Entry not found.", "Error", Messagebox.OK, Messagebox.ERROR);
                return;
            }

            // Set checker verification fields
            entry.setCheckerVerified(true);
            entry.setCheckerVerifiedBy(currentUser.getUserId());
            entry.setCheckerVerifiedAt(LocalDateTime.now());
            entry.setCheckerRemarks(remarks.isEmpty() ? null : remarks);

            // Save back to DB
            micrRepairService.saveCheckerVerification(entry);

            Messagebox.show(
                "Repair for Cheque " + checkerEntry.getChequeNumber()
                + " approved successfully. ✅",
                "Approved",
                Messagebox.OK,
                Messagebox.INFORMATION
            );

            checkerEntry = null;
            showEntryListPanel();

        } catch (Exception ex) {
            Messagebox.show(
                "Error: " + ex.getMessage(),
                "Error", Messagebox.OK, Messagebox.ERROR
            );
        }
    }


    // ════════════════════════════════════════════════════════════════
    // Checker — Reject / Send Back to Operator (Panel 4)
    // ════════════════════════════════════════════════════════════════

    @Listen("onClick = #btnRejectRepair")
    public void onRejectRepair() {
        String remarks = txtCheckerRemarks.getValue().trim();

        if (remarks.isEmpty()) {
            Messagebox.show(
                "Checker remarks are required when sending back to operator.",
                "Required", Messagebox.OK, Messagebox.EXCLAMATION
            );
            return;
        }

        try {
            // Reset repair status to PENDING so operator can re-enter
            MicrRepairEntry entry = micrRepairService.getRepairEntryById(checkerEntry.getId());
            if (entry == null) {
                Messagebox.show("Entry not found.", "Error", Messagebox.OK, Messagebox.ERROR);
                return;
            }

            entry.setRepairStatus("PENDING");
            entry.setCheckerVerified(false);
            entry.setCheckerVerifiedBy(null);
            entry.setCheckerVerifiedAt(null);
            entry.setCheckerRemarks(remarks);
            // Clear corrected fields so operator enters fresh
            entry.setCorrectedMicrCode(null);
            entry.setCorrectedIfscCode(null);
            entry.setCorrectedAccountNumber(null);
            entry.setCorrectedAmount(null);
            entry.setCorrectedChequeDate(null);
            entry.setRepairedBy(null);
            entry.setRepairedAt(null);
            entry.setRepairRemarks(null);

            // Save back to DB
            micrRepairService.saveCheckerVerification(entry);

            Messagebox.show(
                "Cheque " + checkerEntry.getChequeNumber()
                + " sent back to operator for re-entry. Checker remarks saved.",
                "Sent Back",
                Messagebox.OK,
                Messagebox.INFORMATION
            );

            checkerEntry = null;
            showEntryListPanel();

        } catch (Exception ex) {
            Messagebox.show(
                "Error: " + ex.getMessage(),
                "Error", Messagebox.OK, Messagebox.ERROR
            );
        }
    }


    // ════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════

    // Reads a Long from a button's custom-attribute safely
    private Long getLongAttribute(Event e, String attributeName) {
        if (!(e.getTarget() instanceof Button)) return null;
        Button btn = (Button) e.getTarget();
        Object val = btn.getAttribute(attributeName);
        if (val == null) return null;
        try {
            return Long.parseLong(val.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // Returns "✓ Unchanged" or "⚠ Modified" for comparison table
    private String matchLabel(String original, String corrected) {
        if (corrected == null || corrected.isEmpty() || original.equals(corrected)) {
            return "✓ Unchanged";
        }
        return "⚠ Modified";
    }

    // Null-safe string
    private String safe(String s) {
        return s == null ? "" : s;
    }
}