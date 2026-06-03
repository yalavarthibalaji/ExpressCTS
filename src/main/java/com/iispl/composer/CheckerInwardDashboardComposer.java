package com.iispl.composer;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.util.HibernateUtil;
import com.iispl.util.SessionUtil;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
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
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import java.util.ArrayList;
import java.util.List;

/**
 * File    : com/iispl/composer/CheckerInwardDashboardComposer.java
 * Purpose : Checker Inward Dashboard composer.
 *           - Wires topbar user info (avatar, name, role)
 *           - Handles logout
 *           - Navigates to Checker Inward Verification page
 *           - Navigates to Reports page
 *           - Loads pending inward batches from DB and renders rows dynamically
 *           - Process button stores selected batch in session and navigates to verification
 * ZUL     : dashboard/checkerInward/checkerInwardDashboard.zul
 */
public class CheckerInwardDashboardComposer extends SelectorComposer<Component> {

    // ── Topbar wires (inlined topbar, same as all other dashboard pages) ──
    @Wire private Label userAvatar;
    @Wire private Label userName;
    @Wire private Label userRole;

    // ── Pending batches grid ──
    @Wire private Rows pendingBatchRows;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        // Require login; redirect to login if session missing
        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        // Role guard — only CHECKER_INWARD may access this page
        if (!"CHECKER_INWARD".equals(dto.getRoleCode())) {
            Executions.sendRedirect(SessionUtil.getDashboardUrlFor(dto.getRoleCode()));
            return;
        }

        // Wire topbar labels
        if (userAvatar != null) userAvatar.setValue(dto.getInitials());
        if (userName   != null) userName.setValue(dto.getFullName());
        if (userRole   != null) userRole.setValue("CHECKER_INWARD");

        // Populate pending batches grid
        renderPendingBatches();
    }

    // ════════════════════════════════════════════
    //  Topbar
    // ════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }

    // ════════════════════════════════════════════
    //  Action card buttons
    // ════════════════════════════════════════════

    @Listen("onClick = #goVerificationBtn")
    public void goToVerification() {
        Executions.sendRedirect("/inward/inwardChecker/inwardChecker.zul");
    }

    @Listen("onClick = #goReportsBtn")
    public void goToReports() {
        Clients.showNotification("Reports module coming soon.", "info", null, "top_center", 2500);
    }

    // ════════════════════════════════════════════
    //  Pending batches — dynamic grid rows
    // ════════════════════════════════════════════

    private void renderPendingBatches() {
        if (pendingBatchRows == null) return;
        pendingBatchRows.getChildren().clear();

        List<InwardBatch> batches = fetchPendingBatches();

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

            // Col 1 — Batch ID in monospace
            Label batchIdLabel = new Label(batch.getBatchId());
            batchIdLabel.setSclass("mono");
            row.appendChild(batchIdLabel);

            // Col 2 — Cheque count
            row.appendChild(new Label(batch.getTotalCheques() + " cheques"));

            // Col 3 — Process button wrapped in .ac div
            Button processBtn = new Button("Process");
            processBtn.setSclass("btn bp btn-sm");
            final String batchId = batch.getBatchId();
            processBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override
                public void onEvent(Event e) {
                    org.zkoss.zk.ui.Sessions.getCurrent()
                        .setAttribute("selectedInwardBatchId", batchId);
                    Executions.sendRedirect("/inward/inwardChecker/inwardChecker.zul");
                }
            });
            Div ac = new Div();
            ac.setSclass("ac");
            ac.appendChild(processBtn);
            row.appendChild(ac);

            pendingBatchRows.appendChild(row);
        }
    }

    private List<InwardBatch> fetchPendingBatches() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            NativeQuery<InwardBatch> q = session.createNativeQuery(
                "SELECT * FROM inward_batch " +
                "WHERE status IN ('RECEIVED', 'PENDING_CHECKER') " +
                "ORDER BY created_at ASC",
                InwardBatch.class
            );
            return q.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }
}