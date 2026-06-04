package com.iispl.composer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.dto.InwardReportDTO;
import com.iispl.dto.LoginDTO;
import com.iispl.service.CheckerInwardReportsService;
import com.iispl.serviceImpl.CheckerInwardReportsServiceImpl;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/CheckerInwardReportsComposer.java
 * Purpose : MVC Composer for /inward/inwardReports/inward-reports.zul
 *
 *   CHANGES vs previous version:
 *     - Grid uses HTML divs instead of ZK Grid (flat CSS flexbox layout)
 *     - Renders 6 columns: Batch ID, Count, Accepted, Returned, Presenting Banks, Status
 *     - Removed: MICR Errors, IQA Fails, Passed, Rejected, Referred, Action columns
 *     - New wired buttons: btnDownloadCibf (was btnDownloadBrf),
 *       btnDownloadCibfImages, btnCibfImages
 *     - Inline grid Batch ID filter: txtGridBatchFilter (client-side DOM filter)
 *     - No pager controls (empty state shows "Generate report to view data.")
 *     - Pagination still supported internally; UI simplified per target screenshot
 */
public class CheckerInwardReportsComposer extends SelectorComposer<Component> {

    private static final Logger log = LoggerFactory.getLogger(CheckerInwardReportsComposer.class);

    private static final int PAGE_SIZE = 50; // larger page, no pager shown in target UI

    private final CheckerInwardReportsService reportsService =
            new CheckerInwardReportsServiceImpl();

    private List<InwardReportDTO> currentRows = new ArrayList<>();

    // ── Wired filter controls ─────────────────────────────────────────────────
    @Wire private Textbox  txtBatchSearch;
    @Wire private Datebox  dtFrom;
    @Wire private Datebox  dtTo;
    @Wire private Combobox cmbStatus;

    // ── Wired grid body (div rendered by composer) ────────────────────────────
    @Wire private Div rptGridBody;

    // ── Wired buttons ─────────────────────────────────────────────────────────
    @Wire private Button btnGenerate;
    @Wire private Button btnDownloadAck;
    @Wire private Button btnDownloadRrf;   // was btnDownloadBrf — now CIBF.xml
    @Wire private Button btnRrfImages;          // inline CIBF Images button in filter bar
    @Wire private Button btnDownloadCibfImages;  // above-filter CIBF Images button

    // ── Inline grid Batch ID filter ───────────────────────────────────────────
    @Wire private Textbox txtGridBatchFilter;

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO user = SessionUtil.requireLogin();
        if (user == null) return;

        if (!"CHECKER_INWARD".equals(user.getRoleCode())) {
            Executions.sendRedirect(SessionUtil.getDashboardUrlFor(user.getRoleCode()));
            return;
        }

        populateStatusCombo();
        setExportButtonsEnabled(false);
        showEmptyState();

        log.info("CheckerInwardReportsComposer initialised for user '{}'",
                 user.getUserLoginId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Button listeners
    // ─────────────────────────────────────────────────────────────────────────

    @Listen("onClick = #btnGenerate")
    public void onGenerate() {
        loadPage();
    }

    @Listen("onClick = #btnDownloadAck")
    public void onDownloadAck() {
        if (currentRows.isEmpty()) {
            Clients.showNotification("Generate a report first.", "warning", null, "top_center", 2500);
            return;
        }
        downloadXml("ACK");
    }

    /** Download CIBF.xml (replaces BRF.xml in target UI). */
    @Listen("onClick = #btnDownloadRrf")
    public void onDownloadRrf() {
        if (currentRows.isEmpty()) {
            Clients.showNotification("Generate a report first.", "warning", null, "top_center", 2500);
            return;
        }
        downloadXml("RRF");
    }

    /** CIBF Images button in filter bar. */
    @Listen("onClick = #btnRrfImages")
    public void onRrfImages() {
        Clients.showNotification("RRF image download is not yet implemented.", "info", null, "top_center", 3000);
    }

    /** CIBF Images button above filter. */
    @Listen("onClick = #btnDownloadRrfImages")
    public void onDownloadRrfImages() {
        Clients.showNotification("RRF image download is not yet implemented.", "info", null, "top_center", 3000);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core load
    // ─────────────────────────────────────────────────────────────────────────

    private void loadPage() {
        String batchSearch = (txtBatchSearch != null) ? txtBatchSearch.getValue() : null;
        Date   fromDate    = (dtFrom != null)         ? dtFrom.getValue()         : null;
        Date   toDate      = (dtTo   != null)         ? dtTo.getValue()           : null;
        String status      = getSelectedStatus();

        try {
            if (fromDate != null && toDate != null && fromDate.after(toDate)) {
                Messagebox.show("From Date cannot be after To Date.",
                                "Validation Error", Messagebox.OK, Messagebox.ERROR);
                return;
            }

            currentRows = reportsService.getReports(
                    batchSearch, fromDate, toDate, status, 1, PAGE_SIZE);

            renderRows(currentRows);
            setExportButtonsEnabled(!currentRows.isEmpty());

            log.info("loadPage — rows={}", currentRows.size());

        } catch (IllegalArgumentException e) {
            Messagebox.show(e.getMessage(), "Validation Error", Messagebox.OK, Messagebox.ERROR);
        } catch (Exception e) {
            log.error("loadPage error: {}", e.getMessage(), e);
            Messagebox.show("An error occurred while loading reports. Please try again.",
                            "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Grid rendering  (flat CSS div rows — matches target screenshot)
    // ─────────────────────────────────────────────────────────────────────────

    private void renderRows(List<InwardReportDTO> rows) {
        if (rptGridBody == null) return;
        rptGridBody.getChildren().clear();

        if (rows == null || rows.isEmpty()) {
            showEmptyState();
            return;
        }

        for (InwardReportDTO dto : rows) {

            // Row div  ─────────────────────────────────────────────────────
            Div row = new Div();
            row.setSclass("rpt-row");

            // Col 1 — Batch ID
            Div cBatchId = cell("rpt-cell rpt-cell-batchid rpt-col-batchid",
                                nullSafe(dto.getBatchId()));
            row.appendChild(cBatchId);

            // Col 2 — Count
            Div cCount = cell("rpt-cell rpt-col-count",
                              String.valueOf(dto.getTotalCheques()));
            row.appendChild(cCount);

            // Col 3 — Accepted
            Div cAccepted = cell("rpt-cell rpt-col-accepted",
                                 String.valueOf(dto.getAcceptedCount()));
            row.appendChild(cAccepted);

            // Col 4 — Returned
            Div cReturned = cell("rpt-cell rpt-col-returned",
                                 String.valueOf(dto.getReturnedCount()));
            row.appendChild(cReturned);

            // Col 5 — Presenting Banks
            Div cBanks = cell("rpt-cell rpt-col-banks",
                              nullSafe(dto.getPresentingBanks()));
            row.appendChild(cBanks);

            // Col 6 — Status badge
            Div cStatus = new Div();
            cStatus.setSclass("rpt-cell rpt-col-status");
            Label lStatus = new Label(nullSafe(dto.getStatus()));
            lStatus.setSclass(resolveStatusBadgeClass(dto.getStatus()));
            cStatus.appendChild(lStatus);
            row.appendChild(cStatus);

            rptGridBody.appendChild(row);
        }
    }

    /** Show the "Generate report to view data." centred empty state. */
    private void showEmptyState() {
        if (rptGridBody == null) return;
        rptGridBody.getChildren().clear();
        Div empty = new Div();
        empty.setSclass("rpt-empty-state");
        Label msg = new Label("Generate report to view data.");
        empty.appendChild(msg);
        rptGridBody.appendChild(empty);
    }

    /** Helper: create a cell div with a text label. */
    private Div cell(String sclass, String text) {
        Div d = new Div();
        d.setSclass(sclass);
        d.appendChild(new Label(text));
        return d;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Export helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bulk XML download.
     * type = "CXFFFFFF"  → CXFFFFF.xml   (cheque transaction data)
     * type = "CIBF" → CIBF.xml  (was BRF — NPCI bank response file)
     */
    private void downloadXml(String type) {
        try {
            StringBuilder combined = new StringBuilder();
            combined.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            combined.append("<InwardReportExport type=\"").append(type).append("\">\n");

            for (InwardReportDTO dto : currentRows) {
                String singleXml;
                if ("ACK".equals(type)) {
                    singleXml = reportsService.buildAckXml(dto);
                } else {
                    // CIBF uses BRF builder — same structure, renamed file
                    singleXml = reportsService.buildRrfXml(dto);
                }
                singleXml = singleXml.replaceFirst("<\\?xml[^?]*\\?>\\s*", "");
                combined.append(singleXml).append("\n");
            }
            combined.append("</InwardReportExport>\n");

            String filename = "inward-report-export." + type.toLowerCase() + ".xml";
            pushDownload(combined.toString(), filename);

        } catch (Exception e) {
            log.error("XML export error ({}): {}", type, e.getMessage(), e);
            Messagebox.show("Export failed: " + e.getMessage(),
                            "Export Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

    private void pushDownload(String content, String filename) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Filedownload.save(new ByteArrayInputStream(bytes), "application/xml", filename);
        log.info("File download initiated: {}", filename);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void populateStatusCombo() {
        if (cmbStatus == null) return;
        cmbStatus.getChildren().clear();
        // "All Status" label shown in the combobox to match target screenshot
        String[][] items = {
            { "All Status",       "ALL"             },
            { "PENDING_CHECKER",  "PENDING_CHECKER" },
            { "ACCEPTED",         "ACCEPTED"        },
            { "RETURNED",         "RETURNED"        },
            { "REJECTED",         "REJECTED"        }
        };
        for (String[] item : items) {
            Comboitem ci = new Comboitem(item[0]);
            ci.setValue(item[1]);
            cmbStatus.appendChild(ci);
        }
        cmbStatus.setSelectedIndex(0);
    }

    private String getSelectedStatus() {
        if (cmbStatus == null) return "ALL";
        Comboitem sel = cmbStatus.getSelectedItem();
        if (sel == null) return "ALL";
        return sel.getValue() != null ? sel.getValue().toString() : "ALL";
    }

    private void setExportButtonsEnabled(boolean enabled) {
        if (btnDownloadAck  != null) btnDownloadAck.setDisabled(!enabled);
        if (btnDownloadRrf != null) btnDownloadRrf.setDisabled(!enabled);
        if (btnRrfImages   != null) btnRrfImages.setDisabled(!enabled);
    }

    private String resolveStatusBadgeClass(String status) {
        if (status == null) return "badge b-grey";
        switch (status.toUpperCase()) {
            case "PENDING_CHECKER": return "badge b-pend";
            case "ACCEPTED":        return "badge b-pass";
            case "RETURNED":        return "badge b-info";
            case "REJECTED":        return "badge b-fail";
            default:                return "badge b-grey";
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "—";
    }
}