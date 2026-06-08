package com.iispl.composer.outward;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import com.iispl.dao.OutwardBatchDao;
import com.iispl.dao.OutwardChequeDao;
import com.iispl.daoImpl.OutwardBatchDaoImpl;
import com.iispl.daoImpl.OutwardChequeDaoImpl;
import com.iispl.dto.LoginDTO;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/outward/ViewBatchesComposer.java
 * Purpose : Read-only monitoring screen for outward batches.
 *
 * Two sections in one page:
 *   SECTION 1 — Batch List  : filter bar + batch table
 *   SECTION 2 — Cheque View : split (cheque list | cheque detail panel)
 *
 * Role behaviour:
 *   MAKER_OUTWARD → shows only their own batches
 *   ADMIN         → shows ALL batches in the system
 */
public class ViewBatchesComposer extends SelectorComposer<Component> {

    private final OutwardBatchDao  batchDao  = new OutwardBatchDaoImpl();
    private final OutwardChequeDao chequeDao = new OutwardChequeDaoImpl();
    private final DecimalFormat    moneyFmt  = new DecimalFormat("#,##0.00");

    // ── Topbar ──
    @Wire private Label  userAvatar;
    @Wire private Label  userName;
    @Wire private Label  userRole;

    // ── Section 1: Batch List ──
    @Wire private Div    batchSection;
    @Wire private Textbox searchBox;
    @Wire private Listbox statusFilter;
    @Wire private Textbox fromDateBox;
    @Wire private Textbox toDateBox;
    @Wire private Rows   batchRows;
    @Wire private Label  batchCountBadge;

    // ── Section 2: Cheque Split View ──
    @Wire private Div    chequeSection;
    @Wire private Label  curBatchBadge;

    // Cheque filter bar
    @Wire private Textbox chqSearchBox;
    @Wire private Listbox iqaFilterBox;
    @Wire private Listbox chqStatusFilter;

    // Left panel — cheque list
    @Wire private Rows   chequeRows;
    @Wire private Label  chequeCountBadge;

    // Right panel — detail
    @Wire private Label  detailStatusBadge;
    @Wire private Div    detailEmpty;
    @Wire private Div    detailBody;

    // ── State ──
    private List<OutwardBatch>  allBatches;
    private OutwardBatch        selectedBatch;
    private List<OutwardCheque> batchCheques;
    private Long                currentUserId;
    private String              currentRole;

    // ════════════════════════════════════════════════════
    //  Page Init
    // ════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        userAvatar.setValue(dto.getInitials());
        userName.setValue(dto.getFullName());
        userRole.setValue(dto.getRoleCode().replace("_", " "));
        currentUserId = dto.getUserId();
        currentRole   = dto.getRoleCode();

        // Load batches based on role
        if ("ADMIN".equals(currentRole)) {
            allBatches = batchDao.findAll();
        } else {
            allBatches = batchDao.findByCreatedBy(currentUserId);
        }

        // Default: show batch section
        batchSection.setVisible(true);
        chequeSection.setVisible(false);

        renderBatchTable();
    }

    // ════════════════════════════════════════════════════
    //  Topbar
    // ════════════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }

    // ════════════════════════════════════════════════════
    //  Section Toggle
    // ════════════════════════════════════════════════════

    private void showSection(String section) {
        batchSection.setVisible("batch".equals(section));
        chequeSection.setVisible("cheque".equals(section));
    }

    // ════════════════════════════════════════════════════
    //  SECTION 1: Batch Table
    // ════════════════════════════════════════════════════

    private void renderBatchTable() {
        List<OutwardBatch> filtered = applyBatchFilters();
        batchCountBadge.setValue(filtered.size() + " Batches");
        batchRows.getChildren().clear();

        if (filtered.isEmpty()) {
            Row emptyRow = new Row();
            Label emptyLabel = new Label("No batches found.");
            emptyLabel.setSclass("txt-muted");
            emptyRow.appendChild(emptyLabel);
            batchRows.appendChild(emptyRow);
            return;
        }

        int idx = 1;
        for (OutwardBatch b : filtered) {
            batchRows.appendChild(buildBatchRow(idx++, b));
        }
    }

    private List<OutwardBatch> applyBatchFilters() {
        String search    = safe(searchBox.getValue()).toLowerCase();
        String statusVal = getSelected(statusFilter);
        String from      = safe(fromDateBox.getValue());
        String to        = safe(toDateBox.getValue());

        List<OutwardBatch> result = new ArrayList<>();
        for (OutwardBatch b : allBatches) {
            // Search by Batch ID
            if (!search.isEmpty()
                    && !safe(b.getBatchId()).toLowerCase().contains(search)) {
                continue;
            }
            // Status filter
            if (!statusVal.isEmpty()
                    && !statusVal.equalsIgnoreCase(safe(b.getStatus()))) {
                continue;
            }
            // From date
            if (!from.isEmpty() && b.getCreatedAt() != null) {
                if (b.getCreatedAt().toLocalDate().toString().compareTo(from) < 0) {
                    continue;
                }
            }
            // To date
            if (!to.isEmpty() && b.getCreatedAt() != null) {
                if (b.getCreatedAt().toLocalDate().toString().compareTo(to) > 0) {
                    continue;
                }
            }
            result.add(b);
        }
        return result;
    }

    private Row buildBatchRow(int idx, final OutwardBatch b) {
        Row row = new Row();

        row.appendChild(new Label(String.valueOf(idx)));

        Label batchIdLbl = new Label(safe(b.getBatchId()));
        batchIdLbl.setSclass("mono fw6");
        row.appendChild(batchIdLbl);

        row.appendChild(new Label(String.valueOf(b.getChequeCount())));

        row.appendChild(new Label(
            b.getActualAmount() != null
            ? "₹" + moneyFmt.format(b.getActualAmount()) : "—"));

        // Pending count — cheques still in PENDING status
        int pending   = chequeDao.countPendingEntries(b.getId());
        int processed = b.getChequeCount() - pending;

        Label pendingLbl = new Label(String.valueOf(pending));
        pendingLbl.setSclass(pending > 0 ? "txt-warn fw6" : "txt-success");
        row.appendChild(pendingLbl);

        Label processedLbl = new Label(String.valueOf(processed));
        processedLbl.setSclass(processed > 0 ? "txt-success fw6" : "txt-muted");
        row.appendChild(processedLbl);

        // Status badge
        Label statusBadge = new Label(getStatusLabel(b.getStatus()));
        statusBadge.setSclass(getStatusBadgeClass(b.getStatus()));
        row.appendChild(statusBadge);

        // View button
        Button viewBtn = new Button("View");
        viewBtn.setSclass("btn bo btn-sm");
        viewBtn.addEventListener(Events.ON_CLICK,
            new EventListener<Event>() {
                @Override public void onEvent(Event e) {
                    openChequeView(b);
                }
            });
        row.appendChild(viewBtn);

        return row;
    }

    // ── Batch filter listeners ──

    @Listen("onChanging = #searchBox; onChange = #searchBox")
    public void onSearchChange() {
        renderBatchTable();
    }

    @Listen("onChange = #statusFilter")
    public void onStatusFilterChange() {
        renderBatchTable();
    }

    @Listen("onChange = #fromDateBox; onChanging = #fromDateBox")
    public void onFromDateChange() {
        renderBatchTable();
    }

    @Listen("onChange = #toDateBox; onChanging = #toDateBox")
    public void onToDateChange() {
        renderBatchTable();
    }

    // ════════════════════════════════════════════════════
    //  SECTION 2: Cheque Split View
    // ════════════════════════════════════════════════════

    private void openChequeView(OutwardBatch batch) {
        selectedBatch = batch;
        batchCheques  = chequeDao.findByBatchId(batch.getId());

        curBatchBadge.setValue(safe(batch.getBatchId()));

        // Reset cheque filters
        chqSearchBox.setValue("");
        if (iqaFilterBox.getItemCount() > 0)   iqaFilterBox.setSelectedIndex(0);
        if (chqStatusFilter.getItemCount() > 0) chqStatusFilter.setSelectedIndex(0);

        // Reset detail panel to empty state
        detailStatusBadge.setValue("Select a cheque");
        detailStatusBadge.setSclass("badge b-grey");
        detailEmpty.setVisible(true);
        detailBody.setVisible(false);
        detailBody.getChildren().clear();

        showSection("cheque");
        renderChequeTable();
    }

    private void renderChequeTable() {
        List<OutwardCheque> filtered = applyChequeFilters();
        chequeCountBadge.setValue(filtered.size() + " cheques");
        chequeRows.getChildren().clear();

        if (filtered.isEmpty()) {
            Row empty = new Row();
            empty.appendChild(new Label("No cheques found."));
            chequeRows.appendChild(empty);
            return;
        }

        int idx = 1;
        for (OutwardCheque c : filtered) {
            chequeRows.appendChild(buildChequeRow(idx++, c));
        }
    }

    private List<OutwardCheque> applyChequeFilters() {
        String search    = safe(chqSearchBox.getValue()).toLowerCase();
        String iqaFilter = getSelected(iqaFilterBox);
        String statusF   = getSelected(chqStatusFilter);

        List<OutwardCheque> result = new ArrayList<>();
        for (OutwardCheque c : batchCheques) {
            // Search by cheque no or bank code
            if (!search.isEmpty()) {
                boolean matches =
                    safe(c.getChequeNo()).toLowerCase().contains(search)
                    || safe(c.getBankCode()).toLowerCase().contains(search);
                if (!matches) continue;
            }
            // IQA filter — isMicrError() = true means FAIL
            // FIX: use c.isMicrError() — the correct getter name
            if (!iqaFilter.isEmpty()) {
                boolean isFail = c.isMicrError();
                if ("FAIL".equals(iqaFilter) && !isFail) continue;
                if ("PASS".equals(iqaFilter) &&  isFail) continue;
            }
            // Status filter — MICR Error or Normal
            if (!statusF.isEmpty()) {
                boolean hasMicrError = c.isMicrError();
                if ("MICR_ERROR".equals(statusF) && !hasMicrError) continue;
                if ("NORMAL".equals(statusF)     &&  hasMicrError) continue;
            }
            result.add(c);
        }
        return result;
    }

    private Row buildChequeRow(int idx, final OutwardCheque cheque) {
        Row row = new Row();
        row.setStyle("cursor:pointer");

        row.appendChild(new Label(String.valueOf(idx)));

        Label chqNoLbl = new Label(safe(cheque.getChequeNo()));
        chqNoLbl.setSclass("mono fw6 txt-primary");
        row.appendChild(chqNoLbl);

        row.appendChild(new Label(safe(cheque.getBankCode())));

        row.appendChild(new Label(
            cheque.getAmount() != null
            ? "₹" + moneyFmt.format(cheque.getAmount()) : "—"));

        // IQA badge — FIX: c.isMicrError() not c.getIsMicrError()
        boolean isMicrError = cheque.isMicrError();
        Label iqaBadge = new Label(isMicrError ? "FAIL" : "PASS");
        iqaBadge.setSclass(isMicrError ? "badge b-fail" : "badge b-pass");
        row.appendChild(iqaBadge);

        // Status badge
        Label statusBadge = new Label(isMicrError ? "MICR Error" : "Normal");
        statusBadge.setSclass(isMicrError ? "badge b-pend" : "badge b-pass");
        row.appendChild(statusBadge);

        // Row click → load detail
        row.addEventListener(Events.ON_CLICK,
            new EventListener<Event>() {
                @Override public void onEvent(Event e) {
                    highlightRow(row);
                    buildDetailPanel(cheque);
                }
            });

        return row;
    }

    private void highlightRow(Row selected) {
        for (Object child : chequeRows.getChildren()) {
            if (child instanceof Row) {
                ((Row) child).setStyle("cursor:pointer;background:transparent");
            }
        }
        selected.setStyle("cursor:pointer;background:#E8F0FE");
    }

    // ── Cheque filter listeners ──

    @Listen("onChanging = #chqSearchBox; onChange = #chqSearchBox")
    public void onChqSearchChange() {
        if (batchCheques != null) renderChequeTable();
    }

    @Listen("onChange = #iqaFilterBox")
    public void onIqaFilterChange() {
        if (batchCheques != null) renderChequeTable();
    }

    @Listen("onChange = #chqStatusFilter")
    public void onChqStatusFilterChange() {
        if (batchCheques != null) renderChequeTable();
    }

    @Listen("onClick = #backToBatchesBtn")
    public void onBackToBatches() {
        selectedBatch = null;
        batchCheques  = null;
        showSection("batch");
        renderBatchTable();
    }

    // ════════════════════════════════════════════════════
    //  Detail Panel — builds right panel dynamically
    // ════════════════════════════════════════════════════

    private void buildDetailPanel(OutwardCheque cheque) {
        // Update header badge
        boolean isMicrError = cheque.isMicrError();
        detailStatusBadge.setValue(isMicrError ? "MICR Error" : "Normal");
        detailStatusBadge.setSclass(
            isMicrError ? "badge b-pend" : "badge b-pass");

        // Switch to detail content
        detailEmpty.setVisible(false);
        detailBody.getChildren().clear();
        detailBody.setVisible(true);

        detailBody.appendChild(buildImageSection(cheque));
        detailBody.appendChild(buildKeyInfoSection(cheque));
        detailBody.appendChild(buildProcessingStatusSection(cheque));
        detailBody.appendChild(buildMicrRepairSection(cheque));
    }

    // ── Cheque Image Section ──

    private Div buildImageSection(final OutwardCheque cheque) {
        Div wrapper = new Div();
        wrapper.setSclass("detail-img-wrapper");

        Label sectionLbl = new Label("Cheque Image");
        sectionLbl.setSclass("detail-section-lbl");
        wrapper.appendChild(sectionLbl);

        // Tab buttons
        Div tabs = new Div();
        tabs.setSclass("chq-tabs");

        final Button frontTab = new Button("Front");
        frontTab.setSclass("chq-tab active");
        final Button backTab = new Button("Back");
        backTab.setSclass("chq-tab");
        tabs.appendChild(frontTab);
        tabs.appendChild(backTab);
        wrapper.appendChild(tabs);

        // Front image
        final Div frontPanel = new Div();
        frontPanel.setSclass("chq-img-panel");
        final Image frontImg = new Image();
        frontImg.setSclass("chq-img-actual");
        frontImg.setStyle(
            "width:100%;height:auto;max-height:200px;"
            + "object-fit:contain;display:block");
        setImageSrc(frontImg, cheque.getFrontImagePath());
        frontPanel.appendChild(frontImg);
        wrapper.appendChild(frontPanel);

        // Back image (hidden)
        final Div backPanel = new Div();
        backPanel.setSclass("chq-img-panel");
        backPanel.setVisible(false);
        final Image backImg = new Image();
        backImg.setSclass("chq-img-actual");
        backImg.setStyle(
            "width:100%;height:auto;max-height:200px;"
            + "object-fit:contain;display:block");
        setImageSrc(backImg, cheque.getBackImagePath());
        backPanel.appendChild(backImg);
        wrapper.appendChild(backPanel);

        // Tab events
        frontTab.addEventListener(Events.ON_CLICK,
            new EventListener<Event>() {
                @Override public void onEvent(Event e) {
                    frontPanel.setVisible(true);
                    backPanel.setVisible(false);
                    frontTab.setSclass("chq-tab active");
                    backTab.setSclass("chq-tab");
                }
            });

        backTab.addEventListener(Events.ON_CLICK,
            new EventListener<Event>() {
                @Override public void onEvent(Event e) {
                    frontPanel.setVisible(false);
                    backPanel.setVisible(true);
                    frontTab.setSclass("chq-tab");
                    backTab.setSclass("chq-tab active");
                }
            });

        return wrapper;
    }

    // ── Key Cheque Information Section ──

    private Div buildKeyInfoSection(OutwardCheque cheque) {
        String[][] rows = {
            { "Cheque No.",     safe(cheque.getChequeNo()),    "vv mono" },
            { "Bank Code",      safe(cheque.getBankCode()),    "vv"      },
            { "Amount",
              cheque.getAmount() != null
              ? "₹" + moneyFmt.format(cheque.getAmount()) : "—",
              "vv fw6" },
            { "Amount in Words",safe(cheque.getAmountInWords()), "vvn"   },
            { "Payee Name",     safe(cheque.getPayeeName()),   "vvn"     },
            { "Account No.",    safe(cheque.getAccountNo()),   "vv mono" },
            { "Account Holder", safe(cheque.getAccountHolder()), "vvn"   },
            { "MICR Code",      safe(cheque.getMicrCode()),    "vv mono" },
        };
        return buildVsSection("Key Cheque Information", rows);
    }

    // ── Processing Status Section ──

    private Div buildProcessingStatusSection(OutwardCheque cheque) {
        Div vs = new Div();
        vs.setSclass("vs");
        vs.appendChild(buildVsTitle("Processing Status"));

        // Batch status
        String batchStatus = selectedBatch != null
                ? selectedBatch.getStatus() : "—";
        vs.appendChild(buildVrBadge(
            "Batch Status",
            getStatusLabel(batchStatus),
            getStatusBadgeClass(batchStatus)));

        // Cheque status
        vs.appendChild(buildVrBadge(
            "Cheque Status",
            getStatusLabel(cheque.getStatus()),
            getStatusBadgeClass(cheque.getStatus())));

        // IQA status — FIX: use isMicrError()
        boolean isFail = cheque.isMicrError();
        vs.appendChild(buildVrBadge(
            "IQA",
            isFail ? "FAIL" : "PASS",
            isFail ? "badge b-fail" : "badge b-pass"));

        return vs;
    }

    // ── MICR Repair Section ──

    private Div buildMicrRepairSection(OutwardCheque cheque) {
        Div vs = new Div();
        vs.setSclass("vs");
        vs.appendChild(buildVsTitle("MICR Repair Status"));

        boolean wasRepaired = "REPAIRED".equals(cheque.getRepairStatus());

        if (wasRepaired) {
            Div warnNote = new Div();
            warnNote.setSclass("note-warn");
            Label warnLbl = new Label("MICR fields were repaired by Maker");
            warnLbl.setSclass("note-warn-txt");
            warnNote.appendChild(warnLbl);
            vs.appendChild(warnNote);

            vs.appendChild(buildVrWithSclass(
                "Original MICR",
                safe(cheque.getMicrCode()),
                "vv mono txt-warn"));

            vs.appendChild(buildVrWithSclass(
                "Corrected MICR",
                safe(cheque.getMicrCodeCorrected()),
                "vv mono txt-success"));
        } else {
            Div sucNote = new Div();
            sucNote.setSclass("note-suc");
            Label sucLbl = new Label("No MICR repair required");
            sucLbl.setSclass("note-suc-txt");
            sucNote.appendChild(sucLbl);
            vs.appendChild(sucNote);
        }

        return vs;
    }

    // ════════════════════════════════════════════════════
    //  ZK Component Builder Helpers
    // ════════════════════════════════════════════════════

    private Div buildVsSection(String title, String[][] rows) {
        Div vs = new Div();
        vs.setSclass("vs");
        vs.appendChild(buildVsTitle(title));
        for (String[] r : rows) {
            vs.appendChild(buildVrWithSclass(
                r[0], r[1], r.length > 2 ? r[2] : "vv"));
        }
        return vs;
    }

    private Label buildVsTitle(String title) {
        Label t = new Label(title);
        t.setSclass("vs-title-lbl");
        return t;
    }

    private Div buildVrWithSclass(String label, String value,
                                   String valueSclass) {
        Div vr = new Div();
        vr.setSclass("vr");
        Label vl = new Label(label);
        vl.setSclass("vl");
        Label vv = new Label(value != null ? value : "—");
        vv.setSclass(valueSclass);
        vr.appendChild(vl);
        vr.appendChild(vv);
        return vr;
    }

    private Div buildVrBadge(String label, String badgeText,
                               String badgeSclass) {
        Div vr = new Div();
        vr.setSclass("vr");
        Label vl = new Label(label);
        vl.setSclass("vl");
        Label badge = new Label(badgeText);
        badge.setSclass(badgeSclass);
        vr.appendChild(vl);
        vr.appendChild(badge);
        return vr;
    }

    // ════════════════════════════════════════════════════
    //  Image Helper
    // ════════════════════════════════════════════════════

    private void setImageSrc(Image img, String path) {
        if (path != null && !path.trim().isEmpty()) {
            try {
                img.setSrc("/imageServlet?path="
                    + URLEncoder.encode(path.trim(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                img.setSrc("/imageServlet?path=" + path.trim());
            }
        } else {
            img.setSrc("");
        }
    }

    // ════════════════════════════════════════════════════
    //  Status Label / Badge Helpers
    // ════════════════════════════════════════════════════

 // ════════════════════════════════════════════════════
//  Status Label / Badge Helpers
//  Covers ALL statuses across the outward workflow:
//     Maker side  : NEEDS_REPAIR → ENTRY_PENDING → SUBMITTED
//     Checker side: CHECKER_IN_PROGRESS → CHECKER_HOLD → CHECKER_APPROVED
//     Cheque-level: CHECKER_PASSED / CHECKER_REJECTED / CHECKER_REFERRED
//     Final       : EXPORTED / REFER_BACK / REJECTED
// ════════════════════════════════════════════════════

private String getStatusLabel(String status) {
    if (status == null) return "Unknown";
    switch (status.toUpperCase()) {
        // ── Batch-level statuses ──
        case "NEEDS_REPAIR":         return "Needs MICR Repair";
        case "ENTRY_PENDING":        return "Pending Data Entry";
        case "SUBMITTED":            return "Submitted to Checker";
        case "REFER_BACK":           return "Referred Back to Maker";
        case "CHECKER_IN_PROGRESS":  return "Checker In Progress";
        case "CHECKER_HOLD":         return "On Hold (Refer Pending)";
        case "CHECKER_APPROVED":     return "Approved by Checker";
        case "EXPORTED":             return "DEM Exported";
        // ── Cheque-level statuses ──
        case "PENDING":              return "Pending";
        case "ENTRY_DONE":           return "Entry Done";
        case "CHECKER_PASSED":       return "Passed";
        case "CHECKER_REJECTED":     return "Rejected by Checker";
        case "CHECKER_REFERRED":     return "Referred";
        case "PASSED":               return "Passed";
        case "REJECTED":             return "Rejected";
        default:                     return status;
    }
}

private String getStatusBadgeClass(String status) {
    if (status == null) return "badge b-grey";
    switch (status.toUpperCase()) {
        // ── Batch-level ──
        case "NEEDS_REPAIR":         return "badge b-pend";
        case "ENTRY_PENDING":        return "badge b-info";
        case "SUBMITTED":            return "badge b-info";
        case "REFER_BACK":           return "badge b-warn";
        case "CHECKER_IN_PROGRESS":  return "badge b-info";
        case "CHECKER_HOLD":         return "badge b-warn";
        case "CHECKER_APPROVED":     return "badge b-pass";
        case "EXPORTED":             return "badge b-cbs";
        // ── Cheque-level ──
        case "PENDING":              return "badge b-pend";
        case "ENTRY_DONE":           return "badge b-info";
        case "CHECKER_PASSED":       return "badge b-pass";
        case "CHECKER_REJECTED":     return "badge b-fail";
        case "CHECKER_REFERRED":     return "badge b-warn";
        case "PASSED":               return "badge b-pass";
        case "REJECTED":             return "badge b-fail";
        default:                     return "badge b-grey";
    }
}
    // ════════════════════════════════════════════════════
    //  General Helpers
    // ════════════════════════════════════════════════════

    private String safe(String s) {
        return s != null ? s.trim() : "";
    }

    private String getSelected(Listbox lb) {
        if (lb.getSelectedItem() == null) return "";
        Object val = lb.getSelectedItem().getValue();
        return val != null ? val.toString() : "";
    }
}