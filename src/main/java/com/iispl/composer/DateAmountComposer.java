package com.iispl.composer;

import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.RejectRepairService;
import com.iispl.serviceImpl.RejectRepairServiceImpl;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DateAmountComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String SESSION_BATCH_ID  = "cts_inward_batch_id";
    private static final String SESSION_MAX_STEP  = "cts_inward_max_step";
    private static final String PAGE_STEP1 = "/inward/inwardMicr/RejectRepair.zul";
    private static final String PAGE_STEP3 = "/inward/inwardMicr/PayeeAccount.zul";

    private static final int PAGE_SIZE = 10;

    // ── Wizard bar ────────────────────────────────────────────────────────
    @Wire("#btnStep1")       private Button btnStep1;
    @Wire("#btnStep2")       private Button btnStep2;
    @Wire("#btnStep3")       private Button btnStep3;
    @Wire("#conn1")          private Div    conn1;
    @Wire("#conn2")          private Div    conn2;
    @Wire("#lblStep3Num")    private Label  lblStep3Num;
    @Wire("#lblStep3Desc")   private Label  lblStep3Desc;

    // ── Sub-toolbar ───────────────────────────────────────────────────────
    @Wire("#lblBatchBadge")  private Label    lblBatchBadge;
    @Wire("#lblPendingBadge")private Label    lblPendingBadge;
    @Wire("#cmbFilter")      private Combobox cmbFilter;
    @Wire("#btnBackToStep1") private Button   btnBackToStep1;

    // ── Panels ────────────────────────────────────────────────────────────
    @Wire("#listPanel")           private Div listPanel;
    @Wire("#reviewSplitPanel")    private Div reviewSplitPanel;

    // ── List panel ────────────────────────────────────────────────────────
    @Wire("#chequeListbox")  private Listbox chequeListbox;
    @Wire("#lblPageInfo")    private Label   lblPageInfo;
    @Wire("#btnPrevPage")    private Button  btnPrevPage;
    @Wire("#btnNextPage")    private Button  btnNextPage;
    @Wire("#btnNextStep3")   private Button  btnNextStep3;

    // ── Split-screen left panel ───────────────────────────────────────────
    @Wire("#btnPrevCheque")      private Button btnPrevCheque;
    @Wire("#btnNextCheque")      private Button btnNextCheque;
    @Wire("#lblNavIndicator")    private Label  lblNavIndicator;
    @Wire("#lblChequeBankName")  private Label  lblChequeBankName;
    @Wire("#lblChequeDate")      private Label  lblChequeDate;
    @Wire("#lblChequeWords")     private Label  lblChequeWords;
    @Wire("#lblChequeAmt")       private Label  lblChequeAmt;
    @Wire("#daWarningBar")       private Div    daWarningBar;

    // ── Split-screen right panel ──────────────────────────────────────────
    @Wire("#lblOcrErrorBadge")   private Label    lblOcrErrorBadge;
    @Wire("#lblProcDate")        private Label    lblProcDate;
    @Wire("#lblProcAmt")         private Label    lblProcAmt;
    @Wire("#lblRcvdDate")        private Label    lblRcvdDate;
    @Wire("#lblRcvdAmt")         private Label    lblRcvdAmt;
    @Wire("#dtCorrectedDate")    private Datebox  dtCorrectedDate;
    @Wire("#numCorrectedAmt")    private Doublebox numCorrectedAmt;
    @Wire("#txtRemarks")         private Textbox  txtRemarks;
    @Wire("#btnAccept")          private Button   btnAccept;
    @Wire("#btnReject")          private Button   btnReject;
    @Wire("#btnRefer")           private Button   btnRefer;
    @Wire("#btnBackToList2")     private Button   btnBackToList2;

    // ── Reject reason popup ───────────────────────────────────────────────
    @Wire("#rejectReasonPopup")  private Window   rejectReasonPopup;
    @Wire("#lblRejChqNo")        private Label    lblRejChqNo;
    @Wire("#lblRejAmt")          private Label    lblRejAmt;
    @Wire("#cmbRejectReason")    private Combobox cmbRejectReason;
    @Wire("#txtRejectRemarks")   private Textbox  txtRejectRemarks;
    @Wire("#btnConfirmReject")   private Button   btnConfirmReject;
    @Wire("#btnCancelReject")    private Button   btnCancelReject;

    // ── Service & state ───────────────────────────────────────────────────
    private final RejectRepairService service = new RejectRepairServiceImpl();

    private String             currentBatchId;
    private List<InwardCheque> allCheques;
    private List<InwardCheque> reviewList;   // cheques shown in split-screen nav
    private InwardCheque       selectedCheque;

    private int currentPage  = 1;
    private int reviewIdx    = 0;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        resolveBatchId();
        if (currentBatchId != null) {
            allCheques = service.getStep2ChequesByBatchId(currentBatchId);
            if (allCheques == null) allCheques = Collections.emptyList();
            updateBatchBadge();
            renderTable();
        }
        wireEvents();
    }

    // ── Batch ID resolution ───────────────────────────────────────────────

    private void resolveBatchId() {
        String param = Executions.getCurrent().getParameter("batchId");
        if (param != null && !param.isBlank()) {
            currentBatchId = param.trim();
            Sessions.getCurrent().setAttribute(SESSION_BATCH_ID, currentBatchId);
            return;
        }
        Object sess = Sessions.getCurrent().getAttribute(SESSION_BATCH_ID);
        if (sess != null) {
            currentBatchId = sess.toString();
            return;
        }
        // Last resort — first eligible batch
        var batches = service.getRepairEligibleBatches();
        if (batches != null && !batches.isEmpty()) {
            currentBatchId = batches.get(0).getBatchId();
            Sessions.getCurrent().setAttribute(SESSION_BATCH_ID, currentBatchId);
        }
    }

    // ── Event wiring ──────────────────────────────────────────────────────

    private void wireEvents() {
        // Filter
        addEvt(cmbFilter, Events.ON_SELECT, e -> { currentPage = 1; renderTable(); });

        // Pagination
        addEvt(btnPrevPage, Events.ON_CLICK, e -> {
            if (currentPage > 1) { currentPage--; renderTable(); }
        });
        addEvt(btnNextPage, Events.ON_CLICK, e -> {
            if (currentPage < totalPages()) { currentPage++; renderTable(); }
        });

        // Split-screen navigation
        addEvt(btnPrevCheque, Events.ON_CLICK, e -> {
            if (reviewIdx > 0) { reviewIdx--; loadReviewRecord(); }
        });
        addEvt(btnNextCheque, Events.ON_CLICK, e -> {
            if (reviewList != null && reviewIdx < reviewList.size() - 1) {
                reviewIdx++; loadReviewRecord();
            }
        });

        // Repair actions
        addEvt(btnAccept,       Events.ON_CLICK, e -> doAccept());
        addEvt(btnReject,       Events.ON_CLICK, e -> openRejectPopup());
        addEvt(btnRefer,        Events.ON_CLICK, e -> doRefer());
        addEvt(btnBackToList2,  Events.ON_CLICK, e -> showList());

        // Popup
        addEvt(btnConfirmReject, Events.ON_CLICK, e -> doConfirmReject());
        addEvt(btnCancelReject,  Events.ON_CLICK, e -> rejectReasonPopup.setVisible(false));
    }

    private void addEvt(Component c, String evt,
                        org.zkoss.zk.ui.event.EventListener<?> l) {
        if (c != null) c.addEventListener(evt, l);
    }

    // ── Panel switching ───────────────────────────────────────────────────

    private void showList() {
        setVis(listPanel,        true);
        setVis(reviewSplitPanel, false);
    }

    private void showSplitReview(int idx) {
        // Build list of cheques shown in split-screen nav
        // (all cheques, not just filtered — consistent with step 1 approach)
        reviewList = (allCheques != null) ? allCheques : Collections.emptyList();
        reviewIdx  = Math.max(0, Math.min(reviewList.size() - 1, idx));
        setVis(listPanel,        false);
        setVis(reviewSplitPanel, true);
        loadReviewRecord();
    }

    private static void setVis(Div div, boolean visible) {
        if (div != null) div.setVisible(visible);
    }

    // ── Render list table ─────────────────────────────────────────────────

    private void renderTable() {
        if (chequeListbox == null || allCheques == null) return;

        List<InwardCheque> filtered = getFilteredList();
        int total      = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage    = Math.min(currentPage, totalPages);

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        chequeListbox.getItems().clear();
        int rowNum = from + 1;

        for (InwardCheque c : filtered.subList(from, to)) {
            Listitem row = new Listitem();

            addCell(row, String.valueOf(rowNum++));
            addCell(row, nvl(c.getChequeNo()));
            addCell(row, nvl(c.getPresentingBankName()));

            // Proc Date
            addCell(row, c.getChequeDate() != null
                    ? c.getChequeDate().format(FMT) : "—");

            // Rcvd Date — highlight mismatch
            boolean dateErr = c.getChequeDateOcr() == null
                    || !c.getChequeDateOcr().equals(c.getChequeDate());
            Listcell rcvdDateCell = new Listcell(
                    c.getChequeDateOcr() != null
                            ? c.getChequeDateOcr().format(FMT) : "—");
            if (dateErr) rcvdDateCell.setStyle("color:var(--danger,#dc2626);font-weight:600");
            row.appendChild(rcvdDateCell);

            // Proc Amount
            Listcell procAmtCell = new Listcell(
                    c.getAmount() != null ? "₹ " + fmt(c.getAmount()) : "—");
            procAmtCell.setStyle("text-align:right");
            row.appendChild(procAmtCell);

            // Rcvd Amount — highlight mismatch
            boolean amtErr = c.getAmountOcr() == null
                    || c.getAmountOcr().compareTo(c.getAmount()) != 0;
            Listcell rcvdAmtCell = new Listcell(
                    c.getAmountOcr() != null ? "₹ " + fmt(c.getAmountOcr()) : "—");
            String amtStyle = "text-align:right" + (amtErr
                    ? ";color:var(--danger,#dc2626);font-weight:600" : "");
            rcvdAmtCell.setStyle(amtStyle);
            row.appendChild(rcvdAmtCell);

            // Status badge
            Listcell statusCell = new Listcell();
            statusCell.setStyle("text-align:center");
            Label badge = new Label(resolveStatusLabel(c.getRepairStatus()));
            badge.setSclass(resolveStatusSclass(c.getRepairStatus()));
            statusCell.appendChild(badge);
            row.appendChild(statusCell);

            // Review button — opens split-screen
            Listcell actionCell = new Listcell();
            actionCell.setStyle("text-align:center");
            Button reviewBtn = new Button("Review");
            reviewBtn.setSclass("btn-repair");
            final int listIdx = filtered.indexOf(c);
            // Use global index in allCheques for split-screen nav
            final int globalIdx = allCheques.indexOf(c);
            reviewBtn.addEventListener(Events.ON_CLICK, ev -> {
                ev.stopPropagation();
                showSplitReview(globalIdx >= 0 ? globalIdx : listIdx);
            });
            actionCell.appendChild(reviewBtn);
            row.appendChild(actionCell);

            chequeListbox.appendChild(row);
        }

        if (lblPageInfo != null)
            lblPageInfo.setValue("Page " + currentPage + " of " + totalPages
                    + " | " + total + " records");
        if (btnPrevPage != null) btnPrevPage.setDisabled(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisabled(currentPage >= totalPages);

        updatePendingBadge();
    }

    // ── Load review record into split-screen ──────────────────────────────

    private void loadReviewRecord() {
        if (reviewList == null || reviewList.isEmpty()) { showList(); return; }
        reviewIdx = Math.max(0, Math.min(reviewList.size() - 1, reviewIdx));
        selectedCheque = reviewList.get(reviewIdx);
        InwardCheque c = selectedCheque;

        // Navigation indicator
        if (lblNavIndicator != null)
            lblNavIndicator.setValue((reviewIdx + 1) + " of " + reviewList.size());
        if (btnPrevCheque != null) btnPrevCheque.setDisabled(reviewIdx <= 0);
        if (btnNextCheque != null)
            btnNextCheque.setDisabled(reviewIdx >= reviewList.size() - 1);

        // ── Left panel: populate cheque card ─────────────────────────────
        boolean dateErr = c.getChequeDateOcr() == null
                || !c.getChequeDateOcr().equals(c.getChequeDate());
        boolean amtErr  = c.getAmountOcr() == null
                || (c.getAmount() != null
                    && c.getAmountOcr().compareTo(c.getAmount()) != 0);

        if (lblChequeBankName != null)
            lblChequeBankName.setValue(nvl(c.getPresentingBankName()));

        // Date field — red-bordered if mismatch
        if (lblChequeDate != null) {
            String dateVal = c.getChequeDateOcr() != null
                    ? c.getChequeDateOcr().format(FMT) : "—";
            lblChequeDate.setValue(dateVal);
            lblChequeDate.setSclass(dateErr
                    ? "cheque-date-field has-mismatch" : "cheque-date-field");
        }

        // Amount in words (derived simply)
        if (lblChequeWords != null)
            lblChequeWords.setValue(amountInWords(c.getAmount()));

        // Amount box — red if mismatch
        if (lblChequeAmt != null) {
            String amtVal = c.getAmount() != null ? "₹  " + fmt(c.getAmount()) : "—";
            lblChequeAmt.setValue(amtVal);
            lblChequeAmt.setSclass(amtErr
                    ? "cheque-amount-box has-mismatch" : "cheque-amount-box");
        }

        // Warning bar
        boolean hasMismatch = dateErr || amtErr;
        if (daWarningBar != null) daWarningBar.setVisible(hasMismatch);

        // OCR badge text
        if (lblOcrErrorBadge != null) {
            if (dateErr && amtErr)       lblOcrErrorBadge.setValue("DATE + AMT MISMATCH");
            else if (dateErr)            lblOcrErrorBadge.setValue("DATE MISMATCH");
            else if (amtErr)             lblOcrErrorBadge.setValue("AMOUNT MISMATCH");
            else                         lblOcrErrorBadge.setValue("✓ MATCH");
        }

        // ── Right panel: comparison values ───────────────────────────────
        if (lblProcDate != null)
            lblProcDate.setValue(c.getChequeDate() != null
                    ? c.getChequeDate().format(FMT) : "—");
        if (lblProcAmt != null)
            lblProcAmt.setValue(c.getAmount() != null
                    ? "₹  " + fmt(c.getAmount()) : "—");
        if (lblRcvdDate != null)
            lblRcvdDate.setValue(c.getChequeDateOcr() != null
                    ? c.getChequeDateOcr().format(FMT) : "—");
        if (lblRcvdAmt != null)
            lblRcvdAmt.setValue(c.getAmountOcr() != null
                    ? "₹  " + fmt(c.getAmountOcr()) : "—");

        // Pre-fill corrected fields with processed (authoritative) values
        if (dtCorrectedDate != null) {
            if (c.getChequeDate() != null) {
                dtCorrectedDate.setValue(java.util.Date.from(
                        c.getChequeDate()
                         .atStartOfDay(java.time.ZoneId.systemDefault())
                         .toInstant()));
            } else {
                dtCorrectedDate.setValue(null);
            }
        }
        if (numCorrectedAmt != null) {
            if (c.getAmount() != null)
                numCorrectedAmt.setValue(c.getAmount().doubleValue());
            else
                numCorrectedAmt.setValue(null);
        }
        if (txtRemarks != null) txtRemarks.setValue("");
    }

    // ── Accept ────────────────────────────────────────────────────────────

    private void doAccept() {
        if (selectedCheque == null) return;
        if (dtCorrectedDate != null && dtCorrectedDate.getValue() == null) {
            Messagebox.show("Please enter corrected date.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        if (numCorrectedAmt != null && numCorrectedAmt.getValue() == null) {
            Messagebox.show("Please enter corrected amount.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        // Persist corrected values
        if (dtCorrectedDate != null && dtCorrectedDate.getValue() != null) {
            selectedCheque.setChequeDateOcr(
                    dtCorrectedDate.getValue().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate());
        }
        if (numCorrectedAmt != null && numCorrectedAmt.getValue() != null) {
            selectedCheque.setAmountOcr(
                    BigDecimal.valueOf(numCorrectedAmt.getValue()));
        }
        selectedCheque.setRepairStatus("REPAIRED");
        if (txtRemarks != null)
            selectedCheque.setRemarks(txtRemarks.getValue());

        service.saveStep2Repair(selectedCheque);

        Messagebox.show("Cheque " + selectedCheque.getChequeNo() + " accepted ✓",
                "Success", Messagebox.OK, Messagebox.INFORMATION);

        moveToNextPending();
    }

    // ── Reject ────────────────────────────────────────────────────────────

    private void openRejectPopup() {
        if (selectedCheque == null) return;
        if (lblRejChqNo != null) lblRejChqNo.setValue(nvl(selectedCheque.getChequeNo()));
        if (lblRejAmt   != null) lblRejAmt.setValue(selectedCheque.getAmount() != null
                ? "₹ " + fmt(selectedCheque.getAmount()) : "—");
        if (cmbRejectReason  != null) cmbRejectReason.setSelectedItem(null);
        if (txtRejectRemarks != null) txtRejectRemarks.setValue("");
        if (rejectReasonPopup != null) rejectReasonPopup.setVisible(true);
    }

    private void doConfirmReject() {
        if (cmbRejectReason != null && cmbRejectReason.getSelectedItem() == null) {
            Messagebox.show("Please select a reject reason.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        if (selectedCheque == null) return;

        String reason = (cmbRejectReason != null && cmbRejectReason.getSelectedItem() != null)
                ? cmbRejectReason.getSelectedItem().getValue().toString() : "";
        String remarks = (txtRejectRemarks != null) ? txtRejectRemarks.getValue() : "";

        selectedCheque.setRepairStatus("REJECTED");
        selectedCheque.setRemarks(reason + (remarks.isBlank() ? "" : " | " + remarks));
        service.saveStep2Repair(selectedCheque);

        if (rejectReasonPopup != null) rejectReasonPopup.setVisible(false);
        Messagebox.show("Cheque " + selectedCheque.getChequeNo() + " rejected.",
                "Info", Messagebox.OK, Messagebox.INFORMATION);

        moveToNextPending();
    }

    // ── Refer ─────────────────────────────────────────────────────────────

    private void doRefer() {
        if (selectedCheque == null) return;
        selectedCheque.setRepairStatus("REFERRED_BACK");
        if (txtRemarks != null && !txtRemarks.getValue().isBlank())
            selectedCheque.setRemarks(txtRemarks.getValue());
        service.saveStep2Repair(selectedCheque);

        Messagebox.show("Cheque " + selectedCheque.getChequeNo() + " referred back.",
                "Info", Messagebox.OK, Messagebox.INFORMATION);

        moveToNextPending();
    }

    // ── Move to next pending after action ─────────────────────────────────

    private void moveToNextPending() {
        if (reviewList == null) { showList(); return; }

        // Try forward first
        for (int i = reviewIdx + 1; i < reviewList.size(); i++) {
            if (isPending(reviewList.get(i))) {
                reviewIdx = i; loadReviewRecord(); return;
            }
        }
        // Then backward
        for (int i = 0; i < reviewIdx; i++) {
            if (isPending(reviewList.get(i))) {
                reviewIdx = i; loadReviewRecord(); return;
            }
        }

        // All done
        Messagebox.show(
                "All Date & Amount reviews completed.\nProceed to Step 3: Payee & Account?",
                "Step 2 Complete", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
                ev -> {
                    if (Messagebox.ON_YES.equals(ev.getName())) {
                        setSessionMaxStep(3);
                        Executions.getCurrent().sendRedirect(PAGE_STEP3 + batchParam());
                    } else {
                        showList();
                    }
                });
    }

    private boolean isPending(InwardCheque c) {
        String s = c.getRepairStatus();
        return s == null || "NEEDS_REPAIR".equalsIgnoreCase(s)
                || "REFERRED_BACK".equalsIgnoreCase(s);
    }

    // ── Wizard navigation ─────────────────────────────────────────────────

    @Listen("onClick=#btnStep1")
    public void onStep1() {
        Executions.getCurrent().sendRedirect(PAGE_STEP1 + batchParam());
    }

    @Listen("onClick=#btnStep2")
    public void onStep2() { /* already here */ }

    @Listen("onClick=#btnStep3")
    public void onStep3() {
        if (getSessionMaxStep() >= 3)
            Executions.getCurrent().sendRedirect(PAGE_STEP3 + batchParam());
    }

    @Listen("onClick=#btnBackToStep1")
    public void onBackToStep1() {
        Executions.getCurrent().sendRedirect(PAGE_STEP1 + batchParam());
    }

    @Listen("onClick=#btnNextStep3")
    public void onNextStep3() {
        setSessionMaxStep(3);
        Executions.getCurrent().sendRedirect(PAGE_STEP3 + batchParam());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private List<InwardCheque> getFilteredList() {
        if (allCheques == null) return Collections.emptyList();
        final String filterVal =
                (cmbFilter != null && cmbFilter.getSelectedItem() != null
                 && cmbFilter.getSelectedItem().getValue() != null)
                        ? cmbFilter.getSelectedItem().getValue().toString() : "";
        return allCheques.stream()
                .filter(c -> filterVal.isEmpty()
                        || filterVal.equalsIgnoreCase(
                                c.getRepairStatus() != null ? c.getRepairStatus() : ""))
                .collect(Collectors.toList());
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil((double) getFilteredList().size() / PAGE_SIZE));
    }

    private void updateBatchBadge() {
        if (lblBatchBadge != null)
            lblBatchBadge.setValue("BATCH: " + currentBatchId);
    }

    private void updatePendingBadge() {
        if (lblPendingBadge == null || allCheques == null) return;
        long pending = allCheques.stream().filter(this::isPending).count();
        lblPendingBadge.setValue(pending + " PENDING");
    }

    private void addCell(Listitem row, String text) {
        row.appendChild(new Listcell(text != null ? text : "—"));
    }

    private String nvl(String v) {
        return (v != null && !v.isBlank()) ? v : "—";
    }

    private String fmt(BigDecimal v) {
        return v != null ? String.format("%,.2f", v) : "—";
    }

    private String batchParam() {
        return currentBatchId != null ? "?batchId=" + currentBatchId : "";
    }

    private int getSessionMaxStep() {
        Object v = Sessions.getCurrent().getAttribute(SESSION_MAX_STEP);
        return (v instanceof Integer i) ? i : 2;
    }

    private void setSessionMaxStep(int step) {
        if (step > getSessionMaxStep())
            Sessions.getCurrent().setAttribute(SESSION_MAX_STEP, step);
    }

    private String resolveStatusLabel(String s) {
        if (s == null || s.isEmpty()) return "NEEDS REPAIR";
        return switch (s.toUpperCase()) {
            case "REPAIRED"      -> "REPAIRED";
            case "REFERRED_BACK" -> "REFERRED BACK";
            case "REJECTED"      -> "REJECTED";
            default              -> "NEEDS REPAIR";
        };
    }

    private String resolveStatusSclass(String s) {
        if (s == null || s.isEmpty()) return "badge-needs-repair";
        return switch (s.toUpperCase()) {
            case "REPAIRED"      -> "badge-repaired";
            case "REFERRED_BACK" -> "badge-referred";
            case "REJECTED"      -> "badge-fail";
            default              -> "badge-needs-repair";
        };
    }

    /**
     * Simple amount-in-words helper (handles up to crores for Indian banking context).
     * For production use, replace with a proper library or utility.
     */
    private String amountInWords(BigDecimal amount) {
        if (amount == null) return "—";
        long val = amount.longValue();
        if (val == 0) return "Zero Only";
        // Basic implementation — replace with full utility if needed
        return toWords(val) + " Only";
    }

    private static final String[] ONES = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
        "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty",
        "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private String toWords(long n) {
        if (n < 20)        return ONES[(int) n];
        if (n < 100)       return TENS[(int)(n / 10)] + (n % 10 != 0 ? " " + ONES[(int)(n % 10)] : "");
        if (n < 1000)      return ONES[(int)(n / 100)] + " Hundred" + (n % 100 != 0 ? " " + toWords(n % 100) : "");
        if (n < 100000)    return toWords(n / 1000) + " Thousand" + (n % 1000 != 0 ? " " + toWords(n % 1000) : "");
        if (n < 10000000)  return toWords(n / 100000) + " Lakh" + (n % 100000 != 0 ? " " + toWords(n % 100000) : "");
        return toWords(n / 10000000) + " Crore" + (n % 10000000 != 0 ? " " + toWords(n % 10000000) : "");
    }
}