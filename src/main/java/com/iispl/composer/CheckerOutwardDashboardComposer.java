package com.iispl.composer;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.service.BatchUploadService;
import com.iispl.serviceImpl.BatchUploadServiceImpl;
import com.iispl.util.SessionUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import java.util.List;

public class CheckerOutwardDashboardComposer extends SelectorComposer<Component> {

    @Wire private Label userAvatar;
    @Wire private Label userName;
    @Wire private Label userRole;

    @Wire("#gotoVerificationQueue") private Button gotoVerificationQueue;
    @Wire("#gotoDemExport")         private Button gotoDemExport;
    @Wire("#gotoReports")           private Button gotoReports;

    @Wire private Rows  pendingBatchRows;
    @Wire private Label sumPending;
    @Wire private Label sumOnHold;
    @Wire private Label sumReadyExport;
    @Wire private Label sumExported;

    private final BatchUploadService batchService = new BatchUploadServiceImpl();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        if (!("CHECKER_OUTWARD".equals(dto.getRoleCode()))) {
            Executions.sendRedirect(SessionUtil.getDashboardUrlFor(dto.getRoleCode()));
            return;
        }

        if (userAvatar != null) userAvatar.setValue(dto.getInitials());
        if (userName   != null) userName.setValue(dto.getFullName());
        if (userRole   != null) userRole.setValue("Checker — Outward");

        if (pendingBatchRows != null) loadPendingBatches();
        if (sumPending       != null) loadSummary();
    }

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }

    @Listen("onClick = #gotoVerificationQueue")
    public void gotoVerificationQueue() {
        Executions.sendRedirect("/outward/checkerQueue/checkerQueue.zul");
    }

    @Listen("onClick = #gotoDemExport")
    public void gotoDemExport() {
        Executions.sendRedirect("/outward/demExport/demExport.zul");
    }

    @Listen("onClick = #gotoReports")
    public void gotoReports() {
        Executions.sendRedirect("/reports/reports.zul");
    }

    private void loadPendingBatches() {
        // Load all batches with SUBMITTED status — these are pending checker verification
        List<OutwardBatch> all = batchService.getMyBatches(null) != null
                ? getAllSubmittedBatches() : java.util.Collections.emptyList();

        pendingBatchRows.getChildren().clear();

        if (all.isEmpty()) {
            Row empty = new Row();
            Label lbl = new Label("No pending batches.");
            lbl.setStyle("color:var(--tm); font-size:12px;");
            empty.appendChild(lbl);
            pendingBatchRows.appendChild(empty);
            return;
        }

        int limit = Math.min(all.size(), 5);
        for (int i = 0; i < limit; i++) {
            OutwardBatch b = all.get(i);
            Row row = new Row();

            Label batchIdLbl = new Label(b.getBatchId());
            batchIdLbl.setSclass("mono");
            row.appendChild(batchIdLbl);

            row.appendChild(new Label(b.getChequeCount() + " cheques"));

            final String batchId = b.getBatchId();
            Button processBtn = new Button("Process");
            processBtn.setSclass("btn bp btn-sm");
            processBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override
                public void onEvent(Event e) {
                    Executions.sendRedirect(
                        "/outward/checkerQueue/checkerQueue.zul?batchId=" + batchId);
                }
            });
            row.appendChild(processBtn);

            pendingBatchRows.appendChild(row);
        }
    }

    private List<OutwardBatch> getAllSubmittedBatches() {
        // Filter only SUBMITTED batches from all batches
        List<OutwardBatch> all = batchService.getMyBatches(0L);
        List<OutwardBatch> submitted = new java.util.ArrayList<>();
        for (OutwardBatch b : all) {
            if ("SUBMITTED".equals(b.getStatus())) {
                submitted.add(b);
            }
        }
        return submitted;
    }

    private void loadSummary() {
        List<OutwardBatch> all = getAllSubmittedBatches();
        // For now use all batches for full summary counts
        List<OutwardBatch> allBatches = batchService.getMyBatches(0L);

        int pending     = 0;
        int onHold      = 0;
        int readyExport = 0;
        int exported    = 0;

        for (OutwardBatch b : allBatches) {
            String s = b.getStatus();
            if ("SUBMITTED".equals(s))   pending++;
            else if ("REJECTED".equals(s) ||
                     "REFERRED_BACK".equals(s)) onHold++;
            else if ("PASSED".equals(s)) readyExport++;
        }

        sumPending.setValue(String.valueOf(pending));
        sumOnHold.setValue(String.valueOf(onHold));
        sumReadyExport.setValue(String.valueOf(readyExport));
        sumExported.setValue(String.valueOf(exported));
    }
}