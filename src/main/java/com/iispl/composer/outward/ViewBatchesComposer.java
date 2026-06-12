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
import org.zkoss.zk.ui.util.Clients;
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
import com.iispl.service.MakerOutwardService;
import com.iispl.serviceImpl.MakerOutwardServiceImpl;
import com.iispl.util.SessionUtil;

public class ViewBatchesComposer extends SelectorComposer<Component> {

    private final OutwardBatchDao  batchDao  = new OutwardBatchDaoImpl();
    private final OutwardChequeDao chequeDao = new OutwardChequeDaoImpl();
    private final DecimalFormat    moneyFmt  = new DecimalFormat("#,##0.00");

    // ── Batch List Section ──
    @Wire private Div     batchSection;
    @Wire private Textbox searchBox;
    @Wire private Listbox statusFilter;
    @Wire private Textbox fromDateBox;
    @Wire private Textbox toDateBox;
    @Wire private Rows    batchRows;
    @Wire private Label   batchCountBadge;

    // ── Batch Pagination ──
    @Wire private Div    batchPager;
    @Wire private Button btnPrevPage;
    @Wire private Button btnNextPage;
    @Wire private Label  batchPagerInfo;

    // ── Cheque Split View ──
    @Wire private Div    chequeSection;
    @Wire private Label  curBatchBadge;
    @Wire private Textbox chqSearchBox;
    @Wire private Rows   chequeRows;
    @Wire private Label  chequeCountBadge;

    // ── Cheque Pagination ──
    @Wire private Div    chequePager;
    @Wire private Button btnChqPrevPage;
    @Wire private Button btnChqNextPage;
    @Wire private Label  chequePagerInfo;

    // ── Detail Panel ──
    @Wire private Label detailStatusBadge;
    @Wire private Div   detailEmpty;
    @Wire private Div   detailBody;

    // ── Re-submit Panel ──
    @Wire private Div    resubmitPanel;
    @Wire private Div    resubmitNoteBox;
    @Wire private Label  resubmitNote;
    @Wire private Button resubmitBtn;

    // ── Service ──
    private final MakerOutwardService makerOutwardService = new MakerOutwardServiceImpl();

    // ── State ──
    private List<OutwardBatch>  allBatches;
    private OutwardBatch        selectedBatch;
    private List<OutwardCheque> batchCheques;
    private Long                currentUserId;
    private String              currentRole;
    private String              currentUserName;

    // ── Batch Pagination State ──
    private static final int    PAGE_SIZE          = 10;
    private int                 currentPage        = 0;
    private List<OutwardBatch>  currentDisplayList = new ArrayList<>();

    // ── Cheque Pagination State ──
    private static final int    CHEQUE_PAGE_SIZE   = 4;
    private int                 chequePage         = 0;
    private List<OutwardCheque> chequeDisplayList  = new ArrayList<>();

    // ════════════════════════════════════════════════════
    //  Page Init
    // ════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        currentUserId   = dto.getUserId();
        currentRole     = dto.getRoleCode();
        currentUserName = dto.getFullName();

        allBatches = "ADMIN".equals(currentRole)
            ? batchDao.findAll()
            : batchDao.findByCreatedBy(currentUserId);

        batchSection.setVisible(true);
        chequeSection.setVisible(false);

        renderBatchTable();
    }

    // ════════════════════════════════════════════════════
    //  Section Toggle
    // ════════════════════════════════════════════════════

    private void showSection(String section) {
        batchSection.setVisible("batch".equals(section));
        chequeSection.setVisible("cheque".equals(section));
        if ("cheque".equals(section)) batchPager.setVisible(false);
    }

    // ════════════════════════════════════════════════════
    //  Batch Table
    // ════════════════════════════════════════════════════

    private void renderBatchTable() {
        List<OutwardBatch> filtered = applyBatchFilters();
        currentDisplayList = filtered;
        currentPage = 0;
        batchCountBadge.setValue(filtered.size() + " Batches");
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        batchRows.getChildren().clear();

        if (currentDisplayList == null || currentDisplayList.isEmpty()) {
            Row emptyRow = new Row();
            Label emptyLabel = new Label("No batches found.");
            emptyLabel.setSclass("txt-muted");
            emptyRow.appendChild(emptyLabel);
            batchRows.appendChild(emptyRow);
            batchPager.setVisible(false);
            return;
        }

        int totalItems = currentDisplayList.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0)           currentPage = 0;

        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, totalItems);

        int idx = fromIndex + 1;
        for (OutwardBatch b : currentDisplayList.subList(fromIndex, toIndex)) {
            batchRows.appendChild(buildBatchRow(idx++, b));
        }

        batchPager.setVisible(totalPages > 1);
        batchPagerInfo.setValue("Page " + (currentPage + 1) + " of " + totalPages
                + "  (" + totalItems + " batches)");
        btnPrevPage.setDisabled(currentPage == 0);
        btnNextPage.setDisabled(currentPage >= totalPages - 1);
    }

    // ── Batch Pagination Listeners ──

    @Listen("onClick = #btnPrevPage")
    public void onPrevPage() {
        if (currentPage > 0) { currentPage--; renderCurrentPage(); }
    }

    @Listen("onClick = #btnNextPage")
    public void onNextPage() {
        if (currentDisplayList != null) {
            int tp = (int) Math.ceil((double) currentDisplayList.size() / PAGE_SIZE);
            if (currentPage < tp - 1) { currentPage++; renderCurrentPage(); }
        }
    }

    // ── Batch Filter Listeners ──

    @Listen("onClick = #btnApplyFilter")
    public void onApplyFilter() { renderBatchTable(); }

    @Listen("onClick = #btnClearFilter")
    public void onClearFilter() {
        searchBox.setValue("");
        statusFilter.setSelectedIndex(0);
        fromDateBox.setValue("");
        toDateBox.setValue("");
        renderBatchTable();
    }

    private List<OutwardBatch> applyBatchFilters() {
        String search    = safe(searchBox.getValue()).toLowerCase();
        String statusVal = getSelected(statusFilter);
        String from      = safe(fromDateBox.getValue());
        String to        = safe(toDateBox.getValue());

        List<OutwardBatch> result = new ArrayList<>();
        for (OutwardBatch b : allBatches) {
            if (!search.isEmpty()
                    && !safe(b.getBatchId()).toLowerCase().contains(search)) continue;
            if (!statusVal.isEmpty()
                    && !statusVal.equalsIgnoreCase(safe(b.getStatus()))) continue;
            if (!from.isEmpty() && b.getCreatedAt() != null) {
                if (b.getCreatedAt().toLocalDate().toString().compareTo(from) < 0) continue;
            }
            if (!to.isEmpty() && b.getCreatedAt() != null) {
                if (b.getCreatedAt().toLocalDate().toString().compareTo(to) > 0) continue;
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
        row.appendChild(new Label(b.getActualAmount() != null
            ? "₹" + moneyFmt.format(b.getActualAmount()) : "—"));

        int pending   = chequeDao.countPendingEntries(b.getId());
        int processed = b.getChequeCount() - pending;

        Label pendingLbl = new Label(String.valueOf(pending));
        pendingLbl.setSclass(pending > 0 ? "txt-warn fw6" : "txt-success");
        row.appendChild(pendingLbl);

        Label processedLbl = new Label(String.valueOf(processed));
        processedLbl.setSclass(processed > 0 ? "txt-success fw6" : "txt-muted");
        row.appendChild(processedLbl);

        Label statusBadge = new Label(getStatusLabel(b.getStatus()));
        statusBadge.setSclass(getStatusBadgeClass(b.getStatus()));
        row.appendChild(statusBadge);

        Button viewBtn = new Button("View");
        viewBtn.setSclass("btn bo btn-sm");
        viewBtn.addEventListener(Events.ON_CLICK,
            new EventListener<Event>() {
                @Override public void onEvent(Event e) { openChequeView(b); }
            });
        row.appendChild(viewBtn);

        return row;
    }

    // ════════════════════════════════════════════════════
    //  Cheque Split View
    // ════════════════════════════════════════════════════

    private void openChequeView(OutwardBatch batch) {
        selectedBatch = batch;
        batchCheques  = chequeDao.findByBatchId(batch.getId());

        curBatchBadge.setValue(safe(batch.getBatchId()));
        updateResubmitPanel(batch);

        chqSearchBox.setValue("");

        detailStatusBadge.setValue("Select a cheque");
        detailStatusBadge.setSclass("badge b-grey");
        detailEmpty.setVisible(true);
        detailBody.setVisible(false);
        detailBody.getChildren().clear();

        showSection("cheque");
        renderChequeTable();
    }

    // ── Cheque Filter Listeners ──

    @Listen("onClick = #btnSearchCheque")
    public void onSearchCheque() { renderChequeTable(); }

    @Listen("onClick = #btnClearCheque")
    public void onClearCheque() {
        chqSearchBox.setValue("");
        renderChequeTable();
    }

    @Listen("onClick = #backToBatchesBtn")
    public void onBackToBatches() {
        selectedBatch = null;
        batchCheques  = null;
        showSection("batch");
        renderBatchTable();
    }

    private void renderChequeTable() {
        List<OutwardCheque> filtered = applyChequeFilters();
        chequeDisplayList = filtered;
        chequePage = 0;
        chequeCountBadge.setValue(filtered.size() + " cheques");
        renderChequePage();
    }

    private void renderChequePage() {
        chequeRows.getChildren().clear();

        if (chequeDisplayList == null || chequeDisplayList.isEmpty()) {
            Row empty = new Row();
            empty.appendChild(new Label("No cheques found."));
            chequeRows.appendChild(empty);
            chequePager.setVisible(false);
            return;
        }

        int totalItems = chequeDisplayList.size();
        int totalPages = (int) Math.ceil((double) totalItems / CHEQUE_PAGE_SIZE);

        if (chequePage >= totalPages) chequePage = totalPages - 1;
        if (chequePage < 0)           chequePage = 0;

        int fromIndex = chequePage * CHEQUE_PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + CHEQUE_PAGE_SIZE, totalItems);

        int idx = fromIndex + 1;
        for (OutwardCheque c : chequeDisplayList.subList(fromIndex, toIndex)) {
            chequeRows.appendChild(buildChequeRow(idx++, c));
        }

        chequePager.setVisible(totalPages > 1);
        chequePagerInfo.setValue("Page " + (chequePage + 1) + " of " + totalPages
                + "  (" + totalItems + " cheques)");
        btnChqPrevPage.setDisabled(chequePage == 0);
        btnChqNextPage.setDisabled(chequePage >= totalPages - 1);
    }

    // ── Cheque Pagination Listeners ──

    @Listen("onClick = #btnChqPrevPage")
    public void onChqPrevPage() {
        if (chequePage > 0) { chequePage--; renderChequePage(); }
    }

    @Listen("onClick = #btnChqNextPage")
    public void onChqNextPage() {
        if (chequeDisplayList != null) {
            int tp = (int) Math.ceil((double) chequeDisplayList.size() / CHEQUE_PAGE_SIZE);
            if (chequePage < tp - 1) { chequePage++; renderChequePage(); }
        }
    }

    private List<OutwardCheque> applyChequeFilters() {
        String search = safe(chqSearchBox.getValue()).toLowerCase();
        List<OutwardCheque> result = new ArrayList<>();
        for (OutwardCheque c : batchCheques) {
            if (!search.isEmpty()) {
                boolean matches = safe(c.getChequeNo()).toLowerCase().contains(search)
                    || safe(c.getPayeeName()).toLowerCase().contains(search);
                if (!matches) continue;
            }
            result.add(c);
        }
        return result;
    }

    private Row buildChequeRow(int idx, final OutwardCheque cheque) {
        Row row = new Row();
        row.setStyle("cursor:pointer");

        // Cheque Number
        Label chqNoLbl = new Label(safe(cheque.getChequeNo()));
        chqNoLbl.setSclass("mono fw6 txt-primary");
        row.appendChild(chqNoLbl);

        // Payee Name
        Label payeeLbl = new Label(safe(cheque.getPayeeName()));
        payeeLbl.setSclass("txt-ellipsis");
        row.appendChild(payeeLbl);

        // Amount
        row.appendChild(new Label(cheque.getAmount() != null
            ? "₹" + moneyFmt.format(cheque.getAmount()) : "—"));

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
            if (child instanceof Row)
                ((Row) child).setStyle("cursor:pointer;background:transparent");
        }
        selected.setStyle("cursor:pointer;background:#E8F0FE");
    }

    // ════════════════════════════════════════════════════
    //  Detail Panel
    // ════════════════════════════════════════════════════

    private void buildDetailPanel(OutwardCheque cheque) {
        boolean isMicrError = cheque.isMicrError();
        detailStatusBadge.setValue(isMicrError ? "MICR Error" : "Normal");
        detailStatusBadge.setSclass(isMicrError ? "badge b-pend" : "badge b-pass");

        detailEmpty.setVisible(false);
        detailBody.getChildren().clear();
        detailBody.setVisible(true);

        detailBody.appendChild(buildImageSection(cheque));
        detailBody.appendChild(buildKeyInfoSection(cheque));
        detailBody.appendChild(buildProcessingStatusSection(cheque));
        detailBody.appendChild(buildMicrRepairSection(cheque));
    }

    private Div buildImageSection(final OutwardCheque cheque) {
        Div wrapper = new Div();
        wrapper.setSclass("detail-img-wrapper");

        Label sectionLbl = new Label("Cheque Image");
        sectionLbl.setSclass("detail-section-lbl");
        wrapper.appendChild(sectionLbl);

        Div tabs = new Div();
        tabs.setSclass("chq-tabs");
        final Button frontTab = new Button("Front");
        frontTab.setSclass("chq-tab active");
        final Button backTab = new Button("Back");
        backTab.setSclass("chq-tab");
        tabs.appendChild(frontTab);
        tabs.appendChild(backTab);
        wrapper.appendChild(tabs);

        final Div frontPanel = new Div();
        frontPanel.setSclass("chq-img-panel");
        final Image frontImg = new Image();
        frontImg.setSclass("chq-img-actual");
        frontImg.setStyle("width:100%;height:auto;max-height:200px;object-fit:contain;display:block");
        setImageSrc(frontImg, cheque.getFrontImagePath());
        frontPanel.appendChild(frontImg);
        wrapper.appendChild(frontPanel);

        final Div backPanel = new Div();
        backPanel.setSclass("chq-img-panel");
        backPanel.setVisible(false);
        final Image backImg = new Image();
        backImg.setSclass("chq-img-actual");
        backImg.setStyle("width:100%;height:auto;max-height:200px;object-fit:contain;display:block");
        setImageSrc(backImg, cheque.getBackImagePath());
        backPanel.appendChild(backImg);
        wrapper.appendChild(backPanel);

        frontTab.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
            @Override public void onEvent(Event e) {
                frontPanel.setVisible(true);  backPanel.setVisible(false);
                frontTab.setSclass("chq-tab active"); backTab.setSclass("chq-tab");
            }
        });
        backTab.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
            @Override public void onEvent(Event e) {
                frontPanel.setVisible(false); backPanel.setVisible(true);
                frontTab.setSclass("chq-tab"); backTab.setSclass("chq-tab active");
            }
        });

        return wrapper;
    }

    private Div buildKeyInfoSection(OutwardCheque cheque) {
        String[][] rows = {
            { "Cheque No.",      safe(cheque.getChequeNo()),      "vv mono" },
            { "Bank Code",       safe(cheque.getBankCode()),      "vv"      },
            { "Amount",
              cheque.getAmount() != null
              ? "₹" + moneyFmt.format(cheque.getAmount()) : "—", "vv fw6"  },
            { "Amount in Words", safe(cheque.getAmountInWords()), "vvn"     },
            { "Payee Name",      safe(cheque.getPayeeName()),     "vvn"     },
            { "Account No.",     safe(cheque.getAccountNo()),     "vv mono" },
            { "Account Holder",  safe(cheque.getAccountHolder()), "vvn"     },
            { "MICR Code",       safe(cheque.getMicrCode()),      "vv mono" },
        };
        return buildVsSection("Key Cheque Information", rows);
    }

    private Div buildProcessingStatusSection(OutwardCheque cheque) {
        Div vs = new Div();
        vs.setSclass("vs");
        vs.appendChild(buildVsTitle("Processing Status"));
        String batchStatus = selectedBatch != null ? selectedBatch.getStatus() : "—";
        vs.appendChild(buildVrBadge("Batch Status",
            getStatusLabel(batchStatus), getStatusBadgeClass(batchStatus)));
        vs.appendChild(buildVrBadge("Cheque Status",
            getStatusLabel(cheque.getStatus()), getStatusBadgeClass(cheque.getStatus())));
        return vs;
    }

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
            vs.appendChild(buildVrWithSclass("Original MICR",
                safe(cheque.getMicrCode()), "vv mono txt-warn"));
            vs.appendChild(buildVrWithSclass("Corrected MICR",
                safe(cheque.getMicrCodeCorrected()), "vv mono txt-success"));
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
    //  Builder Helpers
    // ════════════════════════════════════════════════════

    private Div buildVsSection(String title, String[][] rows) {
        Div vs = new Div();
        vs.setSclass("vs");
        vs.appendChild(buildVsTitle(title));
        for (String[] r : rows)
            vs.appendChild(buildVrWithSclass(r[0], r[1], r.length > 2 ? r[2] : "vv"));
        return vs;
    }

    private Label buildVsTitle(String title) {
        Label t = new Label(title);
        t.setSclass("vs-title-lbl");
        return t;
    }

    private Div buildVrWithSclass(String label, String value, String valueSclass) {
        Div vr = new Div();
        vr.setSclass("vr");
        Label vl = new Label(label); vl.setSclass("vl");
        Label vv = new Label(value != null ? value : "—"); vv.setSclass(valueSclass);
        vr.appendChild(vl); vr.appendChild(vv);
        return vr;
    }

    private Div buildVrBadge(String label, String badgeText, String badgeSclass) {
        Div vr = new Div();
        vr.setSclass("vr");
        Label vl = new Label(label); vl.setSclass("vl");
        Label badge = new Label(badgeText); badge.setSclass(badgeSclass);
        vr.appendChild(vl); vr.appendChild(badge);
        return vr;
    }

    // ════════════════════════════════════════════════════
    //  Re-submit Panel
    // ════════════════════════════════════════════════════

    private void updateResubmitPanel(OutwardBatch batch) {
        if (batch == null || !"REFER_BACK".equals(batch.getStatus())) {
            resubmitPanel.setVisible(false);
            return;
        }
        int remaining = makerOutwardService.countActiveReferrals(batch.getId());
        resubmitPanel.setVisible(true);
        if (remaining > 0) {
            resubmitNoteBox.setSclass("note warn flex1");
            resubmitNote.setValue("⚠ " + remaining
                + " cheque(s) still need to be fixed before re-submission.");
            resubmitBtn.setDisabled(true);
        } else {
            resubmitNoteBox.setSclass("note suc flex1");
            resubmitNote.setValue("✓ All referred cheques fixed. Ready to re-submit to Checker.");
            resubmitBtn.setDisabled(false);
        }
    }

    @Listen("onClick = #resubmitBtn")
    public void onResubmitBatch() {
        if (selectedBatch == null) return;
        int remaining = makerOutwardService.countActiveReferrals(selectedBatch.getId());
        if (remaining > 0) {
            Clients.showNotification(remaining + " cheque(s) still need to be fixed.",
                "warning", null, "top_center", 2500);
            return;
        }
        resubmitBtn.setLabel("Re-submitting...");
        resubmitBtn.setDisabled(true);
        boolean ok = makerOutwardService.resubmitBatch(
            selectedBatch.getId(), currentUserId, currentUserName);
        if (!ok) {
            resubmitBtn.setLabel("↩ Re-submit to Checker");
            resubmitBtn.setDisabled(false);
            Clients.showNotification("Re-submit failed. Please try again.",
                "error", null, "top_center", 3000);
            return;
        }
        Clients.showNotification("✓ Batch " + selectedBatch.getBatchId()
            + " re-submitted to Checker.", "info", null, "top_center", 3000);
        allBatches = "ADMIN".equals(currentRole)
            ? batchDao.findAll() : batchDao.findByCreatedBy(currentUserId);
        selectedBatch = null;
        showSection("batch");
        renderBatchTable();
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
    //  Status Helpers
    // ════════════════════════════════════════════════════

    private String getStatusLabel(String status) {
        if (status == null) return "Unknown";
        switch (status.toUpperCase()) {
            case "NEEDS_REPAIR":        return "Needs MICR Repair";
            case "ENTRY_PENDING":       return "Pending Data Entry";
            case "SUBMITTED":           return "Submitted";
            case "REFER_BACK":          return "Referred Back";
            case "CHECKER_IN_PROGRESS": return "Checker In Progress";
            case "CHECKER_HOLD":        return "On Hold";
            case "CHECKER_APPROVED":    return "Approved";
            case "EXPORTED":            return "Exported";
            case "REJECTED":            return "Rejected";
            default:                    return status;
        }
    }

    private String getStatusBadgeClass(String status) {
        if (status == null) return "badge b-grey";
        switch (status.toUpperCase()) {
            case "NEEDS_REPAIR":        return "badge b-pend";
            case "ENTRY_PENDING":       return "badge b-info";
            case "SUBMITTED":           return "badge b-info";
            case "REFER_BACK":          return "badge b-warn";
            case "CHECKER_IN_PROGRESS": return "badge b-info";
            case "CHECKER_HOLD":        return "badge b-ref";
            case "CHECKER_APPROVED":    return "badge b-pass";
            case "EXPORTED":            return "badge b-pass";
            case "REJECTED":            return "badge b-fail";
            default:                    return "badge b-grey";
        }
    }

    // ════════════════════════════════════════════════════
    //  General Helpers
    // ════════════════════════════════════════════════════

    private String safe(String s) { return s != null ? s.trim() : ""; }

    private String getSelected(Listbox lb) {
        if (lb.getSelectedItem() == null) return "";
        Object val = lb.getSelectedItem().getValue();
        return val != null ? val.toString() : "";
    }
}