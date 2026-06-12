package com.iispl.composer;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
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
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.DateAmountService;
import com.iispl.serviceImpl.DateAmountServiceImpl;

public class DateAmountComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String SESSION_BATCH_ID = "cts_inward_batch_id";
    private static final String SESSION_MAX_STEP  = "cts_inward_max_step";
    private static final String PAGE_STEP1 = "/inward/inwardMicr/RejectRepair.zul";
    private static final String PAGE_STEP3 = "/inward/inwardMicr/PayeeAccount.zul";

    /** Show exactly 5 cheques per page in the list. */
    private static final int PAGE_SIZE = 5;

    // Zoom boundaries — same as RejectRepairComposer
    private static final int ZOOM_MIN  = 25;
    private static final int ZOOM_MAX  = 300;
    private static final int ZOOM_STEP = 25;

    // ── Wizard bar ────────────────────────────────────────────────────────
    @Wire("#btnStep1")     private Button btnStep1;
    @Wire("#btnStep2")     private Button btnStep2;
    @Wire("#btnStep3")     private Button btnStep3;
    @Wire("#conn1")        private Div    conn1;
    @Wire("#conn2")        private Div    conn2;
    @Wire("#lblStep3Num")  private Label  lblStep3Num;
    @Wire("#lblStep3Desc") private Label  lblStep3Desc;

    // ── Breadcrumbs ───────────────────────────────────────────────────────
    @Wire("#bcList")   private Div bcList;
    @Wire("#bcDetail") private Div bcDetail;

    // ── Image viewer (split-left) ─────────────────────────────────────────
    @Wire("#btnViewFront")  private Button btnViewFront;
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

    // ── List-panel sub-toolbar ────────────────────────────────────────────
    @Wire("#lblBatchBadge")   private Label    lblBatchBadge;
    @Wire("#lblPendingBadge") private Label    lblPendingBadge;
    @Wire("#cmbFilter")       private Combobox cmbFilter;
    @Wire("#btnBackToStep1")  private Button   btnBackToStep1;

    // ── Panels ────────────────────────────────────────────────────────────
    @Wire("#listPanel")        private Div listPanel;
    @Wire("#reviewSplitPanel") private Div reviewSplitPanel;

    // ── List panel controls ───────────────────────────────────────────────
    @Wire("#chequeListbox") private Listbox chequeListbox;
    @Wire("#lblPageInfo")   private Label   lblPageInfo;
    @Wire("#btnPrevPage")   private Button  btnPrevPage;
    @Wire("#btnNextPage")   private Button  btnNextPage;
    @Wire("#btnNextStep3")  private Button  btnNextStep3;

    // ── Review sub-toolbar (no filter) ───────────────────────────────────
    @Wire("#lblBatchBadge2")   private Label  lblBatchBadge2;
    @Wire("#lblPendingBadge2") private Label  lblPendingBadge2;
    @Wire("#btnBackToList2")   private Button btnBackToList2;

    // ── Split-left nav ────────────────────────────────────────────────────
    @Wire("#btnPrevCheque")   private Button btnPrevCheque;
    @Wire("#btnNextCheque")   private Button btnNextCheque;
    @Wire("#lblNavIndicator") private Label  lblNavIndicator;

    // ── Split-right form ──────────────────────────────────────────────────
    @Wire("#lblOcrErrorBadge") private Label     lblOcrErrorBadge;
    @Wire("#lblDetailChequeNo") private Label    lblDetailChequeNo;
    @Wire("#lblDetailBank")     private Label    lblDetailBank;
    @Wire("#lblProcDate")      private Label     lblProcDate;
    @Wire("#lblProcAmt")       private Label     lblProcAmt;
    @Wire("#lblRcvdDate")      private Label     lblRcvdDate;
    @Wire("#lblRcvdAmt")       private Label     lblRcvdAmt;
    @Wire("#dtCorrectedDate")  private Datebox   dtCorrectedDate;
    @Wire("#numCorrectedAmt")  private Doublebox numCorrectedAmt;
    @Wire("#txtRemarks")       private Textbox   txtRemarks;
    @Wire("#btnAccept")        private Button    btnAccept;
    @Wire("#btnReject")        private Button    btnReject;
    @Wire("#daWarningBar")     private Div       daWarningBar;

    // ── Reject popup ──────────────────────────────────────────────────────
    @Wire("#rejectReasonPopup") private Window   rejectReasonPopup;
    @Wire("#lblRejChqNo")       private Label    lblRejChqNo;
    @Wire("#lblRejAmt")         private Label    lblRejAmt;
    @Wire("#cmbRejectReason")   private Combobox cmbRejectReason;
    @Wire("#txtRejectRemarks")  private Textbox  txtRejectRemarks;
    @Wire("#btnConfirmReject")  private Button   btnConfirmReject;
    @Wire("#btnCancelReject")   private Button   btnCancelReject;

    // ── Service & state ───────────────────────────────────────────────────
    private final DateAmountService service = new DateAmountServiceImpl();

    private String             currentBatchId;
    private List<InwardCheque> allCheques;
    /** Sub-list of cheques that still need work — used for Prev/Next in review. */
    private List<InwardCheque> reviewList;
    private InwardCheque       selectedCheque;

    private int currentPage = 1;
    private int reviewIdx   = 0;

    // ── Zoom & image-view state (mirrors RejectRepairComposer exactly) ────
    private int    currentZoomPct  = 100;
    private String activeImageView = "front";   // "front" | "back" | "gray"

    // ═════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        resolveBatchId();

        if (currentBatchId != null) {
            InwardBatch batch = service.getBatchById(currentBatchId);
            if (batch != null) {
                allCheques = service.getStep2ChequesByBatchId(batch.getId());
            }
            if (allCheques == null) allCheques = Collections.emptyList();
            updateBatchBadge();
            updatePendingBadge();
            renderTable();
        }

        wireEvents();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  BATCH ID RESOLUTION
    // ═════════════════════════════════════════════════════════════════════

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
        var batches = service.getRepairEligibleBatches();
        if (batches != null && !batches.isEmpty()) {
            currentBatchId = batches.get(0).getBatchId();
            Sessions.getCurrent().setAttribute(SESSION_BATCH_ID, currentBatchId);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  EVENT WIRING
    // ═════════════════════════════════════════════════════════════════════

    private void wireEvents() {

        // Filter combobox — refresh list
        addEvt(cmbFilter, Events.ON_SELECT, e -> { currentPage = 1; renderTable(); });

        // Pagination
        addEvt(btnPrevPage, Events.ON_CLICK, e -> {
            if (currentPage > 1) { currentPage--; renderTable(); }
        });
        addEvt(btnNextPage, Events.ON_CLICK, e -> {
            if (currentPage < totalPages()) { currentPage++; renderTable(); }
        });

        // Review panel navigation
        addEvt(btnPrevCheque, Events.ON_CLICK, e -> {
            if (reviewIdx > 0) { reviewIdx--; loadReviewRecord(); }
        });
        addEvt(btnNextCheque, Events.ON_CLICK, e -> {
            if (reviewList != null && reviewIdx < reviewList.size() - 1) {
                reviewIdx++;
                loadReviewRecord();
            }
        });

        // ── Image panel toggle buttons ────────────────────────────────
        // Only Front is wired here; add Back/Gray buttons to ZUL if needed.
        addEvt(btnViewFront, Events.ON_CLICK, e -> switchImagePanel("front"));

        // ── Zoom controls ─────────────────────────────────────────────
        addEvt(btnZoomIn,  Events.ON_CLICK, e -> adjustZoom(+ZOOM_STEP));
        addEvt(btnZoomOut, Events.ON_CLICK, e -> adjustZoom(-ZOOM_STEP));
        addEvt(btnZoomFit, Events.ON_CLICK, e -> resetZoom());

        // Repair actions
        addEvt(btnAccept,        Events.ON_CLICK, e -> doAccept());
        addEvt(btnReject,        Events.ON_CLICK, e -> openRejectPopup());
        addEvt(btnBackToList2,   Events.ON_CLICK, e -> showList());
        addEvt(btnConfirmReject, Events.ON_CLICK, e -> doConfirmReject());
        addEvt(btnCancelReject,  Events.ON_CLICK, e -> rejectReasonPopup.setVisible(false));
    }

    private void addEvt(Component c, String evt,
            org.zkoss.zk.ui.event.EventListener<?> l) {
        if (c != null) c.addEventListener(evt, l);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PANEL SWITCHING
    // ═════════════════════════════════════════════════════════════════════

    /** Called by breadcrumb "Date And Amount" click — go back to list. */
    public void onBreadcrumbBack() {
        showList();
    }

    private void showList() {
        setVis(listPanel,        true);
        setVis(reviewSplitPanel, false);
        setVis(bcList,   true);
        setVis(bcDetail, false);
        updatePendingBadge();
        renderTable();
    }

    /**
     * Opens the split-screen review for the cheque at {@code globalIdx}
     * in {@code allCheques}.  Row click and Review button both call this.
     */
    private void showSplitReview(int globalIdx) {
        // Build the ordered work-list (pending cheques only for Prev/Next)
        reviewList = allCheques.stream()
                .filter(this::isPending)
                .collect(Collectors.toList());

        InwardCheque clicked = allCheques.get(globalIdx);

        // If clicked cheque is in reviewList, start there; otherwise open first
        reviewIdx = reviewList.indexOf(clicked);
        if (reviewIdx < 0) {
            // Cheque is already resolved — still let user view it; put it in a
            // single-item list so Prev/Next are disabled.
            reviewList = List.of(clicked);
            reviewIdx  = 0;
        }

        setVis(listPanel,        false);
        setVis(reviewSplitPanel, true);
        setVis(bcList,   false);
        setVis(bcDetail, true);

        // Sync the review sub-toolbar badges
        if (lblBatchBadge2   != null) lblBatchBadge2.setValue("BATCH: " + currentBatchId);
        updatePendingBadge();

        loadReviewRecord();
    }

    private static void setVis(Component c, boolean visible) {
        if (c != null) c.setVisible(visible);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  RENDER TABLE  (5 rows per page)
    // ═════════════════════════════════════════════════════════════════════

    private void renderTable() {
        if (chequeListbox == null || allCheques == null) return;

        List<InwardCheque> filtered   = getFilteredList();
        int total      = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage    = Math.min(currentPage, totalPages);

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        chequeListbox.getItems().clear();
        int rowNum = from + 1;

        for (InwardCheque c : filtered.subList(from, to)) {

            Listitem row = new Listitem();
            row.setSclass("clickable-row");

            // ── Serial number ──
            addCell(row, String.valueOf(rowNum++));

            // ── Cheque No ──
            Listcell chqCell = new Listcell(nvl(c.getChequeNo()));
            chqCell.setStyle("font-family:'IBM Plex Mono',monospace;font-weight:600");
            row.appendChild(chqCell);

            // ── Bank ──
            addCell(row, nvl(c.getPresentingBankName()));

            // ── Processed Date ──
            addCell(row, c.getChequeDate() != null ? c.getChequeDate().format(FMT) : "—");

            // ── Received (OCR) Date — red if mismatch ──
            boolean dateErr = c.getChequeDateOcr() == null
                    || !c.getChequeDateOcr().equals(c.getChequeDate());
            Listcell rcvdDateCell = new Listcell(
                    c.getChequeDateOcr() != null ? c.getChequeDateOcr().format(FMT) : "—");
            if (dateErr)
                rcvdDateCell.setStyle("color:#dc2626;font-weight:600");
            row.appendChild(rcvdDateCell);

            // ── Processed Amount ──
            Listcell procAmtCell = new Listcell(
                    c.getAmount() != null ? "₹ " + fmt(c.getAmount()) : "—");
            procAmtCell.setStyle("text-align:right");
            row.appendChild(procAmtCell);

            // ── Received (OCR) Amount — red if mismatch ──
            boolean amtErr = c.getAmountOcr() == null
                    || (c.getAmount() != null
                        && c.getAmountOcr().compareTo(c.getAmount()) != 0);
            Listcell rcvdAmtCell = new Listcell(
                    c.getAmountOcr() != null ? "₹ " + fmt(c.getAmountOcr()) : "—");
            rcvdAmtCell.setStyle("text-align:right"
                    + (amtErr ? ";color:#dc2626;font-weight:600" : ""));
            row.appendChild(rcvdAmtCell);

            // ── Status badge ──
            Listcell statusCell = new Listcell();
            statusCell.setStyle("text-align:center");
            Label badge = new Label(resolveStatusLabel(c.getRepairStatus()));
            badge.setSclass(resolveStatusSclass(c.getRepairStatus()));
            statusCell.appendChild(badge);
            row.appendChild(statusCell);

            // ── Review button ──
            Listcell actionCell = new Listcell();
            actionCell.setStyle("text-align:center");
            Button reviewBtn = new Button("Review");
            reviewBtn.setSclass("btn-repair");
            final int gIdx = allCheques.indexOf(c);
            reviewBtn.addEventListener(Events.ON_CLICK, ev -> {
                ev.stopPropagation();
                showSplitReview(gIdx);
            });
            actionCell.appendChild(reviewBtn);
            row.appendChild(actionCell);

            // ── Row click also opens detail ──
            row.addEventListener(Events.ON_CLICK, ev -> showSplitReview(gIdx));

            chequeListbox.appendChild(row);
        }

        // Pagination info
        if (lblPageInfo != null)
            lblPageInfo.setValue(
                    "Page " + currentPage + " of " + totalPages + " | " + total + " records");
        if (btnPrevPage != null) btnPrevPage.setDisabled(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisabled(currentPage >= totalPages);

        updatePendingBadge();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  LOAD REVIEW RECORD INTO SPLIT-SCREEN
    // ═════════════════════════════════════════════════════════════════════

    private void loadReviewRecord() {
        if (reviewList == null || reviewList.isEmpty()) { showList(); return; }
        reviewIdx      = Math.max(0, Math.min(reviewList.size() - 1, reviewIdx));
        selectedCheque = reviewList.get(reviewIdx);
        InwardCheque c = selectedCheque;

        // Nav indicator
        if (lblNavIndicator != null)
            lblNavIndicator.setValue((reviewIdx + 1) + " of " + reviewList.size());
        if (btnPrevCheque != null) btnPrevCheque.setDisabled(reviewIdx <= 0);
        if (btnNextCheque != null)
            btnNextCheque.setDisabled(reviewIdx >= reviewList.size() - 1);

        boolean dateErr = c.getChequeDateOcr() == null
                || !c.getChequeDateOcr().equals(c.getChequeDate());
        boolean amtErr  = c.getAmountOcr() == null
                || (c.getAmount() != null
                    && c.getAmountOcr().compareTo(c.getAmount()) != 0);

        // Identity row
        if (lblDetailChequeNo != null) lblDetailChequeNo.setValue(nvl(c.getChequeNo()));
        if (lblDetailBank     != null) lblDetailBank.setValue(nvl(c.getPresentingBankName()));

        // Warning bar
        if (daWarningBar != null) daWarningBar.setVisible(dateErr || amtErr);

        // OCR error badge
        if (lblOcrErrorBadge != null) {
            if      (dateErr && amtErr) lblOcrErrorBadge.setValue("DATE + AMT MISMATCH");
            else if (dateErr)           lblOcrErrorBadge.setValue("DATE MISMATCH");
            else if (amtErr)            lblOcrErrorBadge.setValue("AMOUNT MISMATCH");
            else                        lblOcrErrorBadge.setValue("✓ MATCH");
        }

        // Comparison labels
        if (lblProcDate != null)
            lblProcDate.setValue(c.getChequeDate() != null
                    ? c.getChequeDate().format(FMT) : "—");
        if (lblProcAmt != null)
            lblProcAmt.setValue(c.getAmount() != null ? "₹ " + fmt(c.getAmount()) : "—");
        if (lblRcvdDate != null)
            lblRcvdDate.setValue(c.getChequeDateOcr() != null
                    ? c.getChequeDateOcr().format(FMT) : "—");
        if (lblRcvdAmt != null)
            lblRcvdAmt.setValue(c.getAmountOcr() != null
                    ? "₹ " + fmt(c.getAmountOcr()) : "—");

        // Pre-fill correction inputs with processed (correct) values
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
            numCorrectedAmt.setValue(
                    c.getAmount() != null ? c.getAmount().doubleValue() : null);
        }
        if (txtRemarks != null) txtRemarks.setValue("");

        // Load images and reset zoom/view to front on every record change
        loadChequeImages(c);
        switchImagePanel("front");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  IMAGE PANEL SWITCHING  (mirrors RejectRepairComposer exactly)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Switch the visible image panel AND reset zoom to 100%.
     * Called on record load and on view-toggle button clicks.
     */
    private void switchImagePanel(String view) {
        activeImageView = view;
        showImagePanel(view);
        // Reset zoom to 100% on every panel switch — same behaviour as Step 1
        currentZoomPct = 100;
        applyZoomToActivePanel();
        if (lblZoomLevel != null) lblZoomLevel.setValue("100%");
    }

    /** Show the correct image div; hide the others. */
    private void showImagePanel(String view) {
        if (divFrontImage != null) divFrontImage.setVisible("front".equals(view));
        if (divBackImage  != null) divBackImage.setVisible("back".equals(view));
        if (divGrayImage  != null) divGrayImage.setVisible("gray".equals(view));
        setToggleActive(btnViewFront, "front".equals(view));
        // Wire these in ZUL when Back/Gray buttons are added:
        // setToggleActive(btnViewBack,  "back".equals(view));
        // setToggleActive(btnViewGray,  "gray".equals(view));
    }

    private void setToggleActive(Button btn, boolean active) {
        if (btn == null) return;
        btn.setSclass(active ? "view-toggle-btn active" : "view-toggle-btn");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ZOOM  (mirrors RejectRepairComposer exactly)
    // ═════════════════════════════════════════════════════════════════════

    private void adjustZoom(int delta) {
        currentZoomPct = Math.max(ZOOM_MIN,
                         Math.min(ZOOM_MAX, currentZoomPct + delta));
        applyZoomToActivePanel();
    }

    private void resetZoom() {
        currentZoomPct = 100;
        applyZoomToActivePanel();
    }

    /**
     * Apply CSS transform:scale() ONLY to the currently active image panel.
     * The other two panels always get scale(1) so they don't ghost-render
     * at an unexpected zoom level when they become visible later.
     */
    private void applyZoomToActivePanel() {
        if (lblZoomLevel != null)
            lblZoomLevel.setValue(currentZoomPct + "%");

        String base    = "overflow:hidden;transform-origin:top left;"
                       + "transition:transform 0.15s ease;";
        String scaled  = base + "transform:scale(" + (currentZoomPct / 100.0) + ");";
        String neutral = base + "transform:scale(1);";

        switch (activeImageView) {
            case "front" -> {
                applyDivStyle(divFrontImage, scaled);
                applyDivStyle(divBackImage,  neutral);
                applyDivStyle(divGrayImage,  neutral);
            }
            case "back" -> {
                applyDivStyle(divFrontImage, neutral);
                applyDivStyle(divBackImage,  scaled);
                applyDivStyle(divGrayImage,  neutral);
            }
            case "gray" -> {
                applyDivStyle(divFrontImage, neutral);
                applyDivStyle(divBackImage,  neutral);
                // Preserve grayscale filter alongside zoom
                applyDivStyle(divGrayImage,
                        scaled + "filter:grayscale(100%);");
            }
            default -> {
                // Fallback — treat as front
                applyDivStyle(divFrontImage, scaled);
                applyDivStyle(divBackImage,  neutral);
                applyDivStyle(divGrayImage,  neutral);
            }
        }
    }

    private void applyDivStyle(Div div, String style) {
        if (div != null) div.setStyle(style);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ACTIONS
    // ═════════════════════════════════════════════════════════════════════

    private void doAccept() {
        if (selectedCheque == null) return;

        if (dtCorrectedDate  != null && dtCorrectedDate.getValue()  == null) {
            Messagebox.show("Please enter corrected date.", "Validation",
                    Messagebox.OK, Messagebox.ERROR);
            return;
        }
        if (numCorrectedAmt != null && numCorrectedAmt.getValue() == null) {
            Messagebox.show("Please enter corrected amount.", "Validation",
                    Messagebox.OK, Messagebox.ERROR);
            return;
        }

        // Apply corrections to entity
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
        if (txtRemarks != null)
            selectedCheque.setRemarks(txtRemarks.getValue());

        service.saveStep2Repair(selectedCheque);

        updatePendingBadge();
        moveToNextPending();
    }

    private void openRejectPopup() {
        if (selectedCheque == null) return;
        if (lblRejChqNo != null) lblRejChqNo.setValue(nvl(selectedCheque.getChequeNo()));
        if (lblRejAmt   != null) lblRejAmt.setValue(
                selectedCheque.getAmount() != null
                        ? "₹ " + fmt(selectedCheque.getAmount()) : "—");
        if (cmbRejectReason  != null) cmbRejectReason.setSelectedItem(null);
        if (txtRejectRemarks != null) txtRejectRemarks.setValue("");
        if (rejectReasonPopup != null) rejectReasonPopup.setVisible(true);
    }

    private void doConfirmReject() {
        if (cmbRejectReason != null && cmbRejectReason.getSelectedItem() == null) {
            Messagebox.show("Please select a reject reason.", "Validation",
                    Messagebox.OK, Messagebox.ERROR);
            return;
        }
        if (selectedCheque == null) return;

        String reason  = (cmbRejectReason != null && cmbRejectReason.getSelectedItem() != null)
                ? cmbRejectReason.getSelectedItem().getValue().toString() : "";
        String remarks = (txtRejectRemarks != null) ? txtRejectRemarks.getValue() : "";

        service.rejectStep2(selectedCheque,
                reason + (remarks.isBlank() ? "" : " | " + remarks));

        if (rejectReasonPopup != null) rejectReasonPopup.setVisible(false);

        selectedCheque.setRepairStatus("REJECTED");
        updatePendingBadge();
        moveToNextPending();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  AUTO-ADVANCE TO NEXT PENDING
    // ═════════════════════════════════════════════════════════════════════

    private void moveToNextPending() {
        if (reviewList == null) { showList(); return; }

        // Scan forward from next position
        for (int i = reviewIdx + 1; i < reviewList.size(); i++) {
            if (isPending(reviewList.get(i))) {
                reviewIdx = i;
                loadReviewRecord();
                return;
            }
        }
        // Wrap around
        for (int i = 0; i < reviewIdx; i++) {
            if (isPending(reviewList.get(i))) {
                reviewIdx = i;
                loadReviewRecord();
                return;
            }
        }

        // All done — prompt to proceed
        Messagebox.show(
                "All Date & Amount reviews completed.\nProceed to Step 3: Payee & Account?",
                "Step 2 Complete",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
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
        return !"DATE_AMT_REPAIRED".equalsIgnoreCase(s)
            && !"REPAIRED".equalsIgnoreCase(s)
            && !"REJECTED".equalsIgnoreCase(s)
            && !"ENTRY_DONE".equalsIgnoreCase(s)
            && !"SUBMITTED_TO_CHECKER".equalsIgnoreCase(s);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  WIZARD NAVIGATION (@Listen)
    // ═════════════════════════════════════════════════════════════════════

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

    // ═════════════════════════════════════════════════════════════════════
    //  IMAGE LOADING
    // ═════════════════════════════════════════════════════════════════════

    private void loadChequeImages(InwardCheque c) {
        setImageViaServlet(imgFront, c.getFrontImagePath());
        setImageViaServlet(imgBack,  c.getBackImagePath());
        // Grayscale = same front image, CSS filter applied via applyZoomToActivePanel()
        setImageViaServlet(imgGray,  c.getFrontImagePath());
    }

    private void setImageViaServlet(Image img, String path) {
        if (img == null) return;
        if (path == null || path.isBlank()) { img.setSrc(""); return; }
        try {
            img.setSrc("/imageServlet?path=" + URLEncoder.encode(path.trim(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            img.setSrc("/imageServlet?path=" + path.trim());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  FILTERING & PAGINATION HELPERS
    // ═════════════════════════════════════════════════════════════════════

    private List<InwardCheque> getFilteredList() {
        if (allCheques == null) return Collections.emptyList();

        final String filterVal =
                (cmbFilter != null && cmbFilter.getSelectedItem() != null
                 && cmbFilter.getSelectedItem().getValue() != null)
                        ? cmbFilter.getSelectedItem().getValue().toString().trim()
                        : "";

        return allCheques.stream()
                .filter(c -> {
                    if (!filterVal.isEmpty()) {
                        String s = c.getRepairStatus() != null
                                ? c.getRepairStatus() : "NEEDS_REPAIR";
                        return s.equalsIgnoreCase(filterVal);
                    }
                    // Default: show everything except fully done
                    String s = c.getRepairStatus();
                    return !"ENTRY_DONE".equalsIgnoreCase(s)
                        && !"SUBMITTED_TO_CHECKER".equalsIgnoreCase(s);
                })
                .collect(Collectors.toList());
    }

    private int totalPages() {
        return Math.max(1,
                (int) Math.ceil((double) getFilteredList().size() / PAGE_SIZE));
    }

    // ═════════════════════════════════════════════════════════════════════
    //  BADGE HELPERS
    // ═════════════════════════════════════════════════════════════════════

    private void updateBatchBadge() {
        if (lblBatchBadge  != null) lblBatchBadge.setValue("BATCH: " + currentBatchId);
        if (lblBatchBadge2 != null) lblBatchBadge2.setValue("BATCH: " + currentBatchId);
    }

    private void updatePendingBadge() {
        if (allCheques == null) return;
        long pending = allCheques.stream().filter(this::isPending).count();
        if (lblPendingBadge  != null) lblPendingBadge.setValue(pending + " PENDING");
        if (lblPendingBadge2 != null) lblPendingBadge2.setValue(pending + " PENDING");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  SESSION HELPERS
    // ═════════════════════════════════════════════════════════════════════

    private int getSessionMaxStep() {
        Object v = Sessions.getCurrent().getAttribute(SESSION_MAX_STEP);
        return (v instanceof Integer i) ? i : 2;
    }

    private void setSessionMaxStep(int step) {
        if (step > getSessionMaxStep())
            Sessions.getCurrent().setAttribute(SESSION_MAX_STEP, step);
    }

    private String batchParam() {
        if (currentBatchId == null) return "";
        try {
            return "?batchId=" + URLEncoder.encode(currentBatchId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "?batchId=" + currentBatchId;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CELL / STATUS HELPERS
    // ═════════════════════════════════════════════════════════════════════

    private void addCell(Listitem row, String text) {
        row.appendChild(new Listcell(text != null ? text : "—"));
    }

    private String nvl(String v) {
        return (v != null && !v.isBlank()) ? v : "—";
    }

    private String fmt(BigDecimal v) {
        return v != null ? String.format("%,.2f", v) : "—";
    }

    private String resolveStatusLabel(String s) {
        if (s == null || s.isEmpty()) return "NEEDS REPAIR";
        return switch (s.toUpperCase()) {
            case "DATE_AMT_REPAIRED"   -> "DATE & AMT REPAIRED";
            case "REPAIRED"            -> "REPAIRED";
            case "REFERRED_DATEAMOUNT" -> "REFERRED BACK";
            case "REJECTED"            -> "REJECTED";
            default                    -> "NEEDS REPAIR";
        };
    }

    private String resolveStatusSclass(String s) {
        if (s == null || s.isEmpty()) return "badge-needs-repair";
        return switch (s.toUpperCase()) {
            case "DATE_AMT_REPAIRED",
                 "REPAIRED"            -> "badge-repaired";
            case "REFERRED_DATEAMOUNT" -> "badge-referred";
            case "REJECTED"            -> "badge-fail";
            default                    -> "badge-needs-repair";
        };
    }

    // ── Amount in words ───────────────────────────────────────────────────
    private static final String[] ONES = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
        "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty",
        "Sixty", "Seventy", "Eighty", "Ninety"
    };

    @SuppressWarnings("unused")
    private String amountInWords(BigDecimal amount) {
        if (amount == null) return "—";
        long val = amount.longValue();
        return val == 0 ? "Zero Only" : toWords(val) + " Only";
    }

    private String toWords(long n) {
        if (n < 20)       return ONES[(int) n];
        if (n < 100)      return TENS[(int)(n / 10)]
                + (n % 10 != 0 ? " " + ONES[(int)(n % 10)] : "");
        if (n < 1_000)    return ONES[(int)(n / 100)] + " Hundred"
                + (n % 100 != 0 ? " " + toWords(n % 100) : "");
        if (n < 100_000)  return toWords(n / 1_000) + " Thousand"
                + (n % 1_000 != 0 ? " " + toWords(n % 1_000) : "");
        if (n < 10_000_000) return toWords(n / 100_000) + " Lakh"
                + (n % 100_000 != 0 ? " " + toWords(n % 100_000) : "");
        return toWords(n / 10_000_000) + " Crore"
                + (n % 10_000_000 != 0 ? " " + toWords(n % 10_000_000) : "");
    }
}