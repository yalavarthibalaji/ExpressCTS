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
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import java.util.List;

/**
 * File    : com/iispl/composer/CheckerInwardDashboardComposer.java
 *
 * Validations added:
 *   1. Session / login guard          — redirect to login if session expired
 *   2. Role guard                     — CHECKER_INWARD only
 *   3. Null-safe stat card rendering  — never throws NPE on label wire failure
 *   4. Empty batch list handling      — friendly message, no blank grid
 *   5. Stale batch guard on View      — re-checks batch still MakerVerified before navigating
 *   6. Session attribute confirmation — warns if batch ID could not be stored
 *   7. Service error isolation        — stat card load failure does not crash the page
 */
public class CheckerInwardDashboardComposer extends SelectorComposer<Component> {

    private final InwardCheckerService inwardCheckerService = new InwardCheckerServiceImpl();

    @Wire private Rows  pendingBatchRows;
    @Wire private Label pendingBatchTitle;
    @Wire private Label lblPendingCount;
    @Wire private Label lblClearedCount;
    @Wire private Label lblTotalCount;

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        // VALIDATION 1 — Session / login guard
        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) {
            Executions.sendRedirect("/login/login.zul");
            return;
        }

        // VALIDATION 2 — Role guard
        if (!"CHECKER_INWARD".equals(dto.getRoleCode())) {
            Executions.sendRedirect(SessionUtil.getDashboardUrlFor(dto.getRoleCode()));
            return;
        }

        populateStatCards();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Navigation buttons
    // ─────────────────────────────────────────────────────────────────────────

    @Listen("onClick = #goVerificationBtn")
    public void goToVerification() {
        Executions.sendRedirect("/inward/inwardChecker/inwardCheckerVerification.zul");
    }

    @Listen("onClick = #goReportsBtn")
    public void goToReports() {
        Executions.sendRedirect("/inward/inwardReports/inward-reports.zul");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Stat cards
    // ─────────────────────────────────────────────────────────────────────────

    private void populateStatCards() {

        // VALIDATION 7 — Isolate service errors so the page still renders
        List<InwardBatch> pending;
        int clearedCount = 0;
        int totalCount   = 0;

        try {
            pending      = inwardCheckerService.getPendingBatches();
            clearedCount = inwardCheckerService.getClearedBatchCount();
            totalCount   = inwardCheckerService.getTotalBatchCount();
        } catch (Exception e) {
            pending = new java.util.ArrayList<>();
            Clients.showNotification(
                "Could not load dashboard data. Please refresh the page.",
                "error", null, "top_center", 5000
            );
        }

        // VALIDATION 3 — Null-safe label updates
        if (lblPendingCount != null) lblPendingCount.setValue(String.valueOf(pending.size()));
        if (lblClearedCount != null) lblClearedCount.setValue(String.valueOf(clearedCount));
        if (lblTotalCount   != null) lblTotalCount.setValue(String.valueOf(totalCount));

        renderPendingBatches(pending);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Pending batches grid
    // ─────────────────────────────────────────────────────────────────────────

    private void renderPendingBatches(List<InwardBatch> batches) {
        if (pendingBatchRows == null) return;
        pendingBatchRows.getChildren().clear();

        // VALIDATION 3 — Null-safe title update
        if (pendingBatchTitle != null) {
            pendingBatchTitle.setValue("Inward Batches Pending (" + batches.size() + ")");
        }

        // VALIDATION 4 — Empty list: show friendly message instead of blank grid
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

            // VALIDATION 3 — Skip any null batch entity returned by the service
            if (batch == null || batch.getBatchId() == null) continue;

            Row row = new Row();

            // Col 1 — Batch ID
            Label batchIdLabel = new Label(batch.getBatchId());
            batchIdLabel.setSclass("mono");
            row.appendChild(batchIdLabel);

            // Col 2 — Cheque count
            row.appendChild(new Label(batch.getTotalCheques() + " cheques"));

            // Col 3 — View button with stale-batch guard
            Button processBtn = new Button("View");
            processBtn.setSclass("btn bp btn-sm");
            final String batchId = batch.getBatchId();

            processBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override
                public void onEvent(Event e) {

                    // VALIDATION 5 — Re-check batch is still MakerVerified before navigating
                    // (another checker may have already picked it up)
                    InwardBatch fresh = inwardCheckerService.getBatchById(batchId);
                    if (fresh == null) {
                        Messagebox.show(
                            "Batch " + batchId + " no longer exists.",
                            "Not Found", Messagebox.OK, Messagebox.EXCLAMATION
                        );
                        return;
                    }
                    if (!"MakerVerified".equals(fresh.getStatus())) {
                        Messagebox.show(
                            "Batch " + batchId + " has already been processed by another checker.\n"
                            + "Current status: " + fresh.getStatus() + ".\n\n"
                            + "The list will now refresh.",
                            "Batch Unavailable", Messagebox.OK, Messagebox.EXCLAMATION,
                            ev -> populateStatCards()   // auto-refresh on dismiss
                        );
                        return;
                    }

                    // VALIDATION 6 — Confirm session attribute was stored before redirecting
                    Sessions.getCurrent().setAttribute("selectedInwardBatchId", batchId);
                    Object stored = Sessions.getCurrent().getAttribute("selectedInwardBatchId");
                    if (stored == null) {
                        Clients.showNotification(
                            "Session error. Please try again.",
                            "error", null, "top_center", 3000
                        );
                        return;
                    }

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