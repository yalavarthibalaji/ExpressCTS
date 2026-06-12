package com.iispl.composer.outward;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
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

public class BatchUploadComposer extends SelectorComposer<Component> {

    private final BatchUploadService service  = new BatchUploadServiceImpl();
    private final DecimalFormat      moneyFmt = new DecimalFormat("#,##0.00");

    // ── Upload Form ──
    @Wire private Decimalbox expectedAmountBox;
    @Wire private Intbox     expectedCountBox;
    @Wire private Label      fileNameLabel;
    @Wire private Label      formErrorLabel;
    @Wire private Button     processBtn;

    // ── Result Section ──
    @Wire private Div     batchResultSection;
    @Wire private Label   batchNoteLabel;
    @Wire private Label   batchCountBadge;
    @Wire private Rows    batchRows;

    // ── Filter Controls ──
    @Wire private Textbox batchSearchBox;
    @Wire private Listbox statusFilterBox;
    

    // ── Pagination ──
    @Wire private Div    batchPager;
    @Wire private Button btnPrevPage;
    @Wire private Button btnNextPage;
    @Wire private Label  pagerInfo;

    // ── Mismatch Modal ──
    @Wire private Div   mismatchModal;
    @Wire private Label mismatchExpectedCount;
    @Wire private Label mismatchExpectedAmount;
    @Wire private Label mismatchParsedCount;
    @Wire private Label mismatchParsedAmount;

    // ── State ──
    private BatchUploadResult  lastResult;
    private OutwardBatch       lastAddedBatch;
    private File               uploadedFile;
    private String             uploadedFileName;
    private Long               currentMakerId;

    // ── Pagination State ──
    private static final int    PAGE_SIZE          = 3;
    private int                 currentPage        = 0;
    private List<OutwardBatch>  allBatches         = new ArrayList<>();
    private List<OutwardBatch>  currentDisplayList = new ArrayList<>();

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

        currentMakerId = dto.getUserId();

        formErrorLabel.setVisible(false);
        batchResultSection.setVisible(false);
        mismatchModal.setSclass("modal-ov");
        processBtn.setDisabled(true);

        loadBatches();
    }

    // ════════════════════════════════════════════════════
    //  Load Batches
    // ════════════════════════════════════════════════════

    private void loadBatches() {
        List<OutwardBatch> list = service.getMyBatches(currentMakerId);
        allBatches = (list != null) ? list : new ArrayList<>();

        if (!allBatches.isEmpty()) {
            batchNoteLabel.setValue("✓ " + allBatches.size() + " batches found.");
            batchNoteLabel.setSclass("note suc mb12");
        }

        applyFiltersAndRender();
    }

    // ════════════════════════════════════════════════════
    //  Filter + Render
    // ════════════════════════════════════════════════════

    private void applyFiltersAndRender() {
        String search = batchSearchBox != null
                ? batchSearchBox.getValue().trim().toLowerCase() : "";
        String status = getSelectedStatus();

        currentDisplayList = allBatches.stream()
                .filter(b -> {
                    boolean matchSearch = search.isEmpty()
                            || (b.getBatchId() != null
                                && b.getBatchId().toLowerCase().contains(search));
                    boolean matchStatus = status.isEmpty()
                            || status.equalsIgnoreCase(
                                b.getStatus() != null ? b.getStatus() : "");
                    return matchSearch && matchStatus;
                })
                .collect(Collectors.toList());

        currentPage = 0;
        renderPage();
    }

    private void renderPage() {
        batchRows.getChildren().clear();

        int totalFiltered = currentDisplayList.size();
        batchCountBadge.setValue(totalFiltered + " Batches");

        if (totalFiltered == 0) {
            batchPager.setVisible(false);
            if (!allBatches.isEmpty()) batchResultSection.setVisible(true);
            return;
        }

        int totalPages = (int) Math.ceil((double) totalFiltered / PAGE_SIZE);

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0)           currentPage = 0;

        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, totalFiltered);

        List<OutwardBatch> pageData = currentDisplayList.subList(fromIndex, toIndex);

        for (OutwardBatch b : pageData) {
            batchRows.appendChild(buildBatchRow(b));
        }

        batchResultSection.setVisible(true);

        batchPager.setVisible(totalPages > 1);
        pagerInfo.setValue("Page " + (currentPage + 1) + " of " + totalPages
                + "  (" + totalFiltered + " batches)");
        btnPrevPage.setDisabled(currentPage == 0);
        btnNextPage.setDisabled(currentPage >= totalPages - 1);
    }

    // ════════════════════════════════════════════════════
    //  Pagination Listeners
    // ════════════════════════════════════════════════════

    @Listen("onClick = #btnPrevPage")
    public void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        }
    }

    @Listen("onClick = #btnNextPage")
    public void onNextPage() {
        if (currentDisplayList != null) {
            int totalPages = (int) Math.ceil(
                    (double) currentDisplayList.size() / PAGE_SIZE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                renderPage();
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  Filter Listeners
    // ════════════════════════════════════════════════════

    @Listen("onChanging = #batchSearchBox; onChange = #batchSearchBox")
    public void onSearchChange() { applyFiltersAndRender(); }
    
    
    @Listen("onClick = #btnApplyFilter")
    public void onApplyFilter() { applyFiltersAndRender(); }
    

    @Listen("onClick = #btnClearFilter")
    public void onClearFilter() {
        batchSearchBox.setValue("");
        statusFilterBox.setSelectedIndex(0);
        applyFiltersAndRender();
    }

    // ════════════════════════════════════════════════════
    //  Navigation
    // ════════════════════════════════════════════════════

    @Listen("onClick = #gotoViewBatches")
    public void gotoViewBatches() {
        Executions.sendRedirect("/outward/viewBatches/viewBatches.zul");
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

        addNewBatch(lastResult);

        if (lastResult.isHasMismatch()) {
            showMismatchModal();
        } else {
            batchNoteLabel.setValue(
                "✓ Batch " + lastResult.getBatchId()
                + " created — " + lastResult.getParsedChequeCount()
                + " cheques | ₹" + moneyFmt.format(lastResult.getParsedTotalAmount()));
            batchNoteLabel.setSclass("note suc mb12");
            Clients.showNotification(
                "✓ Batch " + lastResult.getBatchId() + " processed.",
                "info", null, "top_center", 2500);
            resetForm();
        }
    }

    // ════════════════════════════════════════════════════
    //  Add New Batch to List
    // ════════════════════════════════════════════════════

    private void addNewBatch(BatchUploadResult result) {
        OutwardBatch b = new OutwardBatch();
        b.setId(result.getBatchDbId());
        b.setBatchId(result.getBatchId());
        b.setChequeCount(result.getParsedChequeCount());
        b.setActualAmount(BigDecimal.valueOf(result.getParsedTotalAmount()));
        b.setStatus(result.isHasMicrErrors() ? "NEEDS_REPAIR" : "ENTRY_PENDING");

        allBatches.add(0, b);
        lastAddedBatch = b;

        applyFiltersAndRender();
    }

    // ════════════════════════════════════════════════════
    //  Build Batch Row
    // ════════════════════════════════════════════════════

    private Row buildBatchRow(final OutwardBatch b) {
        int    total     = b.getChequeCount();
        String st        = b.getStatus() != null ? b.getStatus() : "";
        int    processed;
        switch (st) {
            case "CHECKER_APPROVED":
            case "EXPORTED":
            case "SUBMITTED":
                processed = total;
                break;
            default:
                processed = 0;
        }
        int pending = total - processed;

        Row row = new Row();

        // Batch ID
        Label batchIdLbl = new Label(b.getBatchId() != null ? b.getBatchId() : "—");
        batchIdLbl.setSclass("mono fw6");
        row.appendChild(batchIdLbl);

        // Cheque Count
        row.appendChild(cell(String.valueOf(total)));

        // Amount
        row.appendChild(cell(b.getActualAmount() != null
            ? "₹" + moneyFmt.format(b.getActualAmount()) : "—"));

        // Pending
        Label pendingLbl = new Label(String.valueOf(pending));
        pendingLbl.setSclass(pending > 0 ? "txt-warn fw6" : "txt-success");
        row.appendChild(pendingLbl);

        // Processed
        Label processedLbl = new Label(String.valueOf(processed));
        processedLbl.setSclass(processed > 0 ? "txt-success fw6" : "txt-muted");
        row.appendChild(processedLbl);

        // Status badge
        row.appendChild(buildStatusBadge(b.getStatus()));

        // Action buttons
        row.appendChild(buildActionBtns(b));

        return row;
    }

    private Label cell(String text) {
        return new Label(text != null ? text : "—");
    }

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
            case "CHECKER_IN_PROGRESS":
                badge.setValue("Checker In Progress");
                badge.setSclass("badge b-info");
                break;
            case "CHECKER_HOLD":
                badge.setValue("On Hold");
                badge.setSclass("badge b-ref");
                break;
            case "CHECKER_APPROVED":
                badge.setValue("Approved");
                badge.setSclass("badge b-pass");
                break;
            case "EXPORTED":
                badge.setValue("Exported");
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

    private Div buildActionBtns(final OutwardBatch b) {
        Div ac = new Div();
        ac.setSclass("bu-action-btns");

        // View button — always present
        Button viewBtn = new Button("View");
        viewBtn.setSclass("btn bo btn-sm");
        viewBtn.addEventListener(Events.ON_CLICK,
            new EventListener<Event>() {
                @Override public void onEvent(Event e) {
                    Executions.sendRedirect(
                        "/outward/viewBatches/viewBatches.zul?batchId="
                        + b.getBatchId());
                }
            });
        ac.appendChild(viewBtn);

        // Action button based on status
        String status = b.getStatus() != null ? b.getStatus() : "";
        Button actionBtn = null;

        switch (status) {
            case "NEEDS_REPAIR":
                actionBtn = new Button("MICR Repair");
                actionBtn.setSclass("btn bp btn-sm");
                final String micrBatchId = b.getBatchId();
                actionBtn.addEventListener(Events.ON_CLICK,
                    new EventListener<Event>() {
                        @Override public void onEvent(Event e) {
                            Executions.sendRedirect(
                                "/outward/micrRepair/micrRepair.zul?batchId="
                                + micrBatchId);
                        }
                    });
                break;

            case "ENTRY_PENDING":
                actionBtn = new Button("Data Entry");
                actionBtn.setSclass("btn bp btn-sm");
                final String entryBatchId = b.getBatchId();
                actionBtn.addEventListener(Events.ON_CLICK,
                    new EventListener<Event>() {
                        @Override public void onEvent(Event e) {
                            Executions.sendRedirect(
                                "/outward/acctAmount/acctAmount.zul?batchId="
                                + entryBatchId);
                        }
                    });
                break;

            case "REFER_BACK":
                actionBtn = new Button("Re-Process");
                actionBtn.setSclass("btn bw btn-sm");
                final String referBatchId = b.getBatchId();
                actionBtn.addEventListener(Events.ON_CLICK,
                    new EventListener<Event>() {
                        @Override public void onEvent(Event e) {
                            Executions.sendRedirect(
                                "/outward/acctAmount/acctAmount.zul?batchId="
                                + referBatchId);
                        }
                    });
                break;

            default:
                // SUBMITTED / CHECKER_* / EXPORTED / REJECTED — no action
                break;
        }

        if (actionBtn != null) ac.appendChild(actionBtn);
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
    public void onMismatchClose() { mismatchModal.setSclass("modal-ov"); }

    @Listen("onClick = #mismatchAcceptBtn")
    public void onMismatchAccept() {
        mismatchModal.setSclass("modal-ov");
        batchNoteLabel.setValue(
            "✓ Batch " + lastResult.getBatchId()
            + " accepted — " + lastResult.getParsedChequeCount()
            + " cheques | ₹" + moneyFmt.format(lastResult.getParsedTotalAmount()));
        batchNoteLabel.setSclass("note suc mb12");
        Clients.showNotification(
            "✓ Batch accepted with " + lastResult.getParsedChequeCount() + " cheques.",
            "info", null, "top_center", 2500);
        resetForm();
    }

    @Listen("onClick = #mismatchRejectBtn")
    public void onMismatchReject() {
        mismatchModal.setSclass("modal-ov");

        if (lastResult != null && lastResult.getBatchDbId() != null) {
            service.rejectBatch(lastResult.getBatchDbId());
        }

        // Remove last added batch from the list
        if (lastAddedBatch != null) {
            allBatches.remove(lastAddedBatch);
            lastAddedBatch = null;
        }

        applyFiltersAndRender();

        if (allBatches.isEmpty()) batchResultSection.setVisible(false);

        resetForm();
        Clients.showNotification(
            "⚠ Batch rejected. Please re-upload the correct file.",
            "warning", null, "top_center", 3500);
    }

    // ════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════

    private String getSelectedStatus() {
        if (statusFilterBox.getSelectedItem() == null) return "";
        Object val = statusFilterBox.getSelectedItem().getValue();
        return val != null ? val.toString() : "";
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