package com.iispl.composer;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.service.InwardCheckerService;
import com.iispl.serviceImpl.InwardCheckerServiceImpl;
import com.iispl.util.SessionUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import java.util.List;

/**
 * File    : com/iispl/composer/CheckerInwardDashboardComposer.java
 * Purpose : Checker Inward Dashboard page composer.
 *           - Role guard (CHECKER_INWARD only)
 *           - Navigates to Checker Inward Verification page
 *           - Navigates to Inward Reports page  ← /checker/inward-reports.zul
 *           - Renders pending inward batches grid via InwardCheckerService
 *           - Process button stores selected batch in session and navigates
 *           Topbar handled by TopbarComposer via <include src="/component/topbar.zul" />
 * ZUL     : dashboard/checkerInward/checkerInwardDashboard.zul
 */
public class CheckerInwardDashboardComposer extends SelectorComposer<Component> {

    private final InwardCheckerService inwardCheckerService = new InwardCheckerServiceImpl();

    @Wire private Rows pendingBatchRows;
    @Wire
    private Label pendingBatchTitle;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        if (!"CHECKER_INWARD".equals(dto.getRoleCode())) {
            Executions.sendRedirect(SessionUtil.getDashboardUrlFor(dto.getRoleCode()));
            return;
        }

        renderPendingBatches();
    }

    // ════════════════════════════════════════════
    //  Action card buttons
    // ════════════════════════════════════════════

    @Listen("onClick = #goVerificationBtn")
    public void goToVerification() {
        Executions.sendRedirect("/inward/inwardChecker/inwardCheckerVerification.zul");
    }

    /**
     * Reports card "Go →" button.
     * Navigates to the Checker Inward Reports page.
     * Previously showed a "coming soon" notification — now fully wired.
     */
    @Listen("onClick = #goReportsBtn")
    public void goToReports() {
    	 Executions.sendRedirect("/inward/inwardReports/inward-reports.zul");
    }

    // ════════════════════════════════════════════
    //  Pending batches grid — built from service, no queries here
    // ════════════════════════════════════════════

    private void renderPendingBatches() {
        if (pendingBatchRows == null) return;
        pendingBatchRows.getChildren().clear();

        List<InwardBatch> batches = inwardCheckerService.getPendingBatches();
        pendingBatchTitle.setValue(
            "Inward Batches Pending (" + batches.size() + ")"
        );
        if (batches.isEmpty()) {
            Row emptyRow = new Row();
            Label msg = new Label("No pending batches at this time.");
            msg.setSclass("txt-muted txt-sm");
            emptyRow.appendChild(msg);
            emptyRow.appendChild(new Label(""));
            emptyRow.appendChild(new Label(""));
            pendingBatchRows.appendChild(emptyRow);
            return;
        }

        for (InwardBatch batch : batches) {
            Row row = new Row();

            // Col 1 — Batch ID
            Label batchIdLabel = new Label(batch.getBatchId());
            batchIdLabel.setSclass("mono");
            row.appendChild(batchIdLabel);

            // Col 2 — Cheque count
            row.appendChild(new Label(batch.getTotalCheques() + " cheques"));

            // Col 3 — Process button
            Button processBtn = new Button("view");
            processBtn.setSclass("btn bp btn-sm");
            final String batchId = batch.getBatchId();
            processBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override
                public void onEvent(Event e) {
                    Sessions.getCurrent().setAttribute("selectedInwardBatchId", batchId);
                    Executions.sendRedirect("/inward/inwardChecker/inwardCheckerVerification.zul");
                }
            });
            Div ac = new Div();
            ac.setSclass("ac");
            ac.appendChild(processBtn);
            row.appendChild(ac);

            pendingBatchRows.appendChild(row);
        }
    }

}