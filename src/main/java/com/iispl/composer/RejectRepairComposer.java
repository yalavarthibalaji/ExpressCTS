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
 *  - Inline accordion detail row on row click AND repair button click
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

    @Wire("#emptyStatePanel")       private Div      emptyStatePanel;
    @Wire("#batchListPanel")        private Div      batchListPanel;

    @Wire("#btnStep1")              private Button   btnStep1;
    @Wire("#btnStep2")              private Button   btnStep2;
    @Wire("#btnStep3")              private Button   btnStep3;

    @Wire("#conn1")                 private Div      conn1;
    @Wire("#conn2")                 private Div      conn2;

    @Wire("#lblStep2Num")           private Label    lblStep2Num;
    @Wire("#lblStep2Desc")          private Label    lblStep2Desc;
    @Wire("#lblStep3Num")           private Label    lblStep3Num;
    @Wire("#lblStep3Desc")          private Label    lblStep3Desc;

    @Wire("#chequeListbox")         private Listbox  chequeListbox;

    @Wire("#lblBatchBadge")         private Label    lblBatchBadge;
    @Wire("#lblPendingBadge")       private Label    lblPendingBadge;
    @Wire("#lblPageInfo")           private Label    lblPageInfo;

    @Wire("#cmbFilter")             private Combobox cmbFilter;
    @Wire("#txtSearch")             private Textbox  txtSearch;

    @Wire("#btnGoToFileProcessing") private Button   btnGoToFileProcessing;
    @Wire("#btnBackToBatches")      private Button   btnBackToBatches;
    @Wire("#btnNextStep2")          private Button   btnNextStep2;
    @Wire("#btnPrevPage")           private Button   btnPrevPage;
    @Wire("#btnNextPage")           private Button   btnNextPage;

    // ── Service ───────────────────────────────────────────────────────────

    private final RejectRepairService rejectRepairService =
            new RejectRepairServiceImpl();

    // ── State ─────────────────────────────────────────────────────────────

    private String             currentBatchId;
    private List<InwardCheque> allCheques;

    // ── Accordion State ───────────────────────────────────────────────────

    private Listitem currentExpandedItem = null;
    private Listitem currentDetailItem   = null;

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

        if (conn1 != null)
            conn1.setSclass(CURRENT_STEP >= 2 ? "step-connector filled" : "step-connector");
        if (conn2 != null)
            conn2.setSclass(CURRENT_STEP >= 3 ? "step-connector filled" : "step-connector");

        applyLabelStyle(lblStep2Num, lblStep2Desc, 2);
        applyLabelStyle(lblStep3Num, lblStep3Desc, 3);
    }

    private void applyStepStyle(Button button, int stepNo, int maxStep) {
        if (button == null) return;
        if      (stepNo == CURRENT_STEP)  button.setSclass("step-circle-btn active");
        else if (stepNo <  CURRENT_STEP)  button.setSclass("step-circle-btn completed");
        else if (stepNo <= maxStep)       button.setSclass("step-circle-btn active");
        else                              button.setSclass("step-circle-btn disabled-step");
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
        if (targetStep > getSessionMaxStep()) return;
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
                if (batches == null || batches.isEmpty()) { showEmptyState(); return; }
                currentBatchId = batches.get(0).getBatchId();
                Sessions.getCurrent().setAttribute(SESSION_BATCH_ID, currentBatchId);
            }
        }

        List<InwardCheque> fetched = rejectRepairService.getChequesByBatchId(currentBatchId);
        if (fetched == null || fetched.isEmpty()) { showEmptyState(); return; }

        allCheques = fetched.stream()
                .filter(c -> c.isMicrError()
                          || "NEEDS_REPAIR".equalsIgnoreCase(c.getRepairStatus())
                          || "REFERRED_BACK".equalsIgnoreCase(c.getRepairStatus()))
                .collect(Collectors.toList());

        if (allCheques.isEmpty()) { showEmptyState(); return; }

        showChequeList();
    }

    // =====================================================================
    // RUNTIME EVENT WIRING
    // =====================================================================

    private void wireEvents() {
        if (cmbFilter != null) {
            cmbFilter.addEventListener(Events.ON_SELECT, e -> {
                currentPage = 1;
                collapseDetail();
                renderTable();
            });
        }
        if (txtSearch != null) {
            txtSearch.addEventListener(Events.ON_CHANGING, e -> {
                currentPage = 1;
                collapseDetail();
                renderTable();
            });
        }
        if (btnPrevPage != null) {
            btnPrevPage.addEventListener(Events.ON_CLICK, e -> {
                if (currentPage > 1) { currentPage--; collapseDetail(); renderTable(); }
            });
        }
        if (btnNextPage != null) {
            btnNextPage.addEventListener(Events.ON_CLICK, e -> {
                if (currentPage < totalPages()) { currentPage++; collapseDetail(); renderTable(); }
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

        currentExpandedItem = null;
        currentDetailItem   = null;

        List<InwardCheque> filtered  = getFilteredList();
        int total      = filtered.size();
        int totalPages = totalPages(total);
        currentPage    = Math.min(currentPage, Math.max(1, totalPages));

        int fromIdx = (currentPage - 1) * PAGE_SIZE;
        int toIdx   = Math.min(fromIdx + PAGE_SIZE, total);

        chequeListbox.getItems().clear();

        int rowNum = fromIdx + 1;
        for (InwardCheque c : filtered.subList(fromIdx, toIdx)) {
            appendChequeRow(c, rowNum++);
        }

        if (lblPageInfo != null)
            lblPageInfo.setValue("Page " + currentPage + " of " + totalPages + " | " + total + " records");
        if (btnPrevPage != null) btnPrevPage.setDisabled(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisabled(currentPage >= totalPages);

        long pending = allCheques.stream()
                .filter(c -> "NEEDS_REPAIR".equalsIgnoreCase(c.getRepairStatus())
                          || (c.getRepairStatus() == null && c.isMicrError()))
                .count();
        if (lblPendingBadge != null)
            lblPendingBadge.setValue(pending + " PENDING");
    }

    // ── Build one cheque data row ─────────────────────────────────────────

    private void appendChequeRow(InwardCheque c, int sno) {
        Listitem row = new Listitem();
        row.setValue(c);
        row.setSclass("clickable-row");

        // ── Sno cell with chevron ──
        Listcell cellSno = new Listcell();
        Label chevron = new Label("▶");
        chevron.setSclass("row-chevron");
        cellSno.appendChild(chevron);
        cellSno.appendChild(new Label(String.valueOf(sno)));
        row.appendChild(cellSno);

        // ── Data cells ──
        addCell(row, nvl(c.getChequeNo()));
        addCell(row, nvl(c.getPresentingBankName()));
        addCell(row, nvl(c.getMicrCodeRaw()));

        // Amount — right aligned
        Listcell amtCell = new Listcell(
                c.getAmount() != null ? "₹ " + formatAmt(c.getAmount()) : "—");
        amtCell.setStyle("text-align:right");
        row.appendChild(amtCell);

        // Repair Status badge
        Listcell statusCell = new Listcell();
        statusCell.setStyle("text-align:center");
        Label badge = new Label(resolveStatusLabel(c.getRepairStatus()));
        badge.setSclass(resolveStatusSclass(c.getRepairStatus()));
        statusCell.appendChild(badge);
        row.appendChild(statusCell);

        // ── Action cell ──
        // CHANGED: Repair button now opens inline accordion instead of redirecting
        Listcell actionCell = new Listcell();
        actionCell.setStyle("text-align:center");

        if (c.isMicrError()) {
            Button repairBtn = new Button("Repair");
            repairBtn.setSclass("btn-repair-row");
            repairBtn.addEventListener(Events.ON_CLICK, ev -> {
                ev.stopPropagation();                        // don't bubble to row click
                toggleDetailRow(row, c, chevron);           // same accordion as row click
            });
            actionCell.appendChild(repairBtn);
        } else {
            Label noErr = new Label("No Error");
            noErr.setSclass("action-no-error");
            actionCell.appendChild(noErr);
        }
        row.appendChild(actionCell);

        // ── Row click → same accordion toggle ──
        row.addEventListener(Events.ON_CLICK,
                event -> toggleDetailRow(row, c, chevron));

        chequeListbox.appendChild(row);
    }

    // =====================================================================
    // ACCORDION — TOGGLE INLINE DETAIL ROW
    // =====================================================================

    /**
     * Called by BOTH row click and Repair button click.
     * If the row is already open → collapse.
     * If a different row is open → close it, open this one.
     */
    private void toggleDetailRow(Listitem clickedRow,
                                  InwardCheque cheque,
                                  Label chevron) {

        // Same row clicked again → collapse
        if (clickedRow.equals(currentExpandedItem)) {
            collapseDetail();
            return;
        }

        // Close any previously opened detail
        collapseDetail();

        // Mark clicked row expanded
        currentExpandedItem = clickedRow;
        clickedRow.setSclass("clickable-row row-expanded");
        chevron.setSclass("row-chevron open");

        // ── Build injected detail Listitem ──
        Listitem detailItem = new Listitem();
        detailItem.setSclass("cheque-detail-row");

        // Single cell spanning all 7 columns
        Listcell cell = new Listcell();
        cell.setSpan(7);

        Div wrap = new Div();
        wrap.setSclass("detail-inline-wrap");

        // Section 1 — MICR Information
        wrap.appendChild(buildSection("MICR Information", new String[][]{
            {"MICR Code (Raw)",       nvl(cheque.getMicrCodeRaw()),       "mono"},
            {"MICR Code (Corrected)", nvl(cheque.getMicrCodeCorrected()), "mono"},
            {"Bank Code",             nvl(cheque.getBankCode()),          ""},
            {"Branch Code",           nvl(cheque.getBranchCode()),        ""},
            {"Transaction Code",      nvl(cheque.getCityCode()),          ""}
        }));

        // Section 2 — Amount & Date
        wrap.appendChild(buildSection("Amount & Date", new String[][]{
            {"Amount (Figures)",
             cheque.getAmount() != null ? "₹ " + formatAmt(cheque.getAmount()) : "—",
             "detail-field-value highlight-amount"},
            {"Amount (Words)", nvl(cheque.getAmountInWords()), ""},
            {"Cheque Date",
             cheque.getChequeDate() != null ? cheque.getChequeDate().toString() : "—", ""}
        }));

        // Section 3 — Bank & Account
        wrap.appendChild(buildSection("Bank & Account", new String[][]{
            {"Presenting Bank",    nvl(cheque.getPresentingBankName()),  ""},
            {"Instrument Type",    nvl(cheque.getIqaStatus()),           ""},
            {"Drawee Account No.", nvl(cheque.getDraweeAccountNumber()), "mono"},
            {"Drawee Bank",        nvl(cheque.getDraweeAccountHolder()), ""}
        }));

        // Section 4 — Payee & Status
        wrap.appendChild(buildSection("Payee & Status", new String[][]{
            {"Payee Name",    nvl(cheque.getPayeeName()),                   ""},
            {"Batch ID",      nvl(currentBatchId),                         "mono"},
            {"Repair Status", resolveStatusLabel(cheque.getRepairStatus()), ""}
        }));

        // ── Close strip ──
        Div closeStrip = new Div();
        closeStrip.setSclass("detail-inline-close");
        Button btnClose = new Button("✕ Close");
        btnClose.setSclass("btn-cancel");
        btnClose.addEventListener(Events.ON_CLICK, e -> collapseDetail());
        closeStrip.appendChild(btnClose);
        wrap.appendChild(closeStrip);

        cell.appendChild(wrap);
        detailItem.appendChild(cell);

        // Insert immediately after the clicked row
        Component nextSibling = clickedRow.getNextSibling();
        if (nextSibling != null) {
            chequeListbox.insertBefore(detailItem, nextSibling);
        } else {
            chequeListbox.appendChild(detailItem);
        }

        currentDetailItem = detailItem;
    }

    // ── Collapse currently open accordion row ─────────────────────────────

    private void collapseDetail() {
        if (currentDetailItem != null) {
            currentDetailItem.detach();
            currentDetailItem = null;
        }
        if (currentExpandedItem != null) {
            currentExpandedItem.setSclass("clickable-row");
            // Reset chevron in first Listcell
            currentExpandedItem.getChildren().stream()
                .filter(c -> c instanceof Listcell)
                .findFirst()
                .ifPresent(lc ->
                    lc.getChildren().stream()
                        .filter(ch -> ch instanceof Label
                            && ((Label) ch).getSclass() != null
                            && ((Label) ch).getSclass().contains("row-chevron"))
                        .findFirst()
                        .ifPresent(lbl -> ((Label) lbl).setSclass("row-chevron"))
                );
            currentExpandedItem = null;
        }
    }

    // ── Build one detail section (title + field rows) ─────────────────────

    private Div buildSection(String title, String[][] fields) {
        Div section = new Div();
        section.setSclass("detail-inline-section");

        Label heading = new Label(title);
        heading.setSclass("detail-inline-section-title");
        section.appendChild(heading);

        for (String[] f : fields) {
            Div fieldDiv = new Div();
            fieldDiv.setSclass("detail-inline-field");

            Label lbl = new Label(f[0]);
            lbl.setSclass("detail-field-label");

            Label val = new Label(f[1]);
            String valSclass = f[2].contains("detail-field-value")
                    ? f[2]
                    : ("detail-field-value " + f[2]).trim();
            val.setSclass(valSclass);

            fieldDiv.appendChild(lbl);
            fieldDiv.appendChild(val);
            section.appendChild(fieldDiv);
        }
        return section;
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
                    "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        setSessionMaxStep(2);
        Executions.getCurrent().sendRedirect(PAGE_STEP2 + buildBatchParam());
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
        if (step > current)
            Sessions.getCurrent().setAttribute(SESSION_MAX_STEP, step);
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

    private int totalPages() { return totalPages(getFilteredList().size()); }

    private int totalPages(int total) {
        return Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
    }

    private void addCell(Listitem row, String text) {
        row.appendChild(new Listcell(text != null ? text : "—"));
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