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

import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.service.AccountEntryService;
import com.iispl.serviceImpl.AccountEntryServiceImpl;
import com.iispl.util.AmountToWords;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/outward/AccountEntryComposer.java
 * Purpose : Handles the Account and Amount Entry screen.
 *
 * Two access modes:
 *   1. From MICR Repair "Proceed" → URL has ?batchId → skip batch select
 *   2. From sidebar "Account & Amount" → no batchId → show batch select
 *
 * Three views:
 *   emptyStateView  → no batches ready
 *   batchSelectView → batch selection table
 *   entryView       → split screen entry form
 */
public class AccountEntryComposer extends SelectorComposer<Component> {

    private final AccountEntryService entryService = new AccountEntryServiceImpl();
    private final DecimalFormat       moneyFmt     = new DecimalFormat("#,##0.00");

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
    @Wire private Label  navLabel;

    // ── Left Panel — Cheque Images ──
    @Wire private Image  frontImage;
    @Wire private Image  backImage;

    // ── Right Panel — Cheque Info (read-only) ──
    @Wire private Label  chqNoDisplay;
    @Wire private Label  chqDateDisplay;
    @Wire private Label  entryStatusBadge;

    // ── Right Panel — Account Section ──
    @Wire private Textbox accountNoBox;
    @Wire private Div     valResultDiv;
    @Wire private Label   valResultLabel;

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
            // Access mode 1: came from MICR Repair (batchId in URL)
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
            // Access mode 2: came from sidebar
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

        // Pending = count from DB
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
            // All cheques already entered
            Clients.showNotification(
                "All entries already done for this batch.",
                "info", null, "top_center", 3000);
            return;
        }

        loadChequeForm(pendingList.get(currentIndex));
    }

    // ════════════════════════════════════════════════════
    //  Stats Bar
    // ════════════════════════════════════════════════════

    private void refreshStatsBar() {
        statsBatchId.setValue(safe(batchId));

        String totalAmt = currentBatch.getActualAmount() != null
            ? "₹" + moneyFmt.format(currentBatch.getActualAmount()) : "—";
        statsTotal.setValue(totalAmt);

        int done      = totalCheques - pendingList.size();
        int remaining = pendingList.size();
        statsDoneCount.setValue(String.valueOf(done));
        statsRemCount.setValue(String.valueOf(remaining));
    }

    // ════════════════════════════════════════════════════
    //  Load One Cheque into the Form
    // ════════════════════════════════════════════════════

    private void loadChequeForm(OutwardCheque cheque) {
        // Navigation label
        navLabel.setValue((currentIndex + 1) + " of " + pendingList.size());

        // Status badge
        entryStatusBadge.setValue("Pending");
        entryStatusBadge.setSclass("badge b-pend");

        // Cheque info (read-only)
        chqNoDisplay.setValue(safe(cheque.getChequeNo()));
        chqDateDisplay.setValue(
            cheque.getChequeDate() != null
            ? cheque.getChequeDate().toString() : "—");

        // Account section — clear validate result
        accountNoBox.setValue(safe(cheque.getAccountNo()));
        valResultDiv.setVisible(false);
        valResultLabel.setValue("");

        // Amount entry — pre-fill from XML parse
        if (cheque.getAmount() != null) {
            amountBox.setValue(cheque.getAmount());
            amountInWordsBox.setValue(
                AmountToWords.convert(cheque.getAmount().doubleValue()));
        } else {
        	amountBox.setValue((BigDecimal) null);
            amountInWordsBox.setValue("");
        }

        // Cheque date
        chequeDateBox.setValue(
            cheque.getChequeDate() != null
            ? cheque.getChequeDate().toString() : "");

        // Payee name — pre-fill from XML parse
        payeeNameBox.setValue(safe(cheque.getPayeeName()));

        // Load actual images via ImageServlet
        loadImages(cheque);

        // Hide reject panel
        rejectPanel.setVisible(false);
    }

    private void loadImages(OutwardCheque cheque) {
        String frontPath = cheque.getFrontImagePath();
        String backPath  = cheque.getBackImagePath();

        try {
            if (frontPath != null && !frontPath.trim().isEmpty()) {
                frontImage.setSrc("/imageServlet?path="
                    + URLEncoder.encode(frontPath.trim(), "UTF-8"));
            } else {
                frontImage.setSrc("");
            }
            if (backPath != null && !backPath.trim().isEmpty()) {
                backImage.setSrc("/imageServlet?path="
                    + URLEncoder.encode(backPath.trim(), "UTF-8"));
            } else {
                backImage.setSrc("");
            }
        } catch (UnsupportedEncodingException e) {
            System.err.println("AccountEntryComposer → image URL encode failed: "
                    + e.getMessage());
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
    //  Validate Account (CBS Mock)
    // ════════════════════════════════════════════════════

    @Listen("onClick = #validateBtn")
    public void onValidate() {
        String accNo = accountNoBox.getValue();
        if (isBlank(accNo)) {
            Clients.showNotification(
                "Please enter an account number first.",
                "warning", null, "top_center", 2000);
            return;
        }
        // CBS on hold — show mock validated status
        valResultLabel.setValue(
            "Validated  (CBS integration pending)");
        valResultLabel.setSclass("val-ok");
        valResultDiv.setVisible(true);

        System.out.println("AccountEntryComposer → Mock validate for acc: "
                + accNo);
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

        // Gather values
        String     accNo     = accountNoBox.getValue();
        BigDecimal amount    = amountBox.getValue();
        String     dateStr   = chequeDateBox.getValue();
        String     words     = amountInWordsBox.getValue();
        String     payee     = payeeNameBox.getValue();

        // Validate required fields
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

        // Auto-fill words if empty
        if (isBlank(words) && amount != null) {
            words = AmountToWords.convert(amount.doubleValue());
        }

        // Save to DB
        boolean ok = entryService.saveEntry(
            cheque.getId(),
            accNo.trim(),
            "Validated",      // CBS mock account holder
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
            "Cheque " + cheque.getChequeNo() + " entry saved.",
            "info", null, "top_center", 2000);

        // Remove from pending list
        pendingList.remove(currentIndex);

        // Check if all done
        if (entryService.isAllEntriesDone(currentBatch.getId())) {
            entryService.submitBatch(currentBatch.getId());
            Clients.showNotification(
                "All entries done! Batch "
                + batchId + " submitted to Checker queue.",
                "info", null, "top_center", 4000);
            showBatchSubmittedState();
            return;
        }

        // Move to next cheque
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
        Clients.showNotification(
            "Batch " + batchId
            + " is now in the Checker queue.",
            "info", null, "top_center", 5000);
        // Redirect back to batch upload after short delay
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
            entryService.submitBatch(currentBatch.getId());
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
    //  Navigation — Back buttons
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

    private String safe(String s) {
        return s != null ? s.trim() : "";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}