package com.iispl.composer;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;

import com.iispl.dto.InwardBatchDto;
import com.iispl.service.InwardBatchService;
import com.iispl.serviceImpl.InwardBatchServiceImpl;

public class MakerInwardDashboardComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Wire
    private Listbox pendingBatches;

    @Wire
    private Label emptyBatchesMsg;
    
    @Wire private Label lblPendingCount;
    @Wire private Label lblClearedCount;
    @Wire private Label lblTotalCount;


    private List<InwardBatchDto> allBatches;

    private final InwardBatchService service =
            new InwardBatchServiceImpl();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        loadBatches();
    }

    private void loadBatches() {

        allBatches = service.getAllBatcheDtos();

        pendingBatches.getItems().clear();

        if (allBatches == null || allBatches.isEmpty()) {
            pendingBatches.setVisible(false);
            emptyBatchesMsg.setVisible(true);
            lblPendingCount.setValue("0");
            lblClearedCount.setValue("0");
            lblTotalCount.setValue("0");
            return;
        }

        pendingBatches.setVisible(true);
        emptyBatchesMsg.setVisible(false);

        long pending = allBatches.stream()
            .filter(b -> "RECEIVED".equals(b.getStatus()) || "PARSED".equals(b.getStatus()))
            .count();

        long cleared = allBatches.stream()
            .filter(b -> "APPROVED".equals(b.getStatus()))
            .count();

        lblPendingCount.setValue(String.valueOf(pending));
        lblClearedCount.setValue(String.valueOf(cleared));
        lblTotalCount.setValue(String.valueOf(allBatches.size()));

        for (InwardBatchDto dto : allBatches) {
            appendRow(dto);
        }
    }

    private void appendRow(InwardBatchDto dto) {

        Listitem item = new Listitem();

        item.appendChild(new Listcell(dto.getBatchId()));
        item.appendChild(new Listcell(
                String.valueOf(dto.getTotalCheques())));
        item.appendChild(new Listcell(
                String.valueOf(dto.getMicrErrorCount())));

        item.appendChild(new Listcell(
                dto.getParsedAt() != null
                        ? dto.getParsedAt().format(FMT)
                        : "—"));

        item.appendChild(new Listcell(dto.getStatus()));

        Button repairBtn = new Button("Repair");
        repairBtn.setSclass("btn-view");

        Listcell actionCell = new Listcell();
        actionCell.appendChild(repairBtn);

        item.appendChild(actionCell);

        repairBtn.addEventListener("onClick", event -> {
            Executions.sendRedirect(
                    "/inward/inwardMicr/RejectRepair.zul");
        });

        pendingBatches.appendChild(item);
    }

    @Listen("onClick = #btnFileProcessing")
    public void onFileProcessing() {
        Executions.getCurrent()
                .sendRedirect("/inward/bpxfUpload/bpxfUpload.zul");
    }

    @Listen("onClick = #btnRejectRepair")
    public void onRejectRepair() {
        Executions.getCurrent()
                .sendRedirect("/inward/inwardMicr/RejectRepair.zul");
    }
    
    @Listen("onClick = #btnPendingOverlay")
    public void onPendingOverlay() {
        Executions.getCurrent().sendRedirect("/inward/inwardMicr/RejectRepair.zul");
    }

    @Listen("onClick = #btnClearedOverlay")
    public void onClearedOverlay() {
        Executions.getCurrent().sendRedirect("/inward/bpxfUpload/bpxfUpload.zul"); // replace with your cleared batches page
    }

    @Listen("onClick = #btnTotalOverlay")
    public void onTotalOverlay() {
        Executions.getCurrent().sendRedirect("/inward/bpxfUpload/bpxfUpload.zul"); // replace with your all batches page
    }
}