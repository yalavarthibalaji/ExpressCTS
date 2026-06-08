package com.iispl.composer;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.dto.InwardReportDTO;
import com.iispl.dto.LoginDTO;
import com.iispl.service.CheckerInwardReportsService;
import com.iispl.serviceImpl.CheckerInwardReportsServiceImpl;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/composer/CheckerInwardReportsComposer.java
 *
 * Validations added:
 *   1.  Session / login guard              — redirect if session expired
 *   2.  Role guard                         — CHECKER_INWARD only
 *   3.  Date range validation              — From Date cannot be after To Date
 *   4.  Future date prevention             — From/To Date cannot be in the future
 *   5.  Search term length validation      — batch search min 2 chars if provided
 *   6.  Empty result notification          — friendly toast when no rows match
 *   7.  Generate to Debit — eligibility    — button disabled for non-Verified batches
 *   8.  Generate to Debit — confirmation   — confirm dialog before processing
 *   9.  Generate to Debit — duplicate guard— service re-checks status = Verified
 *   10. Generate to Debit — error handling — meaningful error message + auto-refresh
 *   11. Service error isolation            — load failure shows error, does not crash
 *   12. Null-safe rendering               — null DTO fields never cause NPE
 */
public class CheckerInwardReportsComposer extends SelectorComposer<Component> {

    private static final Logger log =
            LoggerFactory.getLogger(CheckerInwardReportsComposer.class);

    private static final int PAGE_SIZE = 50;

    private static final NumberFormat AMOUNT_FMT =
            NumberFormat.getNumberInstance(new Locale("en", "IN"));

    static {
        AMOUNT_FMT.setMinimumFractionDigits(2);
        AMOUNT_FMT.setMaximumFractionDigits(2);
    }

    private final CheckerInwardReportsService reportsService =
            new CheckerInwardReportsServiceImpl();

    private List<InwardReportDTO> currentRows = new ArrayList<>();

    @Wire private Textbox  txtBatchSearch;
    @Wire private Datebox  dtFrom;
    @Wire private Datebox  dtTo;
    @Wire private Combobox cmbStatus;
    @Wire private Button   btnSearch;
    @Wire private Listbox  lstReports;

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        // VALIDATION 1 — Session guard
        LoginDTO user = SessionUtil.requireLogin();
        if (user == null) {
            Executions.sendRedirect("/login/login.zul");
            return;
        }

        // VALIDATION 2 — Role guard
        if (!"CHECKER_INWARD".equals(user.getRoleCode())) {
            Executions.sendRedirect(SessionUtil.getDashboardUrlFor(user.getRoleCode()));
            return;
        }

        populateStatusCombo();
        loadPage();

        log.info("CheckerInwardReportsComposer initialised for user '{}'",
                 user.getUserLoginId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Search button
    // ─────────────────────────────────────────────────────────────────────────

    @Listen("onClick = #btnSearch")
    public void onSearch() {

        // VALIDATION 5 — Batch search minimum length
        if (txtBatchSearch != null) {
            String val = txtBatchSearch.getValue();
            if (val != null && !val.trim().isEmpty() && val.trim().length() < 2) {
                Clients.showNotification(
                    "Search term must be at least 2 characters.",
                    "warning", txtBatchSearch, "after_end", 3000
                );
                return;
            }
        }

        loadPage();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core load
    // ─────────────────────────────────────────────────────────────────────────

    private void loadPage() {
        String batchSearch = (txtBatchSearch != null) ? txtBatchSearch.getValue() : null;
        Date   fromDate    = (dtFrom != null)         ? dtFrom.getValue()         : null;
        Date   toDate      = (dtTo   != null)         ? dtTo.getValue()           : null;
        String status      = getSelectedStatus();

        // VALIDATION 3 — Date range check
        if (fromDate != null && toDate != null && fromDate.after(toDate)) {
            Messagebox.show("From Date cannot be after To Date.",
                            "Validation Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        // VALIDATION 4 — Future date prevention
        Date today = new Date();
        if (fromDate != null && fromDate.after(today)) {
            Clients.showNotification(
                "From Date cannot be a future date.",
                "warning", dtFrom, "after_end", 3000
            );
            return;
        }
        if (toDate != null && toDate.after(today)) {
            Clients.showNotification(
                "To Date cannot be a future date.",
                "warning", dtTo, "after_end", 3000
            );
            return;
        }

        try {
            // VALIDATION 11 — Isolate service errors
            currentRows = reportsService.getReports(
                    batchSearch, fromDate, toDate, status, 1, PAGE_SIZE);

            renderRows(currentRows);

            // VALIDATION 6 — Empty result notification
            if (currentRows.isEmpty()) {
                Clients.showNotification(
                    "No batches found matching the selected filters.",
                    "info", null, "top_center", 3000
                );
            }

            log.info("loadPage — rows={}", currentRows.size());

        } catch (IllegalArgumentException e) {
            Messagebox.show(e.getMessage(),
                            "Validation Error", Messagebox.OK, Messagebox.ERROR);
        } catch (Exception e) {
            log.error("loadPage error: {}", e.getMessage(), e);
            Messagebox.show("An error occurred while loading reports. Please try again.",
                            "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Grid rendering
    // ─────────────────────────────────────────────────────────────────────────

    private void renderRows(List<InwardReportDTO> rows) {
        if (lstReports == null) return;
        lstReports.getItems().clear();

        if (rows == null || rows.isEmpty()) return;

        for (InwardReportDTO dto : rows) {

            // VALIDATION 12 — Skip null DTO entries
            if (dto == null) continue;

            Listitem item = new Listitem();

            // Col 1: Batch ID
            item.appendChild(new Listcell(nullSafe(dto.getBatchId())));

            // Col 2: Date
            item.appendChild(new Listcell(nullSafe(dto.getBatchDate())));

            // Col 3: Total Cheques
            item.appendChild(new Listcell(String.valueOf(dto.getTotalCheques())));

            // Col 4: Total Amount
            BigDecimal amt = dto.getTotalAmount();
            String amtStr  = (amt != null) ? "₹ " + AMOUNT_FMT.format(amt) : "₹ 0.00";
            item.appendChild(new Listcell(amtStr));

            // Col 5: Status badge
            Listcell statusCell = new Listcell();
            Label statusLabel   = new Label(nullSafe(dto.getStatus()));
            statusLabel.setSclass(resolveStatusBadgeClass(dto.getStatus()));
            statusCell.appendChild(statusLabel);
            item.appendChild(statusCell);

            // Col 6: Action
            // VALIDATION 7 — Button disabled for non-eligible batches
            Listcell actionCell = new Listcell();
            Button btnDebit     = new Button("Generate to Debit");
            btnDebit.setSclass("btn bp" +
                               (dto.isDebitEligible() ? "" : " disabled"));
            btnDebit.setDisabled(!dto.isDebitEligible());
            btnDebit.setTooltiptext(
                dto.isDebitEligible()
                    ? "Click to generate debit entries for this batch"
                    : "Batch is not eligible. Only Pending (Verified) batches can be processed."
            );

            if (dto.isDebitEligible()) {
                final String batchId = dto.getBatchId();
                btnDebit.addEventListener("onClick", event -> onGenerateToDebit(batchId));
            }

            actionCell.appendChild(btnDebit);
            item.appendChild(actionCell);

            lstReports.appendChild(item);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Generate to Debit
    // ─────────────────────────────────────────────────────────────────────────

    // VALIDATION 8 — Confirmation dialog before processing
    private void onGenerateToDebit(String batchId) {

        // VALIDATION 12 — Null batch ID guard
        if (batchId == null || batchId.trim().isEmpty()) {
            Clients.showNotification("Invalid batch ID.", "error", null, "top_center", 3000);
            return;
        }

        Messagebox.show(
            "Are you sure you want to generate debit entries for batch:\n" + batchId + "?\n\n"
            + "This action cannot be undone.",
            "Confirm Generate to Debit",
            Messagebox.YES | Messagebox.NO,
            Messagebox.QUESTION,
            event -> {
                if (Messagebox.ON_YES.equals(event.getName())) {
                    executeDebit(batchId);
                }
            }
        );
    }

    private void executeDebit(String batchId) {
        try {
            // VALIDATION 9 — Duplicate guard handled inside service
            // (service re-confirms status = Verified before processing)
            reportsService.generateToDebit(batchId);

            Clients.showNotification(
                "Debit generation completed successfully for batch: " + batchId,
                "info", null, "top_center", 4000
            );
            log.info("executeDebit — batch '{}' completed", batchId);

            // Auto-refresh so the status updates to Completed and button disables
            loadPage();

        } catch (IllegalArgumentException e) {
            // VALIDATION 9 — Duplicate / ineligible batch caught here
            Messagebox.show(e.getMessage(),
                            "Cannot Process", Messagebox.OK, Messagebox.EXCLAMATION);
            log.warn("executeDebit — validation error for '{}': {}", batchId, e.getMessage());
            loadPage();

        } catch (Exception e) {
            // VALIDATION 10 — Meaningful error message on failure
            log.error("executeDebit — error for '{}': {}", batchId, e.getMessage(), e);
            Messagebox.show(
                "Debit generation failed for batch '" + batchId + "'.\n"
                + "The batch remains in Pending state and can be retried.\n\n"
                + "Error: " + e.getMessage(),
                "Processing Error", Messagebox.OK, Messagebox.ERROR
            );
            loadPage();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void populateStatusCombo() {
        if (cmbStatus == null) return;
        cmbStatus.getChildren().clear();

        String[][] items = {
            { "All Status", "ALL"          },
            { "Pending",    "Verified"      },
            { "Completed",  "CBS_Processed" },
            { "Failed",     "Rejected"      }
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

    private String resolveStatusBadgeClass(String displayStatus) {
        if (displayStatus == null) return "badge b-grey";
        switch (displayStatus) {
            case "Pending":   return "badge b-pend";
            case "Completed": return "badge b-pass";
            case "Failed":    return "badge b-fail";
            default:          return "badge b-grey";
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "—";
    }
}