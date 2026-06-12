// File: java/com/iispl/composer/CheckerOutwardDashboardComposer.java

package com.iispl.composer;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.service.CheckerService;
import com.iispl.serviceImpl.CheckerServiceImpl;
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
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import java.util.List;
import java.util.Map;

public class CheckerOutwardDashboardComposer extends SelectorComposer<Component> {

    // ── Topbar labels ──
	@Wire private Label userAvatar;
    @Wire private Label userName;
    @Wire private Label userRole;

    // ── Quick action buttons (wired from included dashCard.zul components) ──
    @Wire("#gotoVerificationQueue") private Button gotoVerificationQueue;
    @Wire("#gotoDemExport")         private Button gotoDemExport;
    @Wire("#gotoReports")           private Button gotoReports;

    // ── Pending Verification table ──
    @Wire private Rows pendingBatchRows;

    // ── Summary labels ──
    @Wire private Label sumPending;
    @Wire private Label sumOnHold;
    @Wire private Label sumReadyExport;
    @Wire private Label sumExported;

    // ── Service ──
    private final CheckerService checkerService = new CheckerServiceImpl();

    // ════════════════════════════════════════════════════
    //  Page Init
    // ════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        // Step 1: Session check
        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        // Step 2: Role guard — only CHECKER_OUTWARD may access this dashboard
        if (!("CHECKER_OUTWARD".equals(dto.getRoleCode()))) {
            Executions.sendRedirect(SessionUtil.getDashboardUrlFor(dto.getRoleCode()));
            return;
        }

        if (userAvatar != null) userAvatar.setValue(dto.getInitials());
        if (userName   != null) userName.setValue(dto.getFullName());
        if (userRole   != null) userRole.setValue("Checker — Outward");

        // Step 4: Wire included dashCard buttons via component tree traversal
        //         (included ZUL components live in a different scope,
        //          so @Listen cannot reach them — we use addEventListener instead)
        wireFellow(comp, "gotoVerificationQueue",
                () -> Executions.sendRedirect("/outward/checkerQueue/checkerQueue.zul"));

        wireFellow(comp, "gotoDemExport",
                () -> Executions.sendRedirect("/outward/demExport/demExport.zul"));

        wireFellow(comp, "gotoReports",
                () -> Executions.sendRedirect("/reports/checkerReports.zul"));

        // Step 5: Load dashboard data
        if (pendingBatchRows != null) loadPendingBatches();
        if (sumPending       != null) loadSummary();
    }

    // ════════════════════════════════════════════════════
    //  Logout
    // ════════════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }

    // ════════════════════════════════════════════════════
    //  Pending Verification Table
    // ════════════════════════════════════════════════════

    /**
     * Loads up to 5 batches with status = SUBMITTED into the
     * "Pending Verification" preview table on the dashboard.
     *
     * Uses CheckerService.getCheckerQueueBatches() which returns
     * SUBMITTED + CHECKER_IN_PROGRESS + CHECKER_HOLD batches.
     * We filter to SUBMITTED only for the pending preview.
     */
    private void loadPendingBatches() {
        List<OutwardBatch> queueBatches = checkerService.getCheckerQueueBatches();

        pendingBatchRows.getChildren().clear();

        // Filter: only SUBMITTED batches appear in the Pending preview
        List<OutwardBatch> pending = new java.util.ArrayList<>();
        for (OutwardBatch b : queueBatches) {
            if ("SUBMITTED".equals(b.getStatus())) {
                pending.add(b);
            }
        }

        if (pending.isEmpty()) {
            Row emptyRow = new Row();
            Label lbl = new Label("No pending batches.");
            lbl.setStyle("color:var(--tm); font-size:12px;");
            emptyRow.appendChild(lbl);
            pendingBatchRows.appendChild(emptyRow);
            return;
        }

        // Show maximum 5 rows in the dashboard preview
        int limit = Math.min(pending.size(), 5);
        for (int i = 0; i < limit; i++) {
            OutwardBatch batch = pending.get(i);

            Row row = new Row();

            // Column 1: Batch ID
            Label batchIdLabel = new Label(batch.getBatchId());
            batchIdLabel.setSclass("mono");
            row.appendChild(batchIdLabel);

            // Column 2: Cheque count
            row.appendChild(new Label(batch.getChequeCount() + " cheques"));

            // Column 3: Process button
            Button processBtn = new Button("Process");
            processBtn.setSclass("btn bp btn-sm");

            final Long batchDbId = batch.getId();
            processBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override
                public void onEvent(Event event) {
                    Executions.sendRedirect(
                        "/outward/checkerQueue/checkerQueue.zul?batchId=" + batchDbId);
                }
            });
            row.appendChild(processBtn);

            pendingBatchRows.appendChild(row);
        }
    }

    /**
     * Loads the four summary counts using CheckerService.getDashboardCounts()
     * plus a separate count for fully-exported batches.
     *
     * Map keys returned by the service:
     *   "pending"     → SUBMITTED batches       → Pending Queue label
     *   "inProgress"  → CHECKER_IN_PROGRESS     → included in On Hold display
     *   "hold"        → CHECKER_HOLD batches    → On Hold label
     *   "approved"    → CHECKER_APPROVED        → Ready to Export label
     *   "exported"    → EXPORTED                → DEM Exported label (real count)
     */
    private void loadSummary() {
        Map<String, Integer> counts = checkerService.getDashboardCounts();

        int pending    = counts.getOrDefault("pending",    0);
        int inProgress = counts.getOrDefault("inProgress", 0);
        int hold       = counts.getOrDefault("hold",       0);
        int approved   = counts.getOrDefault("approved",   0);
        int exported   = counts.getOrDefault("exported",   0);

        // "On Hold" shows both actively-in-progress and referred-back batches
        int onHold = inProgress + hold;

        sumPending.setValue(String.valueOf(pending));
        sumOnHold.setValue(String.valueOf(onHold));
        sumReadyExport.setValue(String.valueOf(approved));
        sumExported.setValue(String.valueOf(exported));
    }

    // ════════════════════════════════════════════════════
    //  Private Helper — Wire included component buttons
    // ════════════════════════════════════════════════════

    /**
     * Finds a component by ID anywhere in the page component tree
     * and attaches a click listener to it.
     *
     * This is needed because ZK's @Wire and @Listen annotations
     * cannot cross the scope boundary created by <include> tags.
     * DashCard buttons (gotoVerificationQueue, gotoDemExport, gotoReports)
     * live inside included ZUL files, so they must be wired this way.
     *
     * @param root   The root component to start searching from
     * @param id     The component ID to search for
     * @param action The action to execute on click
     */
    private void wireFellow(Component root, String id, Runnable action) {
        try {
            Component target = findById(root, id);
            if (target != null) {
                target.addEventListener(Events.ON_CLICK, e -> action.run());
            }
        } catch (Exception ignored) {
            // If wiring fails, the button simply has no action — page still loads
        }
    }

    /**
     * Recursively searches the component tree for a component with the given ID.
     *
     * @param root The component to start from
     * @param id   The ID to search for
     * @return The matching component, or null if not found
     */
    private Component findById(Component root, String id) {
        if (id.equals(root.getId())) return root;
        for (Component child : root.getChildren()) {
            Component found = findById(child, id);
            if (found != null) return found;
        }
        return null;
    }
}