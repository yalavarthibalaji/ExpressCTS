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
 *   8. Stale batch guard on Process   — re-checks batch still MakerVerified before navigating
 *   9. Session attribute confirmation — for Process button (key: selectedBatchId, as
 *      expected by ProcessBatchComposer), warns if it could not be stored
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
        try {
            // Single DB call — fetch all batches visible to checker
            List<InwardBatch> all = inwardCheckerService.getAllBatchesForChecker();

            long pendingCount = all.stream()
                .filter(b -> "MakerVerified".equals(b.getStatus()))
                .count();

            long clearedCount = all.stream()
                .filter(b -> "Verified".equals(b.getStatus())
                          || "CBS_Processed".equals(b.getStatus()))
                .count();

            // Total = only batches checker is involved with
            long totalCount = all.size();

            if (lblPendingCount != null) lblPendingCount.setValue(String.valueOf(pendingCount));
            if (lblClearedCount != null) lblClearedCount.setValue(String.valueOf(clearedCount));
            if (lblTotalCount   != null) lblTotalCount.setValue(String.valueOf(totalCount));

            // Render only pending batches in the grid
            List<InwardBatch> pending = all.stream()
                .filter(b -> "MakerVerified".equals(b.getStatus()))
                .collect(java.util.stream.Collectors.toList());

            renderPendingBatches(pending);

        } catch (Exception e) {
            Clients.showNotification(
                "Could not load dashboard data. Please refresh the page.",
                "error", null, "top_center", 5000
            );
        }
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

            // Col 3b — Process button: sends the batch straight to processBatch.zul
            Button processBatchBtn = new Button("Process");
            processBatchBtn.setSclass("btn bp btn-sm");

            processBatchBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override
                public void onEvent(Event e) {

                    // VALIDATION 8 — Re-check batch is still MakerVerified before navigating
                    // (same stale-batch guard used by the View button)
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

                    // VALIDATION 9 — Confirm session attribute was stored before redirecting.
                    // NOTE: ProcessBatchComposer reads "selectedBatchId" (not "selectedInwardBatchId"),
                    // so that is the key used here.
                    Sessions.getCurrent().setAttribute("selectedBatchId", batchId);
                    Object stored = Sessions.getCurrent().getAttribute("selectedBatchId");
                    if (stored == null) {
                        Clients.showNotification(
                            "Session error. Please try again.",
                            "error", null, "top_center", 3000
                        );
                        return;
                    }

                    Executions.sendRedirect("/inward/inwardChecker/processBatch.zul");
                }
            });

            Div ac = new Div();
            ac.setSclass("ac");
            ac.appendChild(processBtn);
            ac.appendChild(processBatchBtn);
            row.appendChild(ac);

            pendingBatchRows.appendChild(row);
        }
    }
}