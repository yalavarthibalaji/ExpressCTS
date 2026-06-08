package com.iispl.composer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Filedownload;

import com.iispl.dto.LoginDTO;
import com.iispl.service.ReportsService;
import com.iispl.serviceImpl.ReportsServiceImpl;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/ReportsComposer.java
 * Purpose : Drives the Reports screen for CHECKER_OUTWARD role.
 *
 * UI flow:
 *   1. User selects a report type from the listbox.
 *   2. User picks fromDate and toDate.
 *   3. User clicks Generate.
 *   4. Service produces a PDF byte[] → browser downloads via Filedownload.
 */
public class ReportsComposer extends SelectorComposer<Component> {

    private final ReportsService reportsService = new ReportsServiceImpl();

    // ── Topbar ──
    @Wire private Label userAvatar;
    @Wire private Label userName;
    @Wire private Label userRole;

    // ── Report selector ──
    @Wire private Listbox reportTypeList;
    @Wire private Datebox fromDate;
    @Wire private Datebox toDate;
    @Wire private Button  generateBtn;

    // ── Status / preview ──
    @Wire private Div    statusArea;
    @Wire private Label  statusMessage;
    @Wire private Label  selectedReportTitle;
    @Wire private Label  selectedReportDesc;

    // ════════════════════════════════════════════════════
    //  Page Init
    // ════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        if (!"CHECKER_OUTWARD".equals(dto.getRoleCode())) {
            Executions.sendRedirect(SessionUtil.getDashboardUrlFor(dto.getRoleCode()));
            return;
        }

        if (userAvatar != null) userAvatar.setValue(dto.getInitials());
        if (userName   != null) userName.setValue(dto.getFullName());
        if (userRole   != null) userRole.setValue("Checker — Outward");

        // Default date range = last 7 days
        LocalDate today = LocalDate.now();
        toDate.setValue(toLegacyDate(today));
        fromDate.setValue(toLegacyDate(today.minusDays(7)));

        // Select first report by default
        if (reportTypeList.getItemCount() > 0) {
            reportTypeList.setSelectedIndex(0);
            updateReportDescription();
        }
    }

    // ════════════════════════════════════════════════════
    //  Topbar
    // ════════════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }

    // ════════════════════════════════════════════════════
    //  Report Selection
    // ════════════════════════════════════════════════════

    @Listen("onSelect = #reportTypeList")
    public void onReportSelect() {
        updateReportDescription();
    }

    private void updateReportDescription() {
        Listitem sel = reportTypeList.getSelectedItem();
        if (sel == null) {
            selectedReportTitle.setValue("Select a report from the left");
            selectedReportDesc.setValue("");
            return;
        }
        String code = (String) sel.getValue();
        selectedReportTitle.setValue(reportsService.getReportLabel(code));
        selectedReportDesc.setValue(describeReport(code));
    }

    private String describeReport(String code) {
        if (code == null) return "";
        switch (code) {
            case "DAILY_SUMMARY":
                return "Daily count of batches uploaded, total cheques, "
                     + "total amount, exported and rejected batches.";
            case "BATCH_DETAIL":
                return "Detailed list of every outward batch in the date range "
                     + "with maker, checker and status information.";
            case "CHECKER_ACTION":
                return "Per-checker statistics: cheques passed, rejected, referred, "
                     + "and batches handled.";
            case "MAKER_PERFORMANCE":
                return "Per-maker statistics: batches and cheques uploaded, "
                     + "total amount, rejected and referred batches.";
            case "REJECTION":
                return "Every cheque rejected in the date range — by maker (data entry) "
                     + "or by checker (verification) — with reasons.";
            default:
                return "";
        }
    }

    // ════════════════════════════════════════════════════
    //  Generate
    // ════════════════════════════════════════════════════

    @Listen("onClick = #generateBtn")
    public void onGenerate() {
        Listitem sel = reportTypeList.getSelectedItem();
        if (sel == null) {
            showStatus("Please select a report type.", "warn");
            return;
        }
        String reportType = (String) sel.getValue();

        if (fromDate.getValue() == null || toDate.getValue() == null) {
            showStatus("Please select both From and To dates.", "warn");
            return;
        }

        LocalDate from = toLocalDate(fromDate.getValue());
        LocalDate to   = toLocalDate(toDate.getValue());

        if (to.isBefore(from)) {
            showStatus("To Date cannot be before From Date.", "warn");
            return;
        }

        generateBtn.setLabel("Generating...");
        generateBtn.setDisabled(true);

        byte[] pdf;
        try {
            pdf = reportsService.generatePdf(reportType, from, to);
        } catch (Exception e) {
            generateBtn.setLabel("Generate PDF");
            generateBtn.setDisabled(false);
            showStatus("Report generation failed: " + e.getMessage(), "err");
            return;
        }

        generateBtn.setLabel("Generate PDF");
        generateBtn.setDisabled(false);

        if (pdf == null || pdf.length == 0) {
            showStatus("No data found OR template error. "
                     + "Check server log for details.", "warn");
            return;
        }

        String label    = reportsService.getReportLabel(reportType);
        String filename = label.replaceAll("\\s+", "_") + "_" + from + "_to_" + to + ".pdf";

        try {
            AMedia media = new AMedia(filename, "pdf", "application/pdf", pdf);
            Filedownload.save(media);
            showStatus("✓ Report generated and downloaded ("
                     + (pdf.length / 1024) + " KB).", "ok");
        } catch (Exception e) {
            showStatus("Download failed: " + e.getMessage(), "err");
        }
    }

    // ════════════════════════════════════════════════════
    //  Status helpers
    // ════════════════════════════════════════════════════

    private void showStatus(String msg, String tone) {
        statusMessage.setValue(msg);
        switch (tone) {
            case "ok":   statusArea.setSclass("status-pane note suc"); break;
            case "warn": statusArea.setSclass("status-pane note warn"); break;
            case "err":  statusArea.setSclass("status-pane note err");  break;
            default:     statusArea.setSclass("status-pane note info");
        }
        statusArea.setVisible(true);
        Clients.scrollIntoView(statusArea);
    }

    // ════════════════════════════════════════════════════
    //  Date converters (java.util.Date  ↔  java.time.LocalDate)
    // ════════════════════════════════════════════════════

    private Date toLegacyDate(LocalDate ld) {
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}