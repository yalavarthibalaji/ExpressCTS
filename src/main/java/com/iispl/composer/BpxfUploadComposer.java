package com.iispl.composer;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.dto.InwardBatchDto;
import com.iispl.dto.LoginDTO;
import com.iispl.service.BpxfUploadService;
import com.iispl.serviceImpl.BpxfUploadServiceImpl;
import com.iispl.util.SessionUtil;

public class BpxfUploadComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Wire("#batchNameBox")    private Textbox  batchNameBox;
    @Wire("#fileNameDisplay") private Textbox  fileNameDisplay;
    @Wire("#searchBox")       private Textbox  searchBox;
    @Wire("#fileListbox")     private Listbox  fileListbox;

    private Media              uploadedFile = null;
    private List<InwardBatchDto> allBatches = null;
    private final BpxfUploadService service = new BpxfUploadServiceImpl();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        loadBatches();
    }

    @Listen("onUpload = #uploadBtn")   // ✅ onUpload, not onClick
    public void onUpload(org.zkoss.zk.ui.event.UploadEvent event) {
        Media media = event.getMedia();   // ✅ get file directly from the event
        if (media != null) {
            uploadedFile = media;
            fileNameDisplay.setValue(media.getName());
        }
    }

    @Listen("onClick = #parseBtn")
    public void onParse() throws InterruptedException {
        if (uploadedFile == null) {
            Messagebox.show("Please upload a file first.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        LoginDTO operator = SessionUtil.getCurrentUser();
        if (operator == null) {
            Executions.sendRedirect("/login/login.zul");
            return;
        }

        try {
            String batchName = batchNameBox.getValue().trim();
            service.parseAndSave(uploadedFile, batchName, operator);
            uploadedFile = null;
            fileNameDisplay.setValue("");
            batchNameBox.setValue("");
            loadBatches();
            Messagebox.show("File parsed and saved successfully.", "Success",
                    Messagebox.OK, Messagebox.INFORMATION);
        } catch (Exception e) {
            Messagebox.show("Error: " + e.getMessage(), "Parse Failed",
                    Messagebox.OK, Messagebox.ERROR);
        }
    }

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

    // ── Private helpers ───────────────────────────────────────────────────

    private void loadBatches() {
        allBatches = service.getAllBatches();
        fileListbox.getItems().clear();
        for (InwardBatchDto dto : allBatches) {
            appendRow(dto);
        }
    }

    private void appendRow(InwardBatchDto dto) {
        Listitem item = new Listitem();
        item.appendChild(new Listcell(String.valueOf(fileListbox.getItemCount() + 1)));
        item.appendChild(new Listcell(dto.getBatchId()));
        item.appendChild(new Listcell(dto.getSourceFileName()));
        item.appendChild(new Listcell(String.valueOf(dto.getTotalCheques())));
        item.appendChild(new Listcell(String.valueOf(dto.getMicrErrorCount())));
        item.appendChild(new Listcell(dto.getParsedAt() != null
                ? dto.getParsedAt().format(FMT) : "—"));
        item.appendChild(new Listcell(dto.getStatus()));

        // Action button
        Button viewBtn = new Button("View");
        viewBtn.setSclass("btn-view");
        Listcell actionCell = new Listcell();
        actionCell.appendChild(viewBtn);
        item.appendChild(actionCell);

        fileListbox.appendChild(item);
    }
}