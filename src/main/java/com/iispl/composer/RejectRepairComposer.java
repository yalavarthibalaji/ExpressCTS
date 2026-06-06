package com.iispl.composer;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.RejectRepairService;
import com.iispl.serviceImpl.RejectRepairServiceImpl;

/**
 * RejectRepairComposer — Step 1: Reject & Repair
 *
 * Responsibilities:
 *  - Wizard bar rendering and step navigation
 *  - Load cheques for the selected inward batch
 *  - Render cheque table with pagination, filter, and search
 *  - Show cheque detail panel on row click
 *  - Validate all repairs before allowing Step 2
 */
public class RejectRepairComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // ── Step Constants ────────────────────────────────────────────────────

    private static final int    CURRENT_STEP     = 1;
    private static final String SESSION_MAX_STEP = "cts_inward_max_step";
    private static final String SESSION_BATCH_ID = "cts_inward_batch_id";

    private static final String PAGE_STEP1 = "/inward/inwardMicr/RejectRepair.zul";
    private static final String PAGE_STEP2 = "/inward/inwardMicr/DateAmount.zul";
    private static final String PAGE_STEP3 = "/inward/inwardMicr/PayeeAccount.zul";
    private static final String PAGE_FILE  = "/inward/bpxfUpload/bpxfUpload.zul";
    private static final String PAGE_BATCH = "/inward/inwardMicr/batchSelect.zul";

    // ── Pagination ────────────────────────────────────────────────────────

    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;

    // ── Wired Components ──────────────────────────────────────────────────

    // Panels
    @Wire("#emptyStatePanel")        private Div      emptyStatePanel;
    @Wire("#batchListPanel")         private Div      batchListPanel;
    @Wire("#chequeDetailPanel")      private Div      chequeDetailPanel;

    // Wizard step buttons
    @Wire("#btnStep1")               private Button   btnStep1;
    @Wire("#btnStep2")               private Button   btnStep2;
    @Wire("#btnStep3")               private Button   btnStep3;

    // Wizard connectors
    @Wire("#conn1")                  private Div      conn1;
    @Wire("#conn2")                  private Div      conn2;

    // Wizard step labels
    @Wire("#lblStep2Num")            private Label    lblStep2Num;
    @Wire("#lblStep2Desc")           private Label    lblStep2Desc;
    @Wire("#lblStep3Num")            private Label    lblStep3Num;
    @Wire("#lblStep3Desc")           private Label    lblStep3Desc;

    // Table
    @Wire("#chequeListbox")          private Listbox  chequeListbox;

    // Badges / Info
    @Wire("#lblBatchBadge")          private Label    lblBatchBadge;
    @Wire("#lblPendingBadge")        private Label    lblPendingBadge;
    @Wire("#lblPageInfo")            private Label    lblPageInfo;

    // Filter / Search
    @Wire("#cmbFilter")              private Combobox cmbFilter;
    @Wire("#txtSearch")              private Textbox  txtSearch;

    // Navigation buttons
    @Wire("#btnGoToFileProcessing")  private Button   btnGoToFileProcessing;
    @Wire("#btnBackToBatches")       private Button   btnBackToBatches;
    @Wire("#btnNextStep2")           private Button   btnNextStep2;
    @Wire("#btnPrevPage")            private Button   btnPrevPage;
    @Wire("#btnNextPage")            private Button   btnNextPage;

    // Detail panel — close button
    @Wire("#btnCloseDetail")         private Button   btnCloseDetail;

    // Detail panel — labels
    @Wire("#lblDetailChequeNo")      private Label    lblDetailChequeNo;
    @Wire("#lblMicrCodeRaw")         private Label    lblMicrCodeRaw;
    @Wire("#lblMicrCodeProcessed")   private Label    lblMicrCodeProcessed;
    @Wire("#lblBankCode")            private Label    lblBankCode;
    @Wire("#lblBranchCode")          private Label    lblBranchCode;
    @Wire("#lblTransactionCode")     private Label    lblTransactionCode;
    @Wire("#lblAmountFigures")       private Label    lblAmountFigures;
    @Wire("#lblAmountWords")         private Label    lblAmountWords;
    @Wire("#lblChequeDate")          private Label    lblChequeDate;
    @Wire("#lblPresentingBank")      private Label    lblPresentingBank;
    @Wire("#lblInstrumentType")      private Label    lblInstrumentType;
    @Wire("#lblAccountNo")           private Label    lblAccountNo;
    @Wire("#lblDraweeBank")          private Label    lblDraweeBank;
    @Wire("#lblPayeeName")           private Label    lblPayeeName;
    @Wire("#lblDetailBatchId")       private Label    lblDetailBatchId;
    @Wire("#lblDetailRepairStatus")  private Label    lblDetailRepairStatus;

    // ── Service ───────────────────────────────────────────────────────────

    private final RejectRepairService rejectRepairService =
            new RejectRepairServiceImpl();

    // ── State ─────────────────────────────────────────────────────────────

    private String             currentBatchId;
    private List<InwardCheque> allCheques;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        initWizardBar();
        loadPage();
        wireEvents();
    }

    // =====================================================================
    // WIZARD BAR
    // =====================================================================

    private void initWizardBar() {
        int maxStep = getSessionMaxStep();

        applyStepStyle(btnStep1, 1, maxStep);
        applyStepStyle(btnStep2, 2, maxStep);
        applyStepStyle(btnStep3, 3, maxStep);

        if (conn1 != null) {
            conn1.setSclass(CURRENT_STEP >= 2 ? "step-connector filled" : "step-connector");
        }
        if (conn2 != null) {
            conn2.setSclass(CURRENT_STEP >= 3 ? "step-connector filled" : "step-connector");
        }

        applyLabelStyle(lblStep2Num, lblStep2Desc, 2);
        applyLabelStyle(lblStep3Num, lblStep3Desc, 3);
    }

    private void applyStepStyle(Button button, int stepNo, int maxStep) {
        if (button == null) return;
        if (stepNo == CURRENT_STEP) {
            button.setSclass("step-circle-btn active");
        } else if (stepNo < CURRENT_STEP) {
            button.setSclass("step-circle-btn completed");
        } else if (stepNo <= maxStep) {
            button.setSclass("step-circle-btn active");
        } else {
            button.setSclass("step-circle-btn disabled-step");
        }
    }

    private void applyLabelStyle(Label numLabel, Label descLabel, int stepNo) {
        if (numLabel == null) return;
        if (stepNo == CURRENT_STEP) {
            numLabel.setSclass("step-num active");
            if (descLabel != null) descLabel.setSclass("step-desc active");
        } else if (stepNo < CURRENT_STEP) {
            numLabel.setSclass("step-num completed");
            if (descLabel != null) descLabel.setSclass("step-desc completed");
        } else {
            numLabel.setSclass("step-num");
            if (descLabel != null) descLabel.setSclass("step-desc");
        }
    }

    // =====================================================================
    // STEP NAVIGATION
    // =====================================================================

    @Listen("onClick=#btnStep1")
    public void onStep1() { navigateToStep(1); }

    @Listen("onClick=#btnStep2")
    public void onStep2() { navigateToStep(2); }

    @Listen("onClick=#btnStep3")
    public void onStep3() { navigateToStep(3); }

    private void navigateToStep(int targetStep) {
        if (targetStep == CURRENT_STEP) return;
        if (targetStep > getSessionMaxStep()) return;   // not yet unlocked

        switch (targetStep) {
            case 1 -> Executions.getCurrent().sendRedirect(PAGE_STEP1);
            case 2 -> Executions.getCurrent().sendRedirect(PAGE_STEP2 + buildBatchParam());
            case 3 -> Executions.getCurrent().sendRedirect(PAGE_STEP3 + buildBatchParam());
        }
    }

    // =====================================================================
    // DATA LOAD
    // =====================================================================

    private void loadPage() {
        // Priority: URL param → session → first eligible batch
        String paramBatch = Executions.getCurrent().getParameter("batchId");

        if (paramBatch != null && !paramBatch.trim().isEmpty()) {
            currentBatchId = paramBatch.trim();
            Sessions.getCurrent().setAttribute(SESSION_BATCH_ID, currentBatchId);

        } else {
            Object sessionBatch = Sessions.getCurrent().getAttribute(SESSION_BATCH_ID);
            if (sessionBatch != null) {
                currentBatchId = sessionBatch.toString();
            } else {
                List<InwardBatch> batches = rejectRepairService.getRepairEligibleBatches();
                if (batches == null || batches.isEmpty()) {
                    showEmptyState();
                    return;
                }
                currentBatchId = batches.get(0).getBatchId();
                Sessions.getCurrent().setAttribute(SESSION_BATCH_ID, currentBatchId);
            }
        }

        List<InwardCheque> fetched = rejectRepairService.getChequesByBatchId(currentBatchId);

        if (fetched == null || fetched.isEmpty()) {
            showEmptyState();
            return;
        }

        // Show only cheques that actually need repair — skip clean ones
        allCheques = fetched.stream()
                .filter(c -> c.isMicrError()
                          || "NEEDS_REPAIR".equalsIgnoreCase(c.getRepairStatus())
                          || "REFERRED_BACK".equalsIgnoreCase(c.getRepairStatus()))
                .collect(Collectors.toList());

        if (allCheques.isEmpty()) {
            showEmptyState();
            return;
        }

        showChequeList();
    }
    // =====================================================================
    // RUNTIME EVENT WIRING
    // =====================================================================

    private void wireEvents() {
        if (cmbFilter != null) {
            cmbFilter.addEventListener(Events.ON_SELECT, e -> {
                currentPage = 1;
                renderTable();
            });
        }
        if (txtSearch != null) {
            txtSearch.addEventListener(Events.ON_CHANGING, e -> {
                currentPage = 1;
                renderTable();
            });
        }
        if (btnPrevPage != null) {
            btnPrevPage.addEventListener(Events.ON_CLICK, e -> {
                if (currentPage > 1) {
                    currentPage--;
                    renderTable();
                }
            });
        }
        if (btnNextPage != null) {
            btnNextPage.addEventListener(Events.ON_CLICK, e -> {
                if (currentPage < totalPages()) {
                    currentPage++;
                    renderTable();
                }
            });
        }
    }

    // =====================================================================
    // FILTER
    // =====================================================================

    private List<InwardCheque> getFilteredList() {
        if (allCheques == null) return Collections.emptyList();

        final String filterVal;
        if (cmbFilter != null && cmbFilter.getSelectedItem() != null) {
            Object v = cmbFilter.getSelectedItem().getValue();
            filterVal = (v != null) ? v.toString() : "";
        } else {
            filterVal = "";
        }

        final String searchTerm =
                (txtSearch != null && txtSearch.getValue() != null)
                        ? txtSearch.getValue().trim().toLowerCase() : "";

        return allCheques.stream()
                .filter(c -> {
                    if (!filterVal.isEmpty()) {
                        String rs = c.getRepairStatus() != null ? c.getRepairStatus() : "";
                        if (!rs.equalsIgnoreCase(filterVal)) return false;
                    }
                    if (!searchTerm.isEmpty()) {
                        String chqNo = c.getChequeNo() != null
                                ? c.getChequeNo().toLowerCase() : "";
                        String bank  = c.getPresentingBankName() != null
                                ? c.getPresentingBankName().toLowerCase() : "";
                        return chqNo.contains(searchTerm) || bank.contains(searchTerm);
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // =====================================================================
    // TABLE RENDERING
    // =====================================================================

    private void renderTable() {
        if (chequeListbox == null) return;

        List<InwardCheque> filtered  = getFilteredList();
        int total      = filtered.size();
        int totalPages = totalPages(total);
        currentPage    = Math.min(currentPage, Math.max(1, totalPages));

        int fromIdx  = (currentPage - 1) * PAGE_SIZE;
        int toIdx    = Math.min(fromIdx + PAGE_SIZE, total);
        List<InwardCheque> pageData = filtered.subList(fromIdx, toIdx);

        chequeListbox.getItems().clear();

        int rowNum = fromIdx + 1;

        for (InwardCheque c : pageData) {
            Listitem row = new Listitem();
            row.setSclass("clickable-row");

            final InwardCheque cheque = c;
            row.addEventListener(Events.ON_CLICK, ev -> showChequeDetail(cheque));

            // 1  #
            addCell(row, String.valueOf(rowNum++));

            // 2  Cheque No.
            addCell(row, nvl(c.getChequeNo()));

            // 3  Presenting Bank
            addCell(row, nvl(c.getPresentingBankName()));

            // 4  MICR Code (raw)
            addCell(row, nvl(c.getMicrCodeRaw()));

            // 5  Amount — right aligned
            Listcell amtCell = new Listcell(
                    c.getAmount() != null ? "₹ " + formatAmt(c.getAmount()) : "—");
            amtCell.setStyle("text-align:right");
            row.appendChild(amtCell);

            // 6  Repair Status badge
            Listcell statusCell = new Listcell();
            statusCell.setStyle("text-align:center");
            Label badge = new Label(resolveStatusLabel(c.getRepairStatus()));
            badge.setSclass(resolveStatusSclass(c.getRepairStatus()));
            statusCell.appendChild(badge);
            row.appendChild(statusCell);

            // 7  Action button
            Listcell actionCell = new Listcell();
            actionCell.setStyle("text-align:center");
            if (c.isMicrError()) {
                Button repairBtn = new Button("Repair");
                repairBtn.setSclass("btn-repair-row");

                final Long   chequeId = c.getId();        // ← FIXED: was getChequeId()

                final String batchId  = currentBatchId;
                repairBtn.addEventListener(Events.ON_CLICK, ev -> {
                    ev.stopPropagation();
                    openRepairPage(chequeId, batchId);
                });
                actionCell.appendChild(repairBtn);
            } else {
                Label noErr = new Label("No Error");
                noErr.setSclass("action-no-error");
                actionCell.appendChild(noErr);
            }
            row.appendChild(actionCell);

            chequeListbox.appendChild(row);
        }

        // Pagination info
        if (lblPageInfo != null) {
            lblPageInfo.setValue(
                    "Page " + currentPage + " of " + totalPages + " | " + total + " records");
        }
        if (btnPrevPage != null) btnPrevPage.setDisabled(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisabled(currentPage >= totalPages);

        // Pending badge
        long pending = allCheques.stream()
                .filter(c -> "NEEDS_REPAIR".equalsIgnoreCase(c.getRepairStatus())
                          || (c.getRepairStatus() == null && c.isMicrError()))
                .count();
        if (lblPendingBadge != null) {
            lblPendingBadge.setValue(pending + " PENDING");
        }
    }

    // =====================================================================
    // CHEQUE DETAIL PANEL
    // =====================================================================

    @Listen("onClick=#btnCloseDetail")
    public void onCloseDetail() {
        if (chequeDetailPanel != null) {
            chequeDetailPanel.setVisible(false);
        }
    }

    private void showChequeDetail(InwardCheque c) {
        if (chequeDetailPanel == null) return;

        // Header
        setLbl(lblDetailChequeNo, "CHQ: " + nvl(c.getChequeNo()));

        // MICR
        setLbl(lblMicrCodeRaw,       nvl(c.getMicrCodeRaw()));
        setLbl(lblMicrCodeProcessed, nvl(c.getMicrCodeCorrected()));
        setLbl(lblBankCode,          nvl(c.getBankCode()));
        setLbl(lblBranchCode,        nvl(c.getBranchCode()));
        setLbl(lblTransactionCode,   nvl(c.getCityCode()));

        // Amount & Date
        setLbl(lblAmountFigures,
               c.getAmount() != null ? "₹ " + formatAmt(c.getAmount()) : "—");
        setLbl(lblAmountWords, nvl(c.getAmountInWords()));
        setLbl(lblChequeDate,
               c.getChequeDate() != null ? c.getChequeDate().toString() : "—");

        // Bank & Account
        setLbl(lblPresentingBank, nvl(c.getPresentingBankName()));
        setLbl(lblInstrumentType, nvl(c.getIqaStatus()));
        setLbl(lblAccountNo,      nvl(c.getDraweeAccountNumber()));
        setLbl(lblDraweeBank,     nvl(c.getDraweeAccountHolder()));

        // Payee & Status
        setLbl(lblPayeeName,          nvl(c.getPayeeName()));
        setLbl(lblDetailBatchId,      nvl(currentBatchId));
        setLbl(lblDetailRepairStatus, resolveStatusLabel(c.getRepairStatus()));

        chequeDetailPanel.setVisible(true);
        Clients.scrollIntoView(chequeDetailPanel);
    }

    // =====================================================================
    // NAVIGATION BUTTONS
    // =====================================================================

    @Listen("onClick=#btnGoToFileProcessing")
    public void onGoToFileProcessing() {
        Executions.getCurrent().sendRedirect(PAGE_FILE);
    }

    @Listen("onClick=#btnBackToBatches")
    public void onBackToBatches() {
        Sessions.getCurrent().removeAttribute(SESSION_BATCH_ID);
        Executions.getCurrent().sendRedirect(PAGE_BATCH);
    }

    @Listen("onClick=#btnNextStep2")
    public void onNextStep2() {
        if (currentBatchId == null) return;

        if (!allRepairsDone()) {
            Messagebox.show(
                    "Please complete all MICR repairs before proceeding to Step 2.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);
            return;
        }

        setSessionMaxStep(2);
        Executions.getCurrent().sendRedirect(PAGE_STEP2 + buildBatchParam());
    }

    // =====================================================================
    // REPAIR DETAIL PAGE
    // =====================================================================

    private void openRepairPage(Long chequeId, String batchId) {
        Executions.getCurrent().sendRedirect(
                "/inward/inwardMicr/MicrRepairDetail.zul"
                + "?chequeId=" + chequeId
                + "&batchId=" + batchId);
    }

    // =====================================================================
    // VISIBILITY HELPERS
    // =====================================================================

    private void showEmptyState() {
        if (emptyStatePanel != null) emptyStatePanel.setVisible(true);
        if (batchListPanel  != null) batchListPanel.setVisible(false);
    }

    private void showChequeList() {
        if (emptyStatePanel != null) emptyStatePanel.setVisible(false);
        if (batchListPanel  != null) batchListPanel.setVisible(true);
        if (lblBatchBadge   != null) lblBatchBadge.setValue("BATCH: " + currentBatchId);
        currentPage = 1;
        renderTable();
    }

    // =====================================================================
    // SESSION HELPERS
    // =====================================================================

    private int getSessionMaxStep() {
        Object val = Sessions.getCurrent().getAttribute(SESSION_MAX_STEP);
        if (val instanceof Integer) return (Integer) val;
        return CURRENT_STEP;
    }

    private void setSessionMaxStep(int step) {
        int current = getSessionMaxStep();
        if (step > current) {
            Sessions.getCurrent().setAttribute(SESSION_MAX_STEP, step);
        }
    }

    // =====================================================================
    // VALIDATION
    // =====================================================================

    private boolean allRepairsDone() {
        if (allCheques == null || allCheques.isEmpty()) return false;
        return allCheques.stream()
                .noneMatch(c -> "NEEDS_REPAIR".equalsIgnoreCase(c.getRepairStatus())
                             || (c.getRepairStatus() == null && c.isMicrError()));
    }

    // =====================================================================
    // UTILITIES
    // =====================================================================

    private String buildBatchParam() {
        return currentBatchId != null ? "?batchId=" + currentBatchId : "";
    }

    private int totalPages() {
        return totalPages(getFilteredList().size());
    }

    private int totalPages(int total) {
        return Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
    }

    private void addCell(Listitem row, String text) {
        row.appendChild(new Listcell(text != null ? text : "—"));
    }

    private void setLbl(Label lbl, String value) {
        if (lbl != null) lbl.setValue(value != null ? value : "—");
    }

    private String nvl(String val) {
        return (val != null && !val.isEmpty()) ? val : "—";
    }

    private String formatAmt(BigDecimal amt) {
        return String.format("%,.2f", amt);
    }

    private String resolveStatusLabel(String status) {
        if (status == null || status.isEmpty()) return "NEEDS REPAIR";
        return switch (status.toUpperCase()) {
            case "REPAIRED"      -> "REPAIRED";
            case "REFERRED_BACK" -> "REFERRED BACK";
            case "NEEDS_REPAIR"  -> "NEEDS REPAIR";
            default              -> status;
        };
    }

    private String resolveStatusSclass(String status) {
        if (status == null || status.isEmpty()) return "badge-needs-repair";
        return switch (status.toUpperCase()) {
            case "REPAIRED"      -> "badge-repaired";
            case "REFERRED_BACK" -> "badge-referred";
            default              -> "badge-needs-repair";
        };
    }
}