package com.iispl.composer.outward;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import com.iispl.dto.BatchUploadResult;
import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.service.BatchUploadService;
import com.iispl.serviceImpl.BatchUploadServiceImpl;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/outward/BatchUploadComposer.java
 * Purpose : Handles Batch Upload screen for Maker Outward.
 *
 * Batch Status Flow:
 *   NEEDS_REPAIR  → MICR Repair screen
 *   ENTRY_PENDING → Account & Amount Entry screen
 *   REFER_BACK    → Account & Amount Entry screen (re-enter)
 *   SUBMITTED     → No action (in checker queue)
 */
public class BatchUploadComposer extends SelectorComposer<Component> {

    private final BatchUploadService service  = new BatchUploadServiceImpl();
    private final DecimalFormat      moneyFmt = new DecimalFormat("#,##0.00");

    // ── Topbar ──
    @Wire private Label  userAvatar;
    @Wire private Label  userName;
    @Wire private Label  userRole;

    // ── Form ──
    @Wire private Decimalbox expectedAmountBox;
    @Wire private Intbox     expectedCountBox;
    @Wire private Label      fileNameLabel;
    @Wire private Label      formErrorLabel;
    @Wire private Button     processBtn;

    // ── Result section ──
    @Wire private Div     batchResultSection;
    @Wire private Label   batchNoteLabel;
    @Wire private Rows    batchRows;
    @Wire private Label   pagerInfo;
    @Wire private Textbox batchSearchBox;
    @Wire private Listbox statusFilterBox;

    // ── Mismatch modal ──
    @Wire private Div   mismatchModal;
    @Wire private Label mismatchExpectedCount;
    @Wire private Label mismatchExpectedAmount;
    @Wire private Label mismatchParsedCount;
    @Wire private Label mismatchParsedAmount;

    // ── State ──
    private BatchUploadResult lastResult;
    private File              uploadedFile;
    private String            uploadedFileName;
    private Long              currentMakerId;
    private Row               lastAddedRow;
    private int               rowSeqCounter = 0;

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

        formErrorLabel.setVisible(false);
        batchResultSection.setVisible(false);
        mismatchModal.setSclass("modal-ov");
        processBtn.setDisabled(true);

        attachFilterListeners();
        loadBatches();
    }

    // ════════════════════════════════════════════════════
    //  Load Batches
    // ════════════════════════════════════════════════════

    private void loadBatches() {
        List<OutwardBatch> list = service.getMyBatches(currentMakerId);
        if (list == null || list.isEmpty()) return;

        batchRows.getChildren().clear();
        rowSeqCounter = 0;

        for (OutwardBatch b : list) {
            batchRows.appendChild(buildBatchRow(b, false));
        }

        batchResultSection.setVisible(true);
        pagerInfo.setValue("Showing " + list.size() + " batch(es)");
        batchNoteLabel.setValue("✓ " + list.size() + " batch(es) found.");
        batchNoteLabel.setSclass("note suc mb12");
    }

    // ════════════════════════════════════════════════════
    //  Topbar
    // ════════════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }

    // ════════════════════════════════════════════════════
    //  File Upload
    // ════════════════════════════════════════════════════

    @Listen("onUpload = #uploadFileBtn")
    public void onFileSelect(UploadEvent event) {
        Media media = event.getMedia();
        if (media == null) { showError("No file selected."); return; }
        String name = media.getName();
        if (name == null || !name.toLowerCase().endsWith(".zip")) {
            showError("Only ZIP files are accepted.");
            return;
        }
        try {
            uploadedFile = File.createTempFile("cts_upload_", ".zip");
            uploadedFile.deleteOnExit();
            try (InputStream      in  = media.getStreamData();
                 FileOutputStream out = new FileOutputStream(uploadedFile)) {
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            fileNameLabel.setValue(name);
            uploadedFileName = name;
            formErrorLabel.setVisible(false);
            processBtn.setDisabled(false);
        } catch (Exception e) {
            showError("Failed to read file: " + e.getMessage());
            uploadedFile = null;
            processBtn.setDisabled(true);
        }
    }

    // ════════════════════════════════════════════════════
    //  Process Batch
    // ════════════════════════════════════════════════════

    @Listen("onClick = #processBtn")
    public void onProcessBatch() {
        Integer    count  = expectedCountBox.getValue();
        BigDecimal amount = expectedAmountBox.getValue();

        if (count == null || count <= 0) {
            showError("Please enter expected cheque count."); return;
        }
        if (amount == null || amount.signum() <= 0) {
            showError("Please enter expected total amount."); return;
        }
        if (uploadedFile == null || !uploadedFile.exists()) {
            showError("Please select a ZIP file first."); return;
        }

        lastResult = service.processBatchUpload(
            uploadedFile, uploadedFileName,
            count, amount.doubleValue(), currentMakerId);

        if (!lastResult.isSuccess()) {
            showError(lastResult.getErrorMessage());
            return;
        }

        addBatchRow(lastResult);

        if (lastResult.isHasMismatch()) {
            showMismatchModal();
        } else {
            batchNoteLabel.setValue(
                "✓ Batch " + lastResult.getBatchId()
                + " created successfully — "
                + lastResult.getParsedChequeCount() + " cheques | ₹"
                + moneyFmt.format(lastResult.getParsedTotalAmount()));
            batchNoteLabel.setSclass("note suc mb12");
            batchResultSection.setVisible(true);
            Clients.showNotification(
                "✓ Batch " + lastResult.getBatchId() + " processed.",
                "info", null, "top_center", 2500);
            resetForm();
        }
    }

    // ════════════════════════════════════════════════════
    //  Build Batch Row
    // ════════════════════════════════════════════════════

    private void addBatchRow(BatchUploadResult result) {
        OutwardBatch b = new OutwardBatch();
        b.setId(result.getBatchDbId());
        b.setBatchId(result.getBatchId());
        b.setChequeCount(result.getParsedChequeCount());
        b.setActualAmount(BigDecimal.valueOf(result.getParsedTotalAmount()));
        // STATUS FIX: was "ENTRY_DONE" → now "ENTRY_PENDING"
        b.setStatus(result.isHasMicrErrors() ? "NEEDS_REPAIR" : "ENTRY_PENDING");

        Row row = buildBatchRow(b, true);
        batchRows.insertBefore(row, batchRows.getFirstChild());
        lastAddedRow = row;
        renumberRows();
        batchResultSection.setVisible(true);
        refreshPager();
    }

    private Row buildBatchRow(final OutwardBatch b, boolean isNew) {
        rowSeqCounter++;

        int    total     = b.getChequeCount();
        String st        = b.getStatus() != null ? b.getStatus() : "";
        int    processed;
        switch (st) {
            case "PASSED":
            case "SUBMITTED": processed = total; break;
            default:          processed = 0;
        }
        int pending = total - processed;

        Row row = new Row();
        row.setSclass(isNew ? "row-new" : "");
        row.setAttribute("batchStatus", b.getStatus() != null ? b.getStatus() : "");

        row.appendChild(cell(String.valueOf(rowSeqCounter)));

        Label batchIdLbl = new Label(b.getBatchId());
        batchIdLbl.setSclass("mono");
        row.appendChild(batchIdLbl);

        row.appendChild(cell(String.valueOf(total)));
        row.appendChild(cell(b.getActualAmount() != null
            ? "₹" + moneyFmt.format(b.getActualAmount()) : "—"));
        row.appendChild(cell(String.valueOf(pending)));
        row.appendChild(cell(String.valueOf(processed)));
        row.appendChild(buildStatusBadge(b.getStatus()));
        row.appendChild(buildActionBtn(b));
        return row;
    }

    private Label cell(String text) {
        return new Label(text != null ? text : "—");
    }

    /** Status badge label with correct colours for all statuses. */
    private Label buildStatusBadge(String status) {
        Label badge = new Label();
        if (status == null) status = "";
        switch (status) {
            case "NEEDS_REPAIR":
                badge.setValue("Needs MICR Repair");
                badge.setSclass("badge b-pend");
                break;
            case "ENTRY_PENDING":
                badge.setValue("Pending Data Entry");
                badge.setSclass("badge b-info");
                break;
            case "REFER_BACK":
                badge.setValue("Referred Back");
                badge.setSclass("badge b-warn");
                break;
            case "SUBMITTED":
                badge.setValue("Submitted");
                badge.setSclass("badge b-pass");
                break;
            case "PASSED":
                badge.setValue("Passed");
                badge.setSclass("badge b-pass");
                break;
            case "REJECTED":
                badge.setValue("Rejected");
                badge.setSclass("badge b-fail");
                break;
            default:
                badge.setValue(status.isEmpty() ? "—" : status);
                badge.setSclass("badge b-grey");
        }
        return badge;
    }

    /** Action button that routes to the correct next screen per status. */
    private Div buildActionBtn(final OutwardBatch b) {
        Div ac = new Div();
        ac.setSclass("ac");

        // View button — always shown
        Button viewBtn = new Button("View");
        viewBtn.setSclass("btn bo btn-sm");
        viewBtn.addEventListener(Events.ON_CLICK,
            new EventListener<Event>() {
                @Override public void onEvent(Event e) {
                    Executions.sendRedirect(
                        "/outward/viewBatches/viewBatches.zul");
                }
            });
        ac.appendChild(viewBtn);

        String  status    = b.getStatus() != null ? b.getStatus() : "";
        Button  actionBtn = new Button();

        switch (status) {
            case "NEEDS_REPAIR":
                actionBtn.setLabel("MICR Repair");
                actionBtn.setSclass("btn bp btn-sm");
                actionBtn.addEventListener(Events.ON_CLICK,
                    new EventListener<Event>() {
                        @Override public void onEvent(Event e) {
                            Executions.sendRedirect(
                                "/outward/micrRepair/micrRepair.zul?batchId="
                                + b.getBatchId());
                        }
                    });
                break;

            case "ENTRY_PENDING":
                actionBtn.setLabel("Data Entry");
                actionBtn.setSclass("btn bp btn-sm");
                actionBtn.addEventListener(Events.ON_CLICK,
                    new EventListener<Event>() {
                        @Override public void onEvent(Event e) {
                            Executions.sendRedirect(
                                "/outward/acctAmount/acctAmount.zul?batchId="
                                + b.getBatchId());
                        }
                    });
                break;

            case "REFER_BACK":
                // Checker referred back — maker re-processes data entry
                actionBtn.setLabel("Re-Process");
                actionBtn.setSclass("btn bw btn-sm");
                actionBtn.addEventListener(Events.ON_CLICK,
                    new EventListener<Event>() {
                        @Override public void onEvent(Event e) {
                            Executions.sendRedirect(
                                "/outward/acctAmount/acctAmount.zul?batchId="
                                + b.getBatchId());
                        }
                    });
                break;

            default:
                // SUBMITTED, PASSED, REJECTED — no further action
                actionBtn.setLabel("—");
                actionBtn.setSclass("btn bo btn-sm");
                actionBtn.setDisabled(true);
        }

        ac.appendChild(actionBtn);
        return ac;
    }

    // ════════════════════════════════════════════════════
    //  Mismatch Modal
    // ════════════════════════════════════════════════════

    private void showMismatchModal() {
        mismatchExpectedCount.setValue(String.valueOf(lastResult.getExpectedChequeCount()));
        mismatchExpectedAmount.setValue("₹" + moneyFmt.format(lastResult.getExpectedTotalAmount()));
        mismatchParsedCount.setValue(String.valueOf(lastResult.getParsedChequeCount()));
        mismatchParsedAmount.setValue("₹" + moneyFmt.format(lastResult.getParsedTotalAmount()));
        mismatchModal.setSclass("modal-ov open");
    }

    @Listen("onClick = #mismatchCloseBtn")
    public void onMismatchClose() {
        mismatchModal.setSclass("modal-ov");
    }

    @Listen("onClick = #mismatchAcceptBtn")
    public void onMismatchAccept() {
        mismatchModal.setSclass("modal-ov");
        batchNoteLabel.setValue(
            "✓ Batch " + lastResult.getBatchId()
            + " accepted with file values — "
            + lastResult.getParsedChequeCount() + " cheques | ₹"
            + moneyFmt.format(lastResult.getParsedTotalAmount()));
        batchNoteLabel.setSclass("note suc mb12");
        Clients.showNotification(
            "✓ Batch accepted with "
            + lastResult.getParsedChequeCount() + " cheques.",
            "info", null, "top_center", 2500);
        resetForm();
    }

    @Listen("onClick = #mismatchRejectBtn")
    public void onMismatchReject() {
        mismatchModal.setSclass("modal-ov");

        if (lastResult != null && lastResult.getBatchDbId() != null) {
            service.rejectBatch(lastResult.getBatchDbId());
        }

        if (lastAddedRow != null && lastAddedRow.getParent() != null) {
            batchRows.removeChild(lastAddedRow);
            rowSeqCounter = Math.max(0, rowSeqCounter - 1);
            lastAddedRow = null;
        }

        if (batchRows.getChildren().isEmpty()) {
            batchResultSection.setVisible(false);
        } else {
            renumberRows();
            refreshPager();
        }

        resetForm();
        Clients.showNotification(
            "⚠ Batch rejected. Please re-upload the correct file.",
            "warning", null, "top_center", 3500);
    }

    // ════════════════════════════════════════════════════
    //  Filter Listeners
    // ════════════════════════════════════════════════════

    private void attachFilterListeners() {
        batchSearchBox.addEventListener(Events.ON_CHANGING,
            new EventListener<InputEvent>() {
                @Override public void onEvent(InputEvent e) {
                    applyFilters(e.getValue(), getSelectedStatus(), "");
                }
            });

        statusFilterBox.addEventListener(Events.ON_SELECT,
            new EventListener<Event>() {
                @Override public void onEvent(Event e) {
                    applyFilters(batchSearchBox.getValue(),
                                 getSelectedStatus(), "");
                }
            });
    }

    private String getSelectedStatus() {
        if (statusFilterBox.getSelectedItem() == null) return "";
        Object val = statusFilterBox.getSelectedItem().getValue();
        return val != null ? val.toString() : "";
    }

    private void applyFilters(String search, String status, String colFilter) {
        String srch = search != null ? search.trim().toLowerCase() : "";
        String stat = status != null ? status.trim().toUpperCase() : "";

        int visible = 0;
        for (Object obj : batchRows.getChildren()) {
            if (!(obj instanceof Row)) continue;
            Row row = (Row) obj;

            String batchId = "";
            int    idx     = 0;
            for (Object c : row.getChildren()) {
                if (c instanceof Label && idx == 1) {
                    batchId = ((Label) c).getValue().toLowerCase();
                    break;
                }
                idx++;
            }
            Object statusAttr = row.getAttribute("batchStatus");
            String rowStatus  = statusAttr != null
                                 ? statusAttr.toString().toUpperCase() : "";

            boolean show =
                (srch.isEmpty() || batchId.contains(srch))
                && (stat.isEmpty() || rowStatus.equals(stat));
            row.setVisible(show);
            if (show) visible++;
        }
        pagerInfo.setValue("Showing " + visible + " batch(es)");
    }

    // ════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════

    private void renumberRows() {
        int seq = 0;
        for (Object obj : batchRows.getChildren()) {
            if (!(obj instanceof Row)) continue;
            seq++;
            Object first = ((Row) obj).getChildren().isEmpty()
                           ? null : ((Row) obj).getChildren().get(0);
            if (first instanceof Label) {
                ((Label) first).setValue(String.valueOf(seq));
            }
        }
        rowSeqCounter = seq;
    }

    private void refreshPager() {
        pagerInfo.setValue("Showing " + batchRows.getChildren().size() + " batch(es)");
    }

    private void showError(String msg) {
        formErrorLabel.setValue("⚠ " + msg);
        formErrorLabel.setVisible(true);
    }

    private void resetForm() {
        expectedCountBox.setValue((Integer) null);
        expectedAmountBox.setValue((BigDecimal) null);
        fileNameLabel.setValue("No file selected");
        formErrorLabel.setVisible(false);
        uploadedFile     = null;
        uploadedFileName = null;
        lastResult       = null;
        processBtn.setDisabled(true);
    }
}