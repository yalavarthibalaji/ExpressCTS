package com.iispl.composer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
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

    private static final long serialVersionUID = 1L;

    private final BpxfUploadService service   = new BpxfUploadServiceImpl();
    private final InwardChequeDao   chequeDao = new InwardChequeDaoImpl();
    private final DecimalFormat     moneyFmt  = new DecimalFormat("#,##0.00");

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ── Section 1: Batch List ─────────────────────────────────────────────

    @Wire private Div     batchSection;
    @Wire private Textbox searchBox;
    @Wire private Listbox statusFilter;
    @Wire private Datebox batchDateFilter;
    @Wire private Button  btnClearBatchDate;
    @Wire private Listbox batchListbox;
    @Wire private Label   batchCountBadge;

    // ── Section 2: Cheque Split View ──────────────────────────────────────

    @Wire private Div     chequeSection;
    @Wire private Label   curBatchBadge;

    @Wire private Textbox chqSearchBox;
    @Wire private Listbox iqaFilterBox;
    @Wire private Listbox micrFilterBox;

    @Wire private Listbox chequeListbox;
    @Wire private Label   chequeCountBadge;

    @Wire private Label   detailStatusBadge;
    @Wire private Div     detailEmpty;
    @Wire private Div     detailBody;

    @Wire private Div     detailImgWrapper;
    @Wire private Div     detailFrontPanel;
    @Wire private Div     detailBackPanel;
    @Wire private Image   detailFrontImg;
    @Wire private Image   detailBackImg;
    @Wire private Button  detailFrontTab;
    @Wire private Button  detailBackTab;

    // ── State ─────────────────────────────────────────────────────────────

    private List<InwardBatchDto>  allBatches;
    private InwardBatchDto        selectedBatch;
    private List<InwardChequeDto> batchCheques;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        allBatches = service.getAllBatches();

        batchSection.setVisible(true);
        chequeSection.setVisible(false);

        wireFilterEvents();
        renderBatchTable();
    }

    // =====================================================================
    // FILTER EVENT WIRING
    // =====================================================================

    private void wireFilterEvents() {

        // Batch search — InputEvent to avoid stale getValue()
        if (searchBox != null) {
            searchBox.addEventListener(Events.ON_CHANGE, e -> {
                InputEvent ie = (InputEvent) e;
                renderBatchTableWithSearch(ie.getValue());
            });
        }

        // Status filter — ON_SELECT is the correct event for mold="select" listbox
        if (statusFilter != null) {
            statusFilter.addEventListener(Events.ON_SELECT, e -> renderBatchTable());
        }

        // Date filter
        if (batchDateFilter != null) {
            batchDateFilter.addEventListener(Events.ON_CHANGE, e -> renderBatchTable());
        }

        // Clear date button
        if (btnClearBatchDate != null) {
            btnClearBatchDate.addEventListener(Events.ON_CLICK, e -> {
                batchDateFilter.setValue(null);
                renderBatchTable();
            });
        }

        // Cheque search — InputEvent to avoid stale getValue()
        if (chqSearchBox != null) {
            chqSearchBox.addEventListener(Events.ON_CHANGE, e -> {
                InputEvent ie = (InputEvent) e;
                renderChequeTableWithSearch(ie.getValue());
            });
        }

        // IQA filter
        if (iqaFilterBox != null) {
            iqaFilterBox.addEventListener(Events.ON_SELECT, e -> {
                if (batchCheques != null) renderChequeTable();
            });
        }

        // MICR filter
        if (micrFilterBox != null) {
            micrFilterBox.addEventListener(Events.ON_SELECT, e -> {
                if (batchCheques != null) renderChequeTable();
            });
        }
    }

    // =====================================================================
    // SECTION TOGGLE
    // =====================================================================

    private void showSection(String section) {
        batchSection.setVisible("batch".equals(section));
        chequeSection.setVisible("cheque".equals(section));
    }

    // =====================================================================
    // SECTION 1 — BATCH TABLE
    // =====================================================================

    private void renderBatchTable() {
        renderBatchTableWithSearch(safe(searchBox.getValue()));
    }

    private void renderBatchTableWithSearch(String rawSearch) {
        List<InwardBatchDto> filtered = applyBatchFilters(rawSearch);

        batchCountBadge.setValue(filtered.size() + " Batches");
        batchListbox.getItems().clear();

        if (filtered.isEmpty()) {
            Listitem empty = new Listitem();
            Listcell emptyCell = new Listcell("No batches found.");
            emptyCell.setSpan(8);
            empty.appendChild(emptyCell);
            batchListbox.appendChild(empty);
            return;
        }

        int idx = 1;
        for (InwardBatchDto b : filtered) {
            batchListbox.appendChild(buildBatchRow(idx++, b));
        }
    }

    private List<InwardBatchDto> applyBatchFilters(String searchTerm) {
        String search    = searchTerm != null ? searchTerm.trim().toLowerCase() : "";
        String statusVal = getSelected(statusFilter);

        // Read selected date from datebox
        LocalDate filterDate = null;
        if (batchDateFilter != null && batchDateFilter.getValue() != null) {
            filterDate = batchDateFilter.getValue()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        List<InwardBatchDto> result = new ArrayList<>();

        for (InwardBatchDto b : allBatches) {

            // Search — batch ID or file name
            if (!search.isEmpty()) {
                boolean matches =
                        safe(b.getBatchId()).toLowerCase().contains(search)
                     || safe(b.getSourceFileName()).toLowerCase().contains(search);
                if (!matches) continue;
            }

            // Status
            if (!statusVal.isEmpty()
                    && !statusVal.equalsIgnoreCase(safe(b.getStatus()))) {
                continue;
            }

            // Date — exact match on parsedAt date
            if (filterDate != null) {
                if (b.getParsedAt() == null) continue;
                if (!b.getParsedAt().toLocalDate().equals(filterDate)) continue;
            }

            result.add(b);
        }

        return result;
    }

    private Listitem buildBatchRow(int idx, final InwardBatchDto b) {
        Listitem item = new Listitem();

        // S.No
        item.appendChild(new Listcell(String.valueOf(idx)));

        // Batch ID
        Listcell batchIdCell = new Listcell();
        Label batchIdLbl = new Label(safe(b.getBatchId()));
        batchIdLbl.setSclass("mono fw6");
        batchIdCell.appendChild(batchIdLbl);
        item.appendChild(batchIdCell);

        // File Name
        item.appendChild(new Listcell(safe(b.getSourceFileName())));

        // Total Cheques
        Listcell totalCell = new Listcell(String.valueOf(b.getTotalCheques()));
        totalCell.setStyle("text-align:center");
        item.appendChild(totalCell);

        // MICR Errors
        Listcell micrCell = new Listcell();
        micrCell.setStyle("text-align:center");
        Label micrLbl = new Label(String.valueOf(b.getMicrErrorCount()));
        micrLbl.setSclass(b.getMicrErrorCount() > 0 ? "txt-warn fw6" : "txt-success");
        micrCell.appendChild(micrLbl);
        item.appendChild(micrCell);

        // Parsed At
        item.appendChild(new Listcell(
                b.getParsedAt() != null ? b.getParsedAt().format(FMT) : "—"));

        // Status badge
        Listcell statusCell = new Listcell();
        statusCell.setStyle("text-align:center");
        Label statusBadge = new Label(getStatusLabel(b.getStatus()));
        statusBadge.setSclass(getStatusBadgeClass(b.getStatus()));
        statusCell.appendChild(statusBadge);
        item.appendChild(statusCell);

        // Action button
        Listcell actionCell = new Listcell();
        actionCell.setStyle("text-align:center");
        Button viewBtn = new Button("View");
        viewBtn.setSclass("btn bo btn-sm");
        viewBtn.addEventListener(Events.ON_CLICK,
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event e) {
                        openChequeView(b);
                    }
                });
        actionCell.appendChild(viewBtn);
        item.appendChild(actionCell);

        return item;
    }

    // =====================================================================
    // SECTION 2 — CHEQUE SPLIT VIEW
    // =====================================================================

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
        renderChequeTableWithSearch(safe(chqSearchBox.getValue()));
    }

    private void renderChequeTableWithSearch(String rawSearch) {
        if (batchCheques == null) return;

        List<InwardChequeDto> filtered = applyChequeFilters(rawSearch);
        chequeCountBadge.setValue(filtered.size() + " cheques");
        chequeListbox.getItems().clear();

        if (filtered.isEmpty()) {
            Listitem empty = new Listitem();
            Listcell emptyCell = new Listcell("No cheques found.");
            emptyCell.setSpan(6);
            empty.appendChild(emptyCell);
            chequeListbox.appendChild(empty);
            return;
        }

        int idx = 1;
        for (InwardChequeDto c : filtered) {
            chequeListbox.appendChild(buildChequeRow(idx++, c));
        }
    }

    private List<InwardChequeDto> applyChequeFilters(String rawSearch) {
        String search     = rawSearch != null ? rawSearch.trim().toLowerCase() : "";
        String iqaFilter  = getSelected(iqaFilterBox);
        String micrFilter = getSelected(micrFilterBox);

        List<InwardChequeDto> result = new ArrayList<>();

        for (InwardChequeDto c : batchCheques) {

            // Search — cheque no. or bank code
            if (!search.isEmpty()) {
                boolean matches =
                        safe(c.getChequeNo()).toLowerCase().contains(search)
                     || safe(c.getBankCode()).toLowerCase().contains(search);
                if (!matches) continue;
            }

            // IQA filter
            if (!iqaFilter.isEmpty()
                    && !iqaFilter.equalsIgnoreCase(safe(c.getIqaStatus()))) {
                continue;
            }

            // MICR filter
            if (!micrFilter.isEmpty()) {
                boolean hasMicr = c.isMicrError();
                if ("MICR_ERROR".equals(micrFilter) && !hasMicr) continue;
                if ("NORMAL".equals(micrFilter)     &&  hasMicr) continue;
            }

            result.add(c);
        }

        return result;
    }

    private Listitem buildChequeRow(int idx, final InwardChequeDto cheque) {
        Listitem item = new Listitem();
        item.setStyle("cursor:pointer");

        // #
        item.appendChild(new Listcell(String.valueOf(idx)));

        // Cheque No.
        Listcell chqNoCell = new Listcell();
        Label chqNoLbl = new Label(safe(cheque.getChequeNo()));
        chqNoLbl.setSclass("mono fw6 txt-primary");
        chqNoCell.appendChild(chqNoLbl);
        item.appendChild(chqNoCell);

        // Bank
        item.appendChild(new Listcell(safe(cheque.getBankCode())));

        // Amount
        item.appendChild(new Listcell(
                cheque.getAmount() != null
                        ? "₹" + moneyFmt.format(cheque.getAmount()) : "—"));

        // IQA badge
        Listcell iqaCell = new Listcell();
        iqaCell.setStyle("text-align:center");
        String iqaStatus = safe(cheque.getIqaStatus());
        Label iqaBadge = new Label(iqaStatus.isEmpty() ? "—" : iqaStatus);
        iqaBadge.setSclass("FAIL".equalsIgnoreCase(iqaStatus)
                ? "badge b-fail" : "badge b-pass");
        iqaCell.appendChild(iqaBadge);
        item.appendChild(iqaCell);

        // MICR badge
        Listcell micrCell = new Listcell();
        micrCell.setStyle("text-align:center");
        boolean isMicr = cheque.isMicrError();
        Label micrBadge = new Label(isMicr ? "MICR Error" : "Normal");
        micrBadge.setSclass(isMicr ? "badge b-pend" : "badge b-pass");
        micrCell.appendChild(micrBadge);
        item.appendChild(micrCell);

        // Row click — open detail panel
        item.addEventListener(Events.ON_CLICK,
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event e) {
                        highlightRow(item);
                        buildDetailPanel(cheque);
                    }
                });

        return item;
    }

    private void highlightRow(Listitem selected) {
        for (Listitem li : chequeListbox.getItems()) {
            li.setStyle("cursor:pointer;background:transparent");
        }
        selected.setStyle("cursor:pointer;background:#E8F0FE");
    }

    @Listen("onClick = #backToBatchesBtn")
    public void onBackToBatches() {
        selectedBatch = null;
        batchCheques  = null;
        showSection("batch");
        renderBatchTable();
    }

    // =====================================================================
    // IMAGE TAB LISTENERS
    // =====================================================================

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

    // =====================================================================
    // DETAIL PANEL
    // =====================================================================

    private void buildDetailPanel(InwardChequeDto cheque) {
        boolean isMicr = cheque.isMicrError();
        detailStatusBadge.setValue(isMicr ? "MICR Error" : "Normal");
        detailStatusBadge.setSclass(isMicr ? "badge b-pend" : "badge b-pass");

        detailEmpty.setVisible(false);
        detailBody.getChildren().clear();
        detailBody.setVisible(true);

        loadChequeImages(cheque);

        detailBody.appendChild(buildKeyInfoSection(cheque));
        detailBody.appendChild(buildProcessingStatusSection(cheque));
        detailBody.appendChild(buildMicrRepairSection(cheque));
    }

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

    // =====================================================================
    // DETAIL SECTION BUILDERS
    // =====================================================================

    private Div buildKeyInfoSection(InwardChequeDto cheque) {
        String[][] rows = {
            { "Cheque No.",      safe(cheque.getChequeNo()),            "vv mono"  },
            { "Cheque Date",     str(cheque.getChequeDate()),           "vv"       },
            { "Amount (CXF)",
              cheque.getAmount() != null
              ? "₹" + moneyFmt.format(cheque.getAmount()) : "—",        "vv fw6"   },
            { "Amount in Words", safe(cheque.getAmountInWords()),       "vvn"      },
            { "Amount (OCR)",
              cheque.getAmountOcr() != null
              ? "₹" + moneyFmt.format(cheque.getAmountOcr()) : "—",     "vv"       },
            { "Payee Name",      safe(cheque.getPayeeName()),           "vvn"      },
            { "Bank Code",       safe(cheque.getBankCode()),            "vv"       },
            { "Branch Code",     safe(cheque.getBranchCode()),          "vv mono"  },
            { "City Code",       safe(cheque.getCityCode()),            "vv"       },
            { "MICR Code (Raw)", safe(cheque.getMicrCodeRaw()),         "vv mono"  },
            { "Drawee Acc. No.", safe(cheque.getDraweeAccountNumber()), "vv mono"  },
            { "Drawee Holder",   safe(cheque.getDraweeAccountHolder()), "vvn"      },
            { "Presenting Bank", safe(cheque.getPresentingBankName()),  "vvn"      },
        };
        return buildVsSection("Key Cheque Information", rows);
    }

    private Div buildProcessingStatusSection(InwardChequeDto cheque) {
        Div vs = new Div();
        vs.setSclass("vs");
        vs.appendChild(buildVsTitle("Processing Status"));

        String batchStatus = selectedBatch != null ? selectedBatch.getStatus() : "—";
        vs.appendChild(buildVrBadge("Batch Status",
                getStatusLabel(batchStatus), getStatusBadgeClass(batchStatus)));
        vs.appendChild(buildVrBadge("Cheque Status",
                getStatusLabel(cheque.getStatus()), getStatusBadgeClass(cheque.getStatus())));

        String iqa = safe(cheque.getIqaStatus());
        vs.appendChild(buildVrBadge("IQA",
                iqa.isEmpty() ? "—" : iqa,
                "FAIL".equalsIgnoreCase(iqa) ? "badge b-fail" : "badge b-pass"));

        Boolean acctValid = cheque.getIsAccountValid();
        vs.appendChild(buildVrBadge("Account Valid",
                acctValid == null ? "—" : (acctValid ? "Yes" : "No"),
                Boolean.TRUE.equals(acctValid) ? "badge b-pass" : "badge b-fail"));

        Boolean bankMatch = cheque.getIsBankMatched();
        vs.appendChild(buildVrBadge("Bank Matched",
                bankMatch == null ? "—" : (bankMatch ? "Yes" : "No"),
                Boolean.TRUE.equals(bankMatch) ? "badge b-pass" : "badge b-fail"));

        return vs;
    }

    private Div buildMicrRepairSection(InwardChequeDto cheque) {
        Div vs = new Div();
        vs.setSclass("vs");
        vs.appendChild(buildVsTitle("MICR Repair Status"));

        boolean wasRepaired = "REPAIRED".equalsIgnoreCase(cheque.getRepairStatus());

        if (wasRepaired) {
            vs.appendChild(buildNote("MICR fields were repaired by Maker", "note-warn", "note-warn-txt"));
            vs.appendChild(buildVrWithSclass("Original MICR",  safe(cheque.getMicrCodeRaw()),       "vv mono txt-warn"));
            vs.appendChild(buildVrWithSclass("Corrected MICR", safe(cheque.getMicrCodeCorrected()), "vv mono txt-success"));
        } else if ("NEEDS_REPAIR".equalsIgnoreCase(cheque.getRepairStatus())) {
            vs.appendChild(buildNote("MICR error — awaiting repair", "note-warn", "note-warn-txt"));
            vs.appendChild(buildVrWithSclass("Raw MICR", safe(cheque.getMicrCodeRaw()), "vv mono txt-warn"));
        } else {
            vs.appendChild(buildNote("No MICR repair required", "note-suc", "note-suc-txt"));
        }

        return vs;
    }

    // =====================================================================
    // COMPONENT BUILDER HELPERS
    // =====================================================================

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

    private Div buildNote(String text, String divSclass, String lblSclass) {
        Div note = new Div();
        note.setSclass(divSclass);
        Label lbl = new Label(text);
        lbl.setSclass(lblSclass);
        note.appendChild(lbl);
        return note;
    }

    // =====================================================================
    // STATUS HELPERS
    // =====================================================================

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

    // =====================================================================
    // GENERAL HELPERS
    // =====================================================================

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