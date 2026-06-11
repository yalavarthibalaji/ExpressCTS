package com.iispl.composer;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.dto.InwardBatchDto;
import com.iispl.dto.LoginDTO;
import com.iispl.service.BpxfFolderWatchService;
import com.iispl.service.BpxfUploadService;
import com.iispl.serviceImpl.BpxfUploadServiceImpl;
import com.iispl.util.SessionUtil;

public class BpxfUploadComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ── Wired Components ──────────────────────────────────────────────────

    @Wire("#batchNameBox")       private Textbox batchNameBox;
    @Wire("#fileNameDisplay")    private Textbox fileNameDisplay;
    @Wire("#searchBox")          private Textbox searchBox;
    @Wire("#fileListbox")        private Listbox fileListbox;
    @Wire("#watcherStatusLabel") private Label   watcherStatusLabel;
    @Wire("#watcherDot")         private Div     watcherDot;
    @Wire("#tabManual")          private Div     tabManual;
    @Wire("#tabAuto")            private Div     tabAuto;
    @Wire("#panelManual")        private Div     panelManual;
    @Wire("#panelAuto")          private Div     panelAuto;

    // ── State ─────────────────────────────────────────────────────────────

    private Media                uploadedFile = null;
    private List<InwardBatchDto> allBatches   = null;

    // ── Services ──────────────────────────────────────────────────────────

    private final BpxfUploadService    service      = new BpxfUploadServiceImpl();
    private Thread                     watchThread  = null;
    private BpxfFolderWatchService     watchService = null;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        loadBatches();
        startWatcher();
        updateWatcherStatus();
        subscribeToQueue();
    }

    // =====================================================================
    // TAB SWITCHING
    // =====================================================================

    @Listen("onClick = #tabManual")
    public void onTabManual() {
        tabManual.setSclass("mode-tab active");
        tabAuto.setSclass("mode-tab");
        panelManual.setVisible(true);
        panelAuto.setVisible(false);
    }

    @Listen("onClick = #tabAuto")
    public void onTabAuto() {
        tabAuto.setSclass("mode-tab active");
        tabManual.setSclass("mode-tab");
        panelAuto.setVisible(true);
        panelManual.setVisible(false);
    }

    // =====================================================================
    // FILE UPLOAD
    // =====================================================================

    @Listen("onUpload = #uploadBtn")
    public void onUpload(org.zkoss.zk.ui.event.UploadEvent event) {
        Media media = event.getMedia();
        if (media != null) {
            uploadedFile = media;
            fileNameDisplay.setValue(media.getName());
        }
    }

    // =====================================================================
    // PARSE — with duplicate file check
    // =====================================================================

    @Listen("onClick = #parseBtn")
    public void onParse() throws InterruptedException {

        // ── Validate: file must be selected ─────────────────────────────
        if (uploadedFile == null) {
            Messagebox.show(
                    "Please upload a file first.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);
            return;
        }

        // ── Validate: user session ───────────────────────────────────────
        LoginDTO operator = SessionUtil.getCurrentUser();
        if (operator == null) {
            Executions.sendRedirect("/login/login.zul");
            return;
        }

        // ── Duplicate check — compare against already loaded batches ─────
        String incomingFileName = uploadedFile.getName().trim();
        if (allBatches != null) {
            boolean alreadyUploaded = allBatches.stream()
                    .anyMatch(b -> incomingFileName
                            .equalsIgnoreCase(b.getSourceFileName()));
            if (alreadyUploaded) {
                Messagebox.show(
                        "\"" + incomingFileName + "\" has already been uploaded.\n\n"
                        + "Each file can only be processed once.\n"
                        + "Please upload a different file.",
                        "Duplicate File",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION);
                return;
            }
        }

        // ── Parse and save ───────────────────────────────────────────────
        try {
            String batchName = batchNameBox.getValue().trim();
            service.parseAndSave(uploadedFile, batchName, operator);

            // Reset form
            uploadedFile = null;
            fileNameDisplay.setValue("");
            batchNameBox.setValue("");

            // Reload table
            loadBatches();

            Messagebox.show(
                    "File parsed and saved successfully.",
                    "Success",
                    Messagebox.OK,
                    Messagebox.INFORMATION);

        } catch (Exception e) {
            // ── Fallback duplicate check from DB constraint violation ────
            String msg = e.getMessage() != null
                    ? e.getMessage().toLowerCase() : "";

            if (msg.contains("duplicate")
                    || msg.contains("already exists")
                    || msg.contains("pkey")
                    || msg.contains("unique constraint")) {
                Messagebox.show(
                        "This file has already been uploaded and processed.\n\n"
                        + "Duplicate files cannot be re-imported.",
                        "Duplicate File",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION);
            } else {
                Messagebox.show(
                        "Error: " + e.getMessage(),
                        "Parse Failed",
                        Messagebox.OK,
                        Messagebox.ERROR);
            }
        }
    }

    // =====================================================================
    // SEARCH
    // =====================================================================

    @Listen("onChanging = #searchBox")
    public void onSearch() {
        String keyword = searchBox.getValue().toLowerCase().trim();
        fileListbox.getItems().clear();
        if (allBatches == null) return;
        for (InwardBatchDto dto : allBatches) {
            if (dto.getBatchId().toLowerCase().contains(keyword)
                    || dto.getSourceFileName().toLowerCase().contains(keyword)) {
                appendRow(dto);
            }
        }
    }

    // =====================================================================
    // WATCHER
    // =====================================================================

    private void startWatcher() {
        LoginDTO operator = SessionUtil.getCurrentUser();
        if (operator == null) return;

        watchService = new BpxfFolderWatchService(operator);
        watchThread  = new Thread(watchService, "bpxf-folder-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    private void updateWatcherStatus() {
        if (watchThread != null && watchThread.isAlive()) {
            watcherStatusLabel.setValue("Active");
            watcherStatusLabel.setSclass("watcher-status-text");
            watcherDot.setSclass("watcher-dot active");
        } else {
            watcherStatusLabel.setValue("Inactive");
            watcherStatusLabel.setSclass("watcher-status-text inactive");
            watcherDot.setSclass("watcher-dot inactive");
        }
    }

    private void subscribeToQueue() {
        EventQueue<Event> queue = EventQueues.lookup(
                BpxfFolderWatchService.QUEUE_NAME,
                EventQueues.APPLICATION,
                true);

        queue.subscribe(event -> {
            String fileName = (String) event.getData();
            loadBatches();
            Messagebox.show(
                    "Auto-imported: " + fileName,
                    "New File Detected",
                    Messagebox.OK,
                    Messagebox.INFORMATION);
        }, true);
    }

    // =====================================================================
    // TABLE
    // =====================================================================

    private void loadBatches() {
        allBatches = service.getAllBatches();
        fileListbox.getItems().clear();
        for (InwardBatchDto dto : allBatches) {
            appendRow(dto);
        }
    }

    private void appendRow(InwardBatchDto dto) {
        Listitem item = new Listitem();

        item.appendChild(new Listcell(dto.getBatchId()));
        item.appendChild(new Listcell(dto.getSourceFileName()));
        item.appendChild(new Listcell(String.valueOf(dto.getTotalCheques())));
        item.appendChild(new Listcell(String.valueOf(dto.getMicrErrorCount())));
        item.appendChild(new Listcell(
                dto.getParsedAt() != null ? dto.getParsedAt().format(FMT) : "—"));
        item.appendChild(new Listcell(dto.getStatus()));

        // Action buttons
        Listcell actionCell = new Listcell();

        Button viewBtn = new Button("View");
        viewBtn.setSclass("btn-view");
        viewBtn.addEventListener("onClick", event ->
                Executions.sendRedirect("/inward/viewBatches/viewBatches.zul"));
        actionCell.appendChild(viewBtn);

        Button repairBtn = new Button("Repair");
        repairBtn.setSclass("btn-view");
        repairBtn.addEventListener("onClick", event ->
                Executions.sendRedirect(
                        "/inward/inwardMicr/RejectRepair.zul"
                        + "?batchId=" + dto.getBatchId()));
        actionCell.appendChild(repairBtn);

        item.appendChild(actionCell);
        fileListbox.appendChild(item);
    }
}