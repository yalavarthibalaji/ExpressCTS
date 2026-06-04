package com.iispl.composer;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.service.BatchUploadService;
import com.iispl.serviceImpl.BatchUploadServiceImpl;
import com.iispl.util.SessionUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import java.util.List;

public class DashboardComposer extends SelectorComposer<Component> {

    @Wire private Label  userAvatar;
    @Wire private Label  userName;
    @Wire private Label  userRole;

    @Wire("#gotoBatchUpload") private Button gotoBatchUpload;
    @Wire("#gotoMicrRepair")  private Button gotoMicrRepair;
    @Wire("#gotoAcctAmount")  private Button gotoAcctAmount;

    @Wire private Rows  recentBatchRows;
    @Wire private Label sumTotal;
    @Wire private Label sumMicr;
    @Wire private Label sumEntry;
    @Wire private Label sumSubmitted;

    private final BatchUploadService batchService = new BatchUploadServiceImpl();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        if (userAvatar != null) userAvatar.setValue(dto.getInitials());
        if (userName   != null) userName.setValue(dto.getFullName());
        if (userRole   != null) userRole.setValue(formatRole(dto.getRoleCode()));

        if (recentBatchRows != null) {
            loadRecentBatches(dto.getUserId());
        }
        if (sumTotal != null) {
            loadSummary(dto.getUserId());
        }
    }

    // ── Navigation ──────────────────────────────────────

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }

    @Listen("onClick = #userMgmtBtn")
    public void goToUserManagement() {
        Executions.sendRedirect("/admin/userManagement/userManagement.zul");
    }

    @Listen("onClick = #backToDashboardBtn")
    public void backToDashboard() {
        Executions.sendRedirect("/admin/adminDashboard.zul");
    }

    @Listen("onClick = #gotoBatchUpload")
    public void gotoBatchUploadNav() {
        Executions.sendRedirect("/outward/batchUpload/batchUpload.zul");
    }

    @Listen("onClick = #gotoMicrRepair")
    public void gotoMicrRepairNav() {
        Executions.sendRedirect("/outward/micrRepair/micrRepair.zul");
    }

    @Listen("onClick = #gotoAcctAmount")
    public void gotoAcctAmountNav() {
        Executions.sendRedirect("/outward/acctAmount/acctAmount.zul");
    }

    // ── Recent Batches ───────────────────────────────────

    private void loadRecentBatches(Long makerId) {
        List<OutwardBatch> batches = batchService.getMyBatches(makerId);
        recentBatchRows.getChildren().clear();

        int limit = Math.min(batches.size(), 5);
        for (int i = 0; i < limit; i++) {
            OutwardBatch b = batches.get(i);
            Row row = new Row();

            Label batchIdLbl = new Label(b.getBatchId());
            batchIdLbl.setSclass("mono");
            row.appendChild(batchIdLbl);

            row.appendChild(new Label(String.valueOf(b.getChequeCount())));

            Label statusBadge = new Label(formatStatus(b.getStatus()));
            statusBadge.setSclass(badgeClass(b.getStatus()));
            row.appendChild(statusBadge);

            recentBatchRows.appendChild(row);
        }

        if (batches.isEmpty()) {
            Row empty = new Row();
            Label lbl = new Label("No batches found.");
            lbl.setStyle("color:var(--tm); font-size:12px;");
            empty.appendChild(lbl);
            recentBatchRows.appendChild(empty);
        }
    }

    // ── Summary ──────────────────────────────────────────

    private void loadSummary(Long makerId) {
        List<OutwardBatch> batches = batchService.getMyBatches(makerId);

        int total     = batches.size();
        int micr      = 0;
        int entry     = 0;
        int submitted = 0;

        for (OutwardBatch b : batches) {
            String s = b.getStatus();
            if ("NEEDS_REPAIR".equals(s)) {
                micr++;
            } else if ("ENTRY_DONE".equals(s) || "REFERRED_BACK".equals(s)) {
                entry++;
            } else if ("SUBMITTED".equals(s)) {
                submitted++;
            }
        }

        sumTotal.setValue(String.valueOf(total));
        sumMicr.setValue(String.valueOf(micr));
        sumEntry.setValue(String.valueOf(entry));
        sumSubmitted.setValue(String.valueOf(submitted));
    }

    // ── Helpers ──────────────────────────────────────────

    private String formatStatus(String status) {
        if (status == null) return "—";
        switch (status) {
            case "NEEDS_REPAIR":  return "Needs Repair";
            case "ENTRY_DONE":    return "Entry Done";
            case "SUBMITTED":     return "Submitted";
            case "PASSED":        return "Passed";
            case "REJECTED":      return "Rejected";
            case "REFERRED_BACK": return "Referred Back";
            default:              return status;
        }
    }

    private String badgeClass(String status) {
        if (status == null) return "badge b-grey";
        switch (status) {
            case "NEEDS_REPAIR":  return "badge b-pend";
            case "ENTRY_DONE":    return "badge b-info";
            case "SUBMITTED":     return "badge b-info";
            case "PASSED":        return "badge b-pass";
            case "REJECTED":      return "badge b-fail";
            case "REFERRED_BACK": return "badge b-grey";
            default:              return "badge b-grey";
        }
    }

    private String formatRole(String code) {
        if (code == null) return "";
        switch (code) {
            case "ADMIN":           return "Administrator";
            case "MAKER_OUTWARD":   return "Maker — Outward";
            case "CHECKER_OUTWARD": return "Checker — Outward";
            case "MAKER_INWARD":    return "Maker — Inward";
            case "CHECKER_INWARD":  return "Checker — Inward";
            default:                return code;
        }
    }
}