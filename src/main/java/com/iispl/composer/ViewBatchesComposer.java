package com.iispl.composer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
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

import com.iispl.dao.InwardChequeDao;
import com.iispl.daoImpl.InwardChequeDaoImpl;
import com.iispl.dto.InwardBatchDto;
import com.iispl.dto.InwardChequeDto;
import com.iispl.dto.LoginDTO;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.BpxfUploadService;
import com.iispl.serviceImpl.BpxfUploadServiceImpl;
import com.iispl.util.SessionUtil;

public class ViewBatchesComposer extends SelectorComposer<Component> {

    private final BpxfUploadService service   = new BpxfUploadServiceImpl();
    private final InwardChequeDao   chequeDao = new InwardChequeDaoImpl();
    private final DecimalFormat     moneyFmt  = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ── Topbar ──
    @Wire private Label   userAvatar;
    @Wire private Label   userName;
    @Wire private Label   userRole;

    // ── Section 1: Batch List ──
    @Wire private Div     batchSection;
    @Wire private Textbox searchBox;
    @Wire private Listbox statusFilter;
    @Wire private Textbox fromDateBox;
    @Wire private Textbox toDateBox;
    @Wire private Rows    batchRows;
    @Wire private Label   batchCountBadge;

    // ── Section 2: Cheque Split View ──
    @Wire private Div     chequeSection;
    @Wire private Label   curBatchBadge;

    // Cheque filter bar
    @Wire private Textbox chqSearchBox;
    @Wire private Listbox iqaFilterBox;
    @Wire private Listbox micrFilterBox;

    // Left panel — cheque list
    @Wire private Rows    chequeRows;
    @Wire private Label   chequeCountBadge;

    // Right panel — detail
    @Wire private Label   detailStatusBadge;
    @Wire private Div     detailEmpty;
    @Wire private Div     detailBody;

    // ── Wired image components (static in ZUL) ──
    @Wire private Div     detailImgWrapper;
    @Wire private Div     detailFrontPanel;
    @Wire private Div     detailBackPanel;
    @Wire private Image   detailFrontImg;
    @Wire private Image   detailBackImg;
    @Wire private Button  detailFrontTab;
    @Wire private Button  detailBackTab;

    // ── State ──
    private List<InwardBatchDto>  allBatches;
    private InwardBatchDto        selectedBatch;
    private List<InwardChequeDto> batchCheques;

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

        allBatches = service.getAllBatches();

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
        List<InwardBatchDto> filtered = applyBatchFilters();
        batchCountBadge.setValue(filtered.size() + " Batches");
        batchRows.getChildren().clear();

        if (filtered.isEmpty()) {
            Row emptyRow = new Row();
            Label emptyLbl = new Label("No batches found.");
            emptyLbl.setSclass("txt-muted");
            emptyRow.appendChild(emptyLbl);
            batchRows.appendChild(emptyRow);
            return;
        }

        int idx = 1;
        for (InwardBatchDto b : filtered) {
            batchRows.appendChild(buildBatchRow(idx++, b));
        }
    }

    private List<InwardBatchDto> applyBatchFilters() {
        String search    = safe(searchBox.getValue()).toLowerCase();
        String statusVal = getSelected(statusFilter);
        String from      = safe(fromDateBox.getValue());
        String to        = safe(toDateBox.getValue());

        List<InwardBatchDto> result = new ArrayList<>();
        for (InwardBatchDto b : allBatches) {
            if (!search.isEmpty()) {
                boolean matches =
                    safe(b.getBatchId()).toLowerCase().contains(search)
                    || safe(b.getSourceFileName()).toLowerCase().contains(search);
                if (!matches) continue;
            }
            if (!statusVal.isEmpty()
                    && !statusVal.equalsIgnoreCase(safe(b.getStatus()))) {
                continue;
            }
            if (!from.isEmpty() && b.getParsedAt() != null) {
                if (b.getParsedAt().toLocalDate().toString().compareTo(from) < 0) continue;
            }
            if (!to.isEmpty() && b.getParsedAt() != null) {
                if (b.getParsedAt().toLocalDate().toString().compareTo(to) > 0) continue;
            }
            result.add(b);
        }
        return result;
    }

    private Row buildBatchRow(int idx, final InwardBatchDto b) {
        Row row = new Row();

        row.appendChild(new Label(String.valueOf(idx)));

        Label batchIdLbl = new Label(safe(b.getBatchId()));
        batchIdLbl.setSclass("mono fw6");
        row.appendChild(batchIdLbl);

        row.appendChild(new Label(safe(b.getSourceFileName())));
        row.appendChild(new Label(String.valueOf(b.getTotalCheques())));

        Label micrLbl = new Label(String.valueOf(b.getMicrErrorCount()));
        micrLbl.setSclass(b.getMicrErrorCount() > 0 ? "txt-warn fw6" : "txt-success");
        row.appendChild(micrLbl);

        row.appendChild(new Label(
            b.getParsedAt() != null ? b.getParsedAt().format(FMT) : "—"));

        Label statusBadge = new Label(getStatusLabel(b.getStatus()));
        statusBadge.setSclass(getStatusBadgeClass(b.getStatus()));
        row.appendChild(statusBadge);

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
    public void onSearchChange() { renderBatchTable(); }

    @Listen("onChange = #statusFilter")
    public void onStatusFilterChange() { renderBatchTable(); }

    @Listen("onChange = #fromDateBox; onChanging = #fromDateBox")
    public void onFromDateChange() { renderBatchTable(); }

    @Listen("onChange = #toDateBox; onChanging = #toDateBox")
    public void onToDateChange() { renderBatchTable(); }

    // ════════════════════════════════════════════════════
    //  SECTION 2: Cheque Split View
    // ════════════════════════════════════════════════════

    private void openChequeView(InwardBatchDto batch) {
        selectedBatch = batch;

        List<InwardCheque> entities = chequeDao.findByBatchId(batch.getId());
        batchCheques = new ArrayList<>();
        for (InwardCheque e : entities) {
            batchCheques.add(InwardChequeDto.from(e));
        }

        curBatchBadge.setValue(safe(batch.getBatchId()));

        chqSearchBox.setValue("");
        if (iqaFilterBox.getItemCount()  > 0) iqaFilterBox.setSelectedIndex(0);
        if (micrFilterBox.getItemCount() > 0) micrFilterBox.setSelectedIndex(0);

        detailStatusBadge.setValue("Select a cheque");
        detailStatusBadge.setSclass("badge b-grey");
        detailEmpty.setVisible(true);
        detailBody.setVisible(false);
        detailImgWrapper.setVisible(false);

        showSection("cheque");
        renderChequeTable();
    }

    private void renderChequeTable() {
        List<InwardChequeDto> filtered = applyChequeFilters();
        chequeCountBadge.setValue(filtered.size() + " cheques");
        chequeRows.getChildren().clear();

        if (filtered.isEmpty()) {
            Row empty = new Row();
            empty.appendChild(new Label("No cheques found."));
            chequeRows.appendChild(empty);
            return;
        }

        int idx = 1;
        for (InwardChequeDto c : filtered) {
            chequeRows.appendChild(buildChequeRow(idx++, c));
        }
    }

    private List<InwardChequeDto> applyChequeFilters() {
        String search     = safe(chqSearchBox.getValue()).toLowerCase();
        String iqaFilter  = getSelected(iqaFilterBox);
        String micrFilter = getSelected(micrFilterBox);

        List<InwardChequeDto> result = new ArrayList<>();
        for (InwardChequeDto c : batchCheques) {
            if (!search.isEmpty()) {
                boolean matches =
                    safe(c.getChequeNo()).toLowerCase().contains(search)
                    || safe(c.getBankCode()).toLowerCase().contains(search);
                if (!matches) continue;
            }
            if (!iqaFilter.isEmpty()
                    && !iqaFilter.equalsIgnoreCase(safe(c.getIqaStatus()))) {
                continue;
            }
            if (!micrFilter.isEmpty()) {
                boolean hasMicr = c.isMicrError();
                if ("MICR_ERROR".equals(micrFilter) && !hasMicr) continue;
                if ("NORMAL".equals(micrFilter)     &&  hasMicr) continue;
            }
            result.add(c);
        }
        return result;
    }

    private Row buildChequeRow(int idx, final InwardChequeDto cheque) {
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

        String iqaStatus = safe(cheque.getIqaStatus());
        Label iqaBadge = new Label(iqaStatus.isEmpty() ? "—" : iqaStatus);
        iqaBadge.setSclass("FAIL".equalsIgnoreCase(iqaStatus)
            ? "badge b-fail" : "badge b-pass");
        row.appendChild(iqaBadge);

        boolean isMicr = cheque.isMicrError();
        Label micrBadge = new Label(isMicr ? "MICR Error" : "Normal");
        micrBadge.setSclass(isMicr ? "badge b-pend" : "badge b-pass");
        row.appendChild(micrBadge);

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

    @Listen("onChange = #micrFilterBox")
    public void onMicrFilterChange() {
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
    //  Tab Listeners — wired static buttons
    // ════════════════════════════════════════════════════

    @Listen("onClick = #detailFrontTab")
    public void onDetailFrontTab() {
        detailFrontPanel.setVisible(true);
        detailBackPanel.setVisible(false);
        detailFrontTab.setSclass("chq-tab active");
        detailBackTab.setSclass("chq-tab");
    }

    @Listen("onClick = #detailBackTab")
    public void onDetailBackTab() {
        detailFrontPanel.setVisible(false);
        detailBackPanel.setVisible(true);
        detailFrontTab.setSclass("chq-tab");
        detailBackTab.setSclass("chq-tab active");
    }

    // ════════════════════════════════════════════════════
    //  Detail Panel
    // ════════════════════════════════════════════════════

    private void buildDetailPanel(InwardChequeDto cheque) {
        boolean isMicr = cheque.isMicrError();
        detailStatusBadge.setValue(isMicr ? "MICR Error" : "Normal");
        detailStatusBadge.setSclass(isMicr ? "badge b-pend" : "badge b-pass");

        detailEmpty.setVisible(false);

        // Clear only the info sections — image wrapper stays in ZUL
        detailBody.getChildren().clear();
        detailBody.setVisible(true);

        // Load images via wired components — same as reference code
        loadChequeImages(cheque);

        detailBody.appendChild(buildKeyInfoSection(cheque));
        detailBody.appendChild(buildProcessingStatusSection(cheque));
        detailBody.appendChild(buildMicrRepairSection(cheque));
    }

    // ════════════════════════════════════════════════════
    //  Image Loader — exactly like MicrRepairComposer
    // ════════════════════════════════════════════════════

    private void loadChequeImages(InwardChequeDto cheque) {
        String frontPath = cheque.getFrontImagePath();
        String backPath  = cheque.getBackImagePath();

        if (frontPath != null && !frontPath.trim().isEmpty()) {
            try {
                detailFrontImg.setSrc("/imageServlet?path="
                    + URLEncoder.encode(frontPath.trim(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                detailFrontImg.setSrc("/imageServlet?path=" + frontPath.trim());
            }
        } else {
            detailFrontImg.setSrc("");
        }

        if (backPath != null && !backPath.trim().isEmpty()) {
            try {
                detailBackImg.setSrc("/imageServlet?path="
                    + URLEncoder.encode(backPath.trim(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                detailBackImg.setSrc("/imageServlet?path=" + backPath.trim());
            }
        } else {
            detailBackImg.setSrc("");
        }

        detailFrontPanel.setVisible(true);
        detailBackPanel.setVisible(false);
        detailFrontTab.setSclass("chq-tab active");
        detailBackTab.setSclass("chq-tab");
        detailImgWrapper.setVisible(true);
    }

    // ════════════════════════════════════════════════════
    //  Key Cheque Information Section
    // ════════════════════════════════════════════════════

    private Div buildKeyInfoSection(InwardChequeDto cheque) {
        String[][] rows = {
            { "Cheque No.",       safe(cheque.getChequeNo()),           "vv mono"  },
            { "Cheque Date",      str(cheque.getChequeDate()),          "vv"       },
            { "Amount (CXF)",
              cheque.getAmount() != null
              ? "₹" + moneyFmt.format(cheque.getAmount()) : "—",        "vv fw6"   },
            { "Amount in Words",  safe(cheque.getAmountInWords()),      "vvn"      },
            { "Amount (OCR)",
              cheque.getAmountOcr() != null
              ? "₹" + moneyFmt.format(cheque.getAmountOcr()) : "—",     "vv"       },
            { "Payee Name",       safe(cheque.getPayeeName()),          "vvn"      },
            { "Bank Code",        safe(cheque.getBankCode()),           "vv"       },
            { "Branch Code",      safe(cheque.getBranchCode()),         "vv mono"  },
            { "City Code",        safe(cheque.getCityCode()),           "vv"       },
            { "MICR Code (Raw)",  safe(cheque.getMicrCodeRaw()),        "vv mono"  },
            { "Drawee Acc. No.",  safe(cheque.getDraweeAccountNumber()), "vv mono" },
            { "Drawee Holder",    safe(cheque.getDraweeAccountHolder()), "vvn"     },
            { "Presenting Bank",  safe(cheque.getPresentingBankName()),  "vvn"     },
        };
        return buildVsSection("Key Cheque Information", rows);
    }

    // ════════════════════════════════════════════════════
    //  Processing Status Section
    // ════════════════════════════════════════════════════

    private Div buildProcessingStatusSection(InwardChequeDto cheque) {
        Div vs = new Div();
        vs.setSclass("vs");
        vs.appendChild(buildVsTitle("Processing Status"));

        String batchStatus = selectedBatch != null ? selectedBatch.getStatus() : "—";
        vs.appendChild(buildVrBadge(
            "Batch Status",
            getStatusLabel(batchStatus),
            getStatusBadgeClass(batchStatus)));

        vs.appendChild(buildVrBadge(
            "Cheque Status",
            getStatusLabel(cheque.getStatus()),
            getStatusBadgeClass(cheque.getStatus())));

        String iqa = safe(cheque.getIqaStatus());
        vs.appendChild(buildVrBadge(
            "IQA",
            iqa.isEmpty() ? "—" : iqa,
            "FAIL".equalsIgnoreCase(iqa) ? "badge b-fail" : "badge b-pass"));

        Boolean acctValid = cheque.getIsAccountValid();
        vs.appendChild(buildVrBadge(
            "Account Valid",
            acctValid == null ? "—" : (acctValid ? "Yes" : "No"),
            Boolean.TRUE.equals(acctValid) ? "badge b-pass" : "badge b-fail"));

        Boolean bankMatch = cheque.getIsBankMatched();
        vs.appendChild(buildVrBadge(
            "Bank Matched",
            bankMatch == null ? "—" : (bankMatch ? "Yes" : "No"),
            Boolean.TRUE.equals(bankMatch) ? "badge b-pass" : "badge b-fail"));

        return vs;
    }

    // ════════════════════════════════════════════════════
    //  MICR Repair Section
    // ════════════════════════════════════════════════════

    private Div buildMicrRepairSection(InwardChequeDto cheque) {
        Div vs = new Div();
        vs.setSclass("vs");
        vs.appendChild(buildVsTitle("MICR Repair Status"));

        boolean wasRepaired = "REPAIRED".equalsIgnoreCase(cheque.getRepairStatus());

        if (wasRepaired) {
            Div warnNote = new Div();
            warnNote.setSclass("note-warn");
            Label warnLbl = new Label("MICR fields were repaired by Maker");
            warnLbl.setSclass("note-warn-txt");
            warnNote.appendChild(warnLbl);
            vs.appendChild(warnNote);
            vs.appendChild(buildVrWithSclass(
                "Original MICR",
                safe(cheque.getMicrCodeRaw()),
                "vv mono txt-warn"));
            vs.appendChild(buildVrWithSclass(
                "Corrected MICR",
                safe(cheque.getMicrCodeCorrected()),
                "vv mono txt-success"));
        } else if ("NEEDS_REPAIR".equalsIgnoreCase(cheque.getRepairStatus())) {
            Div warnNote = new Div();
            warnNote.setSclass("note-warn");
            Label warnLbl = new Label("MICR error — awaiting repair");
            warnLbl.setSclass("note-warn-txt");
            warnNote.appendChild(warnLbl);
            vs.appendChild(warnNote);
            vs.appendChild(buildVrWithSclass(
                "Raw MICR",
                safe(cheque.getMicrCodeRaw()),
                "vv mono txt-warn"));
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
            vs.appendChild(buildVrWithSclass(r[0], r[1], r.length > 2 ? r[2] : "vv"));
        }
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
        Label vl = new Label(label);
        vl.setSclass("vl");
        Label vv = new Label(value != null ? value : "—");
        vv.setSclass(valueSclass);
        vr.appendChild(vl);
        vr.appendChild(vv);
        return vr;
    }

    private Div buildVrBadge(String label, String badgeText, String badgeSclass) {
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
    //  Status Label / Badge Helpers
    // ════════════════════════════════════════════════════

    private String getStatusLabel(String status) {
        if (status == null) return "Unknown";
        switch (status.toUpperCase()) {
            case "RECEIVED":   return "Received";
            case "PROCESSING": return "Processing";
            case "PROCESSED":  return "Processed";
            case "REJECTED":   return "Rejected";
            case "ENTRY_DONE": return "Entry Done";
            case "SUBMITTED":  return "Submitted";
            case "ACCEPTED":   return "Accepted";
            case "RETURNED":   return "Returned";
            case "SEND_BACK":  return "Send Back";
            default:           return status;
        }
    }

    private String getStatusBadgeClass(String status) {
        if (status == null) return "badge b-grey";
        switch (status.toUpperCase()) {
            case "RECEIVED":   return "badge b-info";
            case "PROCESSING": return "badge b-pend";
            case "PROCESSED":  return "badge b-pass";
            case "ACCEPTED":   return "badge b-pass";
            case "REJECTED":   return "badge b-fail";
            case "RETURNED":   return "badge b-fail";
            case "ENTRY_DONE": return "badge b-info";
            case "SUBMITTED":  return "badge b-info";
            case "SEND_BACK":  return "badge b-pend";
            default:           return "badge b-grey";
        }
    }

    // ════════════════════════════════════════════════════
    //  General Helpers
    // ════════════════════════════════════════════════════

    private String safe(String s) {
        return s != null ? s.trim() : "";
    }

    private String str(Object o) {
        return o != null ? o.toString() : "—";
    }

    private String getSelected(Listbox lb) {
        if (lb.getSelectedItem() == null) return "";
        Object val = lb.getSelectedItem().getValue();
        return val != null ? val.toString() : "";
    }
}