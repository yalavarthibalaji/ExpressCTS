package com.iispl.composer;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
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
import org.zkoss.zul.Image;
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

public class RejectRepairComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // ── Constants ──────────────────────────────────────────────────────────
    private static final int    CURRENT_STEP      = 1;
    private static final String SESSION_MAX_STEP  = "cts_inward_max_step";
    private static final String SESSION_BATCH_ID  = "cts_inward_batch_id";

    private static final String PAGE_STEP2 = "/inward/inwardMicr/DateAmount.zul";
    private static final String PAGE_STEP3 = "/inward/inwardMicr/PayeeAccount.zul";
    private static final String PAGE_FILE  = "/inward/bpxfUpload/bpxfUpload.zul";
    private static final String PAGE_BATCH = "/inward/inwardMicr/batchSelect.zul";

    private static final int PAGE_SIZE = 10;

    // Zoom boundaries
    private static final int ZOOM_MIN  = 25;
    private static final int ZOOM_MAX  = 300;
    private static final int ZOOM_STEP = 25;

    // ── Wired: Panels ──────────────────────────────────────────────────────
    @Wire("#emptyStatePanel") private Div emptyStatePanel;
    @Wire("#batchListPanel")  private Div batchListPanel;
    @Wire("#repairPanel")     private Div repairPanel;

    // ── Wired: Wizard Bar ──────────────────────────────────────────────────
    @Wire("#btnStep1")      private Button btnStep1;
    @Wire("#btnStep2")      private Button btnStep2;
    @Wire("#btnStep3")      private Button btnStep3;
    @Wire("#conn1")         private Div    conn1;
    @Wire("#conn2")         private Div    conn2;
    @Wire("#lblStep2Num")   private Label  lblStep2Num;
    @Wire("#lblStep2Desc")  private Label  lblStep2Desc;
    @Wire("#lblStep3Num")   private Label  lblStep3Num;
    @Wire("#lblStep3Desc")  private Label  lblStep3Desc;

    // ── Wired: List Panel ──────────────────────────────────────────────────
    @Wire("#chequeListbox")        private Listbox  chequeListbox;
    @Wire("#lblBatchBadge")        private Label    lblBatchBadge;
    @Wire("#lblPendingBadge")      private Label    lblPendingBadge;
    @Wire("#lblPageInfo")          private Label    lblPageInfo;
    @Wire("#cmbFilter")            private Combobox cmbFilter;
    @Wire("#txtSearch")            private Textbox  txtSearch;
    @Wire("#btnGoToFileProcessing")private Button   btnGoToFileProcessing;
    @Wire("#btnBackToBatches")     private Button   btnBackToBatches;
    @Wire("#btnNextStep2")         private Button   btnNextStep2;
    @Wire("#btnPrevPage")          private Button   btnPrevPage;
    @Wire("#btnNextPage")          private Button   btnNextPage;

    // ── Wired: Repair Panel ────────────────────────────────────────────────
    @Wire("#btnBackToList")      private Button btnBackToList;
    @Wire("#lblRepairBatchBadge")private Label  lblRepairBatchBadge;
    @Wire("#lblRepairErrorBadge")private Label  lblRepairErrorBadge;

    // Image viewer
    @Wire("#btnViewFront")  private Button btnViewFront;
    @Wire("#btnViewBack")   private Button btnViewBack;
    @Wire("#btnViewGray")   private Button btnViewGray;
    @Wire("#btnZoomIn")     private Button btnZoomIn;
    @Wire("#btnZoomOut")    private Button btnZoomOut;
    @Wire("#btnZoomFit")    private Button btnZoomFit;
    @Wire("#lblZoomLevel")  private Label  lblZoomLevel;
    @Wire("#divFrontImage") private Div    divFrontImage;
    @Wire("#divBackImage")  private Div    divBackImage;
    @Wire("#divGrayImage")  private Div    divGrayImage;
    @Wire("#imgFront")      private Image  imgFront;
    @Wire("#imgBack")       private Image  imgBack;
    @Wire("#imgGray")       private Image  imgGray;
    @Wire("#lblMicrBandStrip") private Label lblMicrBandStrip;
    @Wire("#ocrWarningBar")    private Div   ocrWarningBar;

    // Navigation
    @Wire("#btnPrevCheque")   private Button btnPrevCheque;
    @Wire("#btnNextCheque")   private Button btnNextCheque;
    @Wire("#lblNavIndicator") private Label  lblNavIndicator;

    // MICR comparison
    @Wire("#lblProcessedMicr") private Label lblProcessedMicr;
    @Wire("#lblReceivedMicr")  private Label lblReceivedMicr;
    @Wire("#lblOcrErrorBadge") private Label lblOcrErrorBadge;

    // Form fields
    @Wire("#txtChequeNo")       private Textbox txtChequeNo;
    @Wire("#txtCityCode")       private Textbox txtCityCode;
    @Wire("#txtBankCode")       private Textbox txtBankCode;
    @Wire("#txtBranchCode")     private Textbox txtBranchCode;
    @Wire("#txtPresentingBank") private Textbox txtPresentingBank;
    @Wire("#txtRemarks")        private Textbox txtRemarks;

    // Progress + actions
    @Wire("#divProgressFill")  private Div    divProgressFill;
    @Wire("#lblProgressText")  private Label  lblProgressText;
    @Wire("#btnRepairSave")    private Button btnRepairSave;

    // ── Service ────────────────────────────────────────────────────────────
    private final RejectRepairService service = new RejectRepairServiceImpl();

    // ── State ──────────────────────────────────────────────────────────────
    private String             currentBatchId;
    private List<InwardCheque> allCheques;
    private List<InwardCheque> repairList;
    private int    currentPage    = 1;
    private int    repairIdx      = 0;
    private int    currentZoomPct = 100;
    private String activeImageView = "front";

    // ── Lifecycle ──────────────────────────────────────────────────────────
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        initWizardBar();
        loadPage();
        wireRepairPanelEvents();
    }

    // ======================================================================
    // WIZARD BAR
    // ======================================================================
    private void initWizardBar() {
        int maxStep = getSessionMaxStep();
        applyStepStyle(btnStep1, 1, maxStep);
        applyStepStyle(btnStep2, 2, maxStep);
        applyStepStyle(btnStep3, 3, maxStep);
        if (conn1 != null)
            conn1.setSclass(maxStep >= 2 ? "step-connector filled" : "step-connector");
        if (conn2 != null)
            conn2.setSclass(maxStep >= 3 ? "step-connector filled" : "step-connector");
        applyLabelStyle(lblStep2Num, lblStep2Desc, 2);
        applyLabelStyle(lblStep3Num, lblStep3Desc, 3);
    }

    private void applyStepStyle(Button btn, int step, int maxStep) {
        if (btn == null) return;
        if (step == CURRENT_STEP)
            btn.setSclass("step-circle-btn active");
        else if (step < CURRENT_STEP)
            btn.setSclass("step-circle-btn completed");
        else if (step <= maxStep)
            btn.setSclass("step-circle-btn active");
        else
            btn.setSclass("step-circle-btn disabled-step");
    }

    private void applyLabelStyle(Label num, Label desc, int step) {
        if (num == null) return;
        if (step == CURRENT_STEP) {
            num.setSclass("step-num active");
            if (desc != null) desc.setSclass("step-desc active");
        } else if (step < CURRENT_STEP) {
            num.setSclass("step-num completed");
            if (desc != null) desc.setSclass("step-desc completed");
        } else {
            num.setSclass("step-num");
            if (desc != null) desc.setSclass("step-desc");
        }
    }

    // ======================================================================
    // STEP NAVIGATION
    // ======================================================================
    @Listen("onClick=#btnStep1")
    public void onStep1() { /* already on Step 1 */ }

    @Listen("onClick=#btnStep2")
    public void onStep2() {
        if (getSessionMaxStep() >= 2)
            Executions.getCurrent().sendRedirect(PAGE_STEP2 + batchParam());
    }

    @Listen("onClick=#btnStep3")
    public void onStep3() {
        if (getSessionMaxStep() >= 3)
            Executions.getCurrent().sendRedirect(PAGE_STEP3 + batchParam());
    }

    // ======================================================================
    // DATA LOAD  — FIXED
    // getChequesByBatchId() takes Long (numeric PK), not String (batchId).
    // Resolve InwardBatch first, then pass batch.getId().
    // ======================================================================
    private void loadPage() {
        String paramBatch = Executions.getCurrent().getParameter("batchId");
        if (paramBatch != null && !paramBatch.isBlank()) {
            currentBatchId = paramBatch.trim();
            Sessions.getCurrent().setAttribute(SESSION_BATCH_ID, currentBatchId);
        } else {
            Object sess = Sessions.getCurrent().getAttribute(SESSION_BATCH_ID);
            if (sess != null) {
                currentBatchId = sess.toString();
            } else {
                List<InwardBatch> batches = service.getRepairEligibleBatches();
                if (batches == null || batches.isEmpty()) {
                    showEmpty();
                    return;
                }
                currentBatchId = batches.get(0).getBatchId();
                Sessions.getCurrent().setAttribute(SESSION_BATCH_ID, currentBatchId);
            }
        }

        // FIX: Resolve batch object to get numeric PK for cheque query
        InwardBatch batch = service.getBatchById(currentBatchId);
        if (batch == null) {
            showEmpty();
            return;
        }

        List<InwardCheque> fetched = service.getChequesByBatchId(batch.getId());
        if (fetched == null || fetched.isEmpty()) {
            showEmpty();
            return;
        }

        allCheques = fetched;

        repairList = allCheques.stream()
                .filter(c -> c.isMicrError()
                        || "NEEDS_REPAIR".equalsIgnoreCase(c.getRepairStatus())
                        || "REFERRED_BACK".equalsIgnoreCase(c.getRepairStatus()))
                .collect(Collectors.toList());

        showList();
    }

    // ======================================================================
    // PANEL SWITCHING
    // ======================================================================
    private void showEmpty() {
        setVisible(emptyStatePanel, true);
        setVisible(batchListPanel,  false);
        setVisible(repairPanel,     false);
    }

    private void showList() {
        setVisible(emptyStatePanel, false);
        setVisible(batchListPanel,  true);
        setVisible(repairPanel,     false);
        if (lblBatchBadge != null)
            lblBatchBadge.setValue("BATCH: " + currentBatchId);
        currentPage = 1;
        renderTable();
    }

    private void showRepairPanel(int idx) {
        repairIdx = idx;
        setVisible(emptyStatePanel, false);
        setVisible(batchListPanel,  false);
        setVisible(repairPanel,     true);
        if (lblRepairBatchBadge != null)
            lblRepairBatchBadge.setValue("BATCH: " + currentBatchId);
        loadRepairRecord();
    }

    private static void setVisible(Div div, boolean visible) {
        if (div != null) div.setVisible(visible);
    }

    // ======================================================================
    // LIST TABLE
    // ======================================================================
    private void renderTable() {
        if (chequeListbox == null) return;
        chequeListbox.getItems().clear();

        List<InwardCheque> filtered  = getFilteredList();
        int total      = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage    = Math.min(currentPage, totalPages);

        int from   = (currentPage - 1) * PAGE_SIZE;
        int to     = Math.min(from + PAGE_SIZE, total);
        int rowNum = from + 1;

        for (InwardCheque c : filtered.subList(from, to))
            appendChequeRow(c, rowNum++);

        if (lblPageInfo != null)
            lblPageInfo.setValue("Page " + currentPage + " of " + totalPages
                    + " | " + total + " records");
        if (btnPrevPage != null) btnPrevPage.setDisabled(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisabled(currentPage >= totalPages);

        long pending = repairList.stream()
                .filter(c -> c.isMicrError()
                          || "NEEDS_REPAIR".equalsIgnoreCase(c.getRepairStatus()))
                .count();
        if (lblPendingBadge != null)
            lblPendingBadge.setValue(pending + " PENDING");
    }

    private void appendChequeRow(InwardCheque c, int sno) {
        Listitem row = new Listitem();
        row.setValue(c);
        row.setSclass("clickable-row");

        addCell(row, String.valueOf(sno));
        addMonoCell(row, nvl(c.getChequeNo()));
        addCell(row, nvl(c.getPresentingBankName()));

        Listcell micrCell = new Listcell(nvl(c.getMicrCodeRaw()));
        micrCell.setSclass("z-listcell mono txt-danger");
        row.appendChild(micrCell);

        Listcell amtCell = new Listcell(
                c.getAmount() != null ? "₹ " + fmt(c.getAmount()) : "—");
        amtCell.setStyle("text-align:right");
        row.appendChild(amtCell);

        Listcell statusCell = new Listcell();
        statusCell.setStyle("text-align:center");
        Label badge = new Label(statusLabel(c.getRepairStatus()));
        badge.setSclass(statusSclass(c.getRepairStatus()));
        statusCell.appendChild(badge);
        row.appendChild(statusCell);

        Listcell actionCell = new Listcell();
        actionCell.setStyle("text-align:center");
        Button repairBtn = new Button("Repair");
        repairBtn.setSclass("btn-repair-row");

        int idx = repairList.indexOf(c);
        repairBtn.addEventListener(Events.ON_CLICK, e -> {
            e.stopPropagation();
            showRepairPanel(Math.max(0, idx));
        });
        actionCell.appendChild(repairBtn);
        row.appendChild(actionCell);

        row.addEventListener(Events.ON_CLICK, e -> {
            int idx1 = repairList.indexOf(c);
            if (idx1 >= 0) showRepairPanel(idx1);
        });
        chequeListbox.appendChild(row);
    }

    // ======================================================================
    // FILTER
    // ======================================================================
    private List<InwardCheque> getFilteredList() {
        if (repairList == null) return Collections.emptyList();

        String filterVal = "";
        if (cmbFilter != null && cmbFilter.getSelectedItem() != null) {
            Object v = cmbFilter.getSelectedItem().getValue();
            if (v != null) filterVal = v.toString();
        }
        String search = (txtSearch != null && txtSearch.getValue() != null)
                ? txtSearch.getValue().trim().toLowerCase() : "";

        final String fv = filterVal;
        return repairList.stream().filter(c -> {
            if (!fv.isEmpty()) {
                String rs = c.getRepairStatus() != null ? c.getRepairStatus() : "";
                if (!rs.equalsIgnoreCase(fv)) return false;
            }
            if (!search.isEmpty()) {
                String no   = c.getChequeNo()          != null ? c.getChequeNo().toLowerCase()          : "";
                String bank = c.getPresentingBankName() != null ? c.getPresentingBankName().toLowerCase() : "";
                return no.contains(search) || bank.contains(search);
            }
            return true;
        }).collect(Collectors.toList());
    }

    // ======================================================================
    // REPAIR PANEL — LOAD RECORD
    // ======================================================================
    private void loadRepairRecord() {
        if (repairList == null || repairList.isEmpty()) {
            showList();
            return;
        }
        repairIdx = Math.max(0, Math.min(repairList.size() - 1, repairIdx));
        InwardCheque c = repairList.get(repairIdx);

        if (lblNavIndicator != null)
            lblNavIndicator.setValue((repairIdx + 1) + " of " + repairList.size());
        if (btnPrevCheque != null) btnPrevCheque.setDisabled(repairIdx <= 0);
        if (btnNextCheque != null) btnNextCheque.setDisabled(repairIdx >= repairList.size() - 1);

        long done  = repairList.stream()
                .filter(ch -> "REPAIRED".equalsIgnoreCase(ch.getRepairStatus())).count();
        int  total = repairList.size();
        int  pct   = total > 0 ? (int) ((done * 100) / total) : 0;
        if (divProgressFill != null) divProgressFill.setStyle("width:" + pct + "%");
        if (lblProgressText != null) lblProgressText.setValue(done + "/" + total);

        String errorType = deriveMicrErrorType(c);
        if (lblRepairErrorBadge != null) lblRepairErrorBadge.setValue(errorType);
        if (lblOcrErrorBadge    != null) lblOcrErrorBadge.setValue("OCR ERROR");

        loadChequeImages(c);

        activeImageView = "front";
        showImagePanel("front");
        resetZoom();

        if (lblMicrBandStrip != null)
            lblMicrBandStrip.setValue(
                    "⑆ " + nvl(c.getChequeNo()) + " ⑆  "
                    + nvl(c.getMicrCodeRaw()) + " ⑈  ⑉29⑉");

        if (ocrWarningBar != null) ocrWarningBar.setVisible(true);

        String processedMicr = (c.getMicrCodeCorrected() != null
                && !c.getMicrCodeCorrected().isBlank())
                ? c.getMicrCodeCorrected()
                : sanitizeMicr(c.getMicrCodeRaw());
        if (lblProcessedMicr != null) lblProcessedMicr.setValue(processedMicr);
        if (lblReceivedMicr  != null) lblReceivedMicr.setValue(nvl(c.getMicrCodeRaw()));

        setTbValue(txtChequeNo,       c.getChequeNo());
        setTbValue(txtCityCode,       c.getCityCode());
        setTbValue(txtBankCode,       c.getBankCode());
        setTbValue(txtBranchCode,     c.getBranchCode());
        setTbValue(txtPresentingBank, c.getPresentingBankName());
        setTbValue(txtRemarks,        "");

        highlightErrorFields(c);
    }

    // ======================================================================
    // IMAGE LOADING
    // ======================================================================
    private void loadChequeImages(InwardCheque c) {
        setImageViaServlet(imgFront, c.getFrontImagePath());
        setImageViaServlet(imgBack,  c.getBackImagePath());
        setImageViaServlet(imgGray,  c.getFrontImagePath());
        if (imgGray != null)
            imgGray.setStyle("filter:grayscale(100%);max-width:100%;display:block;");
    }

    private void setImageViaServlet(Image img, String path) {
        if (img == null) return;
        if (path == null || path.trim().isEmpty()) {
            img.setSrc("");
            return;
        }
        try {
            String encoded = URLEncoder.encode(path.trim(), "UTF-8");
            img.setSrc("/imageServlet?path=" + encoded);
        } catch (UnsupportedEncodingException e) {
            img.setSrc("/imageServlet?path=" + path.trim());
        }
    }

    // ======================================================================
    // MICR ERROR TYPE
    // ======================================================================
    private String deriveMicrErrorType(InwardCheque c) {
        if (!c.isMicrError()) return "MANUAL REVIEW";

        boolean cityBad   = containsNonNumeric(c.getCityCode());
        boolean bankBad   = containsNonNumeric(c.getBankCode());
        boolean branchBad = containsNonNumeric(c.getBranchCode());
        boolean chqBad    = containsNonNumeric(c.getChequeNo());

        int badCount = (cityBad ? 1 : 0) + (bankBad ? 1 : 0)
                     + (branchBad ? 1 : 0) + (chqBad ? 1 : 0);
        if (badCount > 1) return "MICR ERROR";
        if (chqBad)       return "CHEQUE NO ERROR";
        if (bankBad)      return "BANK CODE ERROR";
        if (branchBad)    return "BRANCH CODE ERROR";
        if (cityBad)      return "CITY CODE ERROR";
        return "MICR ERROR";
    }

    private void highlightErrorFields(InwardCheque c) {
        applyFieldStyle(txtChequeNo,   "form-input mono");
        applyFieldStyle(txtCityCode,   "form-input mono ocr-filled");
        applyFieldStyle(txtBankCode,   "form-input mono");
        applyFieldStyle(txtBranchCode, "form-input mono ocr-filled");

        if (!c.isMicrError()) return;

        if (containsNonNumeric(c.getChequeNo()))   applyFieldStyle(txtChequeNo,   "form-input mono field-mismatch");
        if (containsNonNumeric(c.getCityCode()))   applyFieldStyle(txtCityCode,   "form-input mono field-mismatch");
        if (containsNonNumeric(c.getBankCode()))   applyFieldStyle(txtBankCode,   "form-input mono field-mismatch");
        if (containsNonNumeric(c.getBranchCode())) applyFieldStyle(txtBranchCode, "form-input mono field-mismatch");

        boolean anyBad = containsNonNumeric(c.getChequeNo())
                || containsNonNumeric(c.getCityCode())
                || containsNonNumeric(c.getBankCode())
                || containsNonNumeric(c.getBranchCode());
        if (!anyBad)
            applyFieldStyle(txtBankCode, "form-input mono field-mismatch");
    }

    // ======================================================================
    // REPAIR PANEL — EVENT WIRING
    // ======================================================================
    private void wireRepairPanelEvents() {
        addClickListener(btnPrevPage, e -> {
            if (currentPage > 1) { currentPage--; renderTable(); }
        });
        addClickListener(btnNextPage, e -> {
            if (currentPage < totalPages()) { currentPage++; renderTable(); }
        });
        if (cmbFilter != null)
            cmbFilter.addEventListener(Events.ON_SELECT, e -> {
                currentPage = 1; renderTable();
            });
        if (txtSearch != null)
            txtSearch.addEventListener(Events.ON_CHANGING, e -> {
                currentPage = 1; renderTable();
            });

        addClickListener(btnPrevCheque, e -> {
            if (repairIdx > 0) { repairIdx--; loadRepairRecord(); }
        });
        addClickListener(btnNextCheque, e -> {
            if (repairList != null && repairIdx < repairList.size() - 1) {
                repairIdx++; loadRepairRecord();
            }
        });

        addClickListener(btnViewFront, e -> switchImagePanel("front"));
        addClickListener(btnViewBack,  e -> switchImagePanel("back"));
        addClickListener(btnViewGray,  e -> switchImagePanel("gray"));

        addClickListener(btnZoomIn,  e -> adjustZoom(+ZOOM_STEP));
        addClickListener(btnZoomOut, e -> adjustZoom(-ZOOM_STEP));
        addClickListener(btnZoomFit, e -> resetZoom());

        addClickListener(btnBackToList, e -> showList());
        addClickListener(btnRepairSave, e -> doRepairSave());
    }

    private void switchImagePanel(String view) {
        activeImageView = view;
        showImagePanel(view);
        currentZoomPct = 100;
        applyZoomToActivePanel();
        if (lblZoomLevel != null) lblZoomLevel.setValue("100%");
    }

    private void showImagePanel(String view) {
        if (divFrontImage != null) divFrontImage.setVisible("front".equals(view));
        if (divBackImage  != null) divBackImage.setVisible("back".equals(view));
        if (divGrayImage  != null) divGrayImage.setVisible("gray".equals(view));
        setToggleActive(btnViewFront, "front".equals(view));
        setToggleActive(btnViewBack,  "back".equals(view));
        setToggleActive(btnViewGray,  "gray".equals(view));
    }

    private void setToggleActive(Button btn, boolean active) {
        if (btn == null) return;
        btn.setSclass(active ? "view-toggle-btn active" : "view-toggle-btn");
    }

    private void adjustZoom(int delta) {
        currentZoomPct = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, currentZoomPct + delta));
        applyZoomToActivePanel();
    }

    private void resetZoom() {
        currentZoomPct = 100;
        applyZoomToActivePanel();
    }

    private void applyZoomToActivePanel() {
        if (lblZoomLevel != null) lblZoomLevel.setValue(currentZoomPct + "%");

        String baseStyle  = "overflow:hidden;transform-origin:top left;transition:transform 0.15s ease;";
        String scaleStyle = baseStyle + "transform:scale(" + (currentZoomPct / 100.0) + ");";
        String neutral    = "transform:scale(1);" + baseStyle;

        switch (activeImageView) {
            case "front" -> {
                applyDivStyle(divFrontImage, scaleStyle);
                applyDivStyle(divBackImage,  neutral);
                applyDivStyle(divGrayImage,  neutral);
            }
            case "back" -> {
                applyDivStyle(divFrontImage, neutral);
                applyDivStyle(divBackImage,  scaleStyle);
                applyDivStyle(divGrayImage,  neutral);
            }
            case "gray" -> {
                applyDivStyle(divFrontImage, neutral);
                applyDivStyle(divBackImage,  neutral);
                applyDivStyle(divGrayImage,  scaleStyle + "filter:grayscale(100%);");
            }
            default -> {
                applyDivStyle(divFrontImage, scaleStyle);
                applyDivStyle(divBackImage,  neutral);
                applyDivStyle(divGrayImage,  neutral);
            }
        }
    }

    // ======================================================================
    // SAVE
    // ======================================================================
    private void doRepairSave() {
        if (repairList == null || repairList.isEmpty()) return;
        InwardCheque c = repairList.get(repairIdx);

        String chequeNo   = sanitizeInput(txtChequeNo);
        String cityCode   = sanitizeInput(txtCityCode);
        String bankCode   = sanitizeInput(txtBankCode);
        String branchCode = sanitizeInput(txtBranchCode);
        String remarks    = tbVal(txtRemarks);

        java.util.logging.Logger.getLogger(getClass().getName()).info(
                "[MICR-SAVE] chequeNo='" + chequeNo + "' cityCode='" + cityCode
                + "' bankCode='" + bankCode + "' branchCode='" + branchCode + "'");

        if (chequeNo.isEmpty())   { showError("Cheque Number is required.");  return; }
        if (cityCode.isEmpty())   { showError("City Code is required.");      return; }
        if (bankCode.isEmpty())   { showError("Bank Code is required.");      return; }
        if (branchCode.isEmpty()) { showError("Branch Code is required.");    return; }

        if (!chequeNo.matches("\\d+")   || chequeNo.length()   > 6)
            { showError("Cheque Number must contain digits only (max 6 digits)."); return; }
        if (!cityCode.matches("\\d+")   || cityCode.length()   > 3)
            { showError("City Code must contain digits only (max 3 digits).");     return; }
        if (!bankCode.matches("\\d+")   || bankCode.length()   > 3)
            { showError("Bank Code must contain digits only (max 3 digits).");     return; }
        if (!branchCode.matches("\\d+") || branchCode.length() > 3)
            { showError("Branch Code must contain digits only (max 3 digits).");   return; }

        String correctedMicr = cityCode + bankCode + branchCode;
        String processedMicr = (c.getMicrCodeCorrected() != null
                && !c.getMicrCodeCorrected().isBlank())
                ? c.getMicrCodeCorrected().trim()
                : sanitizeMicr(c.getMicrCodeRaw());

        String processedMicrBase = processedMicr.length() >= 9
                ? processedMicr.substring(0, 9) : processedMicr;

        boolean micrMatched = correctedMicr.equals(processedMicrBase);

        if (micrMatched) {
            persistRepair(c, chequeNo, cityCode, bankCode, branchCode,
                    correctedMicr, remarks, true);
            showSuccess("Cheque " + chequeNo + " repaired and saved successfully.");
            moveToNextPending();
        } else {
            String msg = "The MICR you entered (" + correctedMicr
                    + ") differs from the processed data (" + processedMicrBase + ").\n\n"
                    + "Do you still want to save this repair?\n"
                    + "(Select YES only if you verified the physical cheque.)";

            Messagebox.show(msg, "MICR Mismatch — Confirm Override",
                    Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
                    event -> {
                        if (Messagebox.ON_YES.equals(event.getName())) {
                            persistRepair(c, chequeNo, cityCode, bankCode, branchCode,
                                    correctedMicr, remarks, false);
                            moveToNextPending();
                        }
                    });
        }
    }

    private String sanitizeInput(Textbox tb) {
        if (tb == null) return "";
        String val = tb.getValue();
        if (val == null) return "";
        return val.replaceAll("[\\s\\u00A0\\u200B\\u200C\\u200D\\uFEFF]", "").trim();
    }

    // ======================================================================
    // PERSIST REPAIR  — FIXED
    // Removed cast to RejectRepairServiceImpl.
    // saveRepair(InwardCheque, String) is on the RejectRepairService interface.
    // ======================================================================
    private void persistRepair(InwardCheque c, String chequeNo, String cityCode,
            String bankCode, String branchCode,
            String correctedMicr, String remarks, boolean matched) {

        c.setChequeNo(chequeNo);
        c.setCityCode(cityCode);
        c.setBankCode(bankCode);
        c.setBranchCode(branchCode);
        c.setMicrCodeCorrected(correctedMicr);
        c.setMicrError(false);
        c.setRepairStatus("REPAIRED");
        if (remarks != null && !remarks.isBlank())
            c.setRemarks(remarks);

        // FIX: Call through interface — no cast to impl needed
        service.saveRepair(c, currentBatchId);

        int pos = allCheques.indexOf(c);
        if (pos >= 0) allCheques.set(pos, c);
    }

    // ======================================================================
    // MOVE TO NEXT PENDING
    // ======================================================================
    private void moveToNextPending() {
        repairList = allCheques.stream()
                .filter(c -> c.isMicrError()
                          || "NEEDS_REPAIR".equalsIgnoreCase(c.getRepairStatus()))
                .collect(Collectors.toList());

        int nextIdx = -1;
        for (int i = repairIdx; i < repairList.size(); i++) {
            if (isStillPending(repairList.get(i))) { nextIdx = i; break; }
        }
        if (nextIdx < 0) {
            for (int i = 0; i < repairIdx; i++) {
                if (isStillPending(repairList.get(i))) { nextIdx = i; break; }
            }
        }

        if (nextIdx >= 0) {
            repairIdx = nextIdx;
            loadRepairRecord();
        } else {
            if (divProgressFill != null) divProgressFill.setStyle("width:100%");
            if (lblProgressText != null)
                lblProgressText.setValue(repairList.size() + "/" + repairList.size());

            Messagebox.show(
                    "All MICR repairs completed.\nProceed to Step 2: Date & Amount Repair?",
                    "Repairs Complete",
                    Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
                    event -> {
                        if (Messagebox.ON_YES.equals(event.getName())) {
                            setSessionMaxStep(2);
                            Executions.getCurrent().sendRedirect(PAGE_STEP2 + batchParam());
                        } else {
                            showList();
                        }
                    });
        }
    }

    private boolean isStillPending(InwardCheque c) {
        return c.isMicrError() || "NEEDS_REPAIR".equalsIgnoreCase(c.getRepairStatus());
    }

    // ======================================================================
    // NAVIGATION BUTTON LISTENERS
    // ======================================================================
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
                    "Validation", Messagebox.OK, Messagebox.NONE);
            return;
        }
        setSessionMaxStep(2);
        Executions.getCurrent().sendRedirect(PAGE_STEP2 + batchParam());
    }

    // ======================================================================
    // UTILITIES
    // ======================================================================
    private boolean containsNonNumeric(String value) {
        if (value == null || value.isBlank()) return false;
        return !value.matches("\\d+");
    }

    private String sanitizeMicr(String raw) {
        if (raw == null) return "—";
        return raw.replace("?", "0");
    }

    private void addClickListener(Button btn,
            org.zkoss.zk.ui.event.EventListener<?> l) {
        if (btn != null) btn.addEventListener(Events.ON_CLICK, l);
    }

    private void applyDivStyle(Div div, String style) {
        if (div != null) div.setStyle(style);
    }

    private void applyFieldStyle(Textbox tb, String sclass) {
        if (tb != null) tb.setSclass(sclass);
    }

    private void setTbValue(Textbox tb, String val) {
        if (tb != null) tb.setValue(val != null ? val : "");
    }

    private String tbVal(Textbox tb) {
        return (tb != null && tb.getValue() != null) ? tb.getValue().trim() : "";
    }

    private boolean allRepairsDone() {
        if (repairList == null) return true;
        return repairList.stream().noneMatch(this::isStillPending);
    }

    private int totalPages() {
        return Math.max(1,
                (int) Math.ceil((double) getFilteredList().size() / PAGE_SIZE));
    }

    private String batchParam() {
        return currentBatchId != null ? "?batchId=" + currentBatchId : "";
    }

    private int getSessionMaxStep() {
        Object v = Sessions.getCurrent().getAttribute(SESSION_MAX_STEP);
        return (v instanceof Integer i) ? i : CURRENT_STEP;
    }

    private void setSessionMaxStep(int step) {
        if (step > getSessionMaxStep())
            Sessions.getCurrent().setAttribute(SESSION_MAX_STEP, step);
    }

    private void addCell(Listitem row, String text) {
        row.appendChild(new Listcell(text != null ? text : "—"));
    }

    private void addMonoCell(Listitem row, String text) {
        Listcell cell = new Listcell(text != null ? text : "—");
        cell.setSclass("z-listcell mono");
        row.appendChild(cell);
    }

    private String nvl(String v) {
        return (v != null && !v.isBlank()) ? v : "—";
    }

    private String fmt(BigDecimal amt) {
        return String.format("%,.2f", amt);
    }

    private String statusLabel(String status) {
        if (status == null || status.isBlank()) return "NEEDS REPAIR";
        return switch (status.toUpperCase()) {
            case "REPAIRED"      -> "REPAIRED";
            case "REFERRED_BACK" -> "REFERRED BACK";
            case "NEEDS_REPAIR"  -> "NEEDS REPAIR";
            case "NOT_REQUIRED"  -> "NOT REQUIRED";
            default              -> status;
        };
    }

    private String statusSclass(String status) {
        if (status == null || status.isBlank()) return "badge-needs-repair";
        return switch (status.toUpperCase()) {
            case "REPAIRED"      -> "badge-repaired";
            case "REFERRED_BACK" -> "badge-referred";
            case "NOT_REQUIRED"  -> "badge-ok";
            default              -> "badge-needs-repair";
        };
    }

    private void showError(String msg) {
        Messagebox.show(msg, "Validation Error", Messagebox.OK, Messagebox.ERROR);
    }

    private void showSuccess(String msg) {
        Messagebox.show(msg, "Success", Messagebox.OK, Messagebox.INFORMATION);
    }
}