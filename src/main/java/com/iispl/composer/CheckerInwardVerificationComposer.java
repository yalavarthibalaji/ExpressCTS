package com.iispl.composer;

import java.util.Date;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.CheckerInwardVerificationService;
import com.iispl.serviceImpl.CheckerInwardVerificationServiceImpl;
import com.iispl.util.SessionUtil;

public class CheckerInwardVerificationComposer extends SelectorComposer<Component> {

    // ── Tab 1: Pending Batches ────────────────────────────────────────────────
    @Wire private Textbox  txtSearchPending;
    @Wire private Listbox  lstPending;

    // ── Tab 2: Cleared Batches ────────────────────────────────────────────────
    @Wire private Textbox  txtSearchCleared;
    @Wire private Datebox  dtFromCleared;
    @Wire private Datebox  dtToCleared;
    @Wire private Listbox  lstCleared;

    // ── Tab 3: Failed / Returned ──────────────────────────────────────────────
    @Wire private Textbox  txtSearchFailed;
    @Wire private Datebox  dtFromFailed;
    @Wire private Datebox  dtToFailed;
    @Wire private Textbox  txtBatchIdFilter;
    @Wire private Listbox  lstFailed;

    private final CheckerInwardVerificationService verificationService =
            new CheckerInwardVerificationServiceImpl();

    // ─────────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        // Session guard — only CHECKER_INWARD can access this page
        LoginDTO user = SessionUtil.requireLogin();
        if (user == null) return;

        // Null-check wired components — if @Wire fails the field stays null.
        // This gives a clear log message instead of a silent blank page.
        if (lstPending == null || lstCleared == null || lstFailed == null) {
            System.err.println(
                "[CheckerInwardVerificationComposer] WIRE FAILED — " +
                "lstPending / lstCleared / lstFailed not found in ZUL. " +
                "Check IDs in inwardCheckerVerification.zul."
            );
            return;
        }

        // Load all three tabs on page open
        loadPendingBatches("");
        loadClearedBatches("", null, null);
        loadFailedCheques("", null, null, "");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EVENT HANDLERS — called from ZUL onChanging / onChange
    // ─────────────────────────────────────────────────────────────────────────

    public void onSearchPending() {
        String keyword = txtSearchPending != null
                ? txtSearchPending.getValue().trim() : "";
        loadPendingBatches(keyword);
    }

    public void onSearchCleared() {
        String keyword = txtSearchCleared != null
                ? txtSearchCleared.getValue().trim() : "";
        Date from = dtFromCleared != null ? dtFromCleared.getValue() : null;
        Date to   = dtToCleared   != null ? dtToCleared.getValue()   : null;
        loadClearedBatches(keyword, from, to);
    }

    public void onSearchFailed() {
        String keyword  = txtSearchFailed  != null ? txtSearchFailed.getValue().trim()  : "";
        String batchId  = txtBatchIdFilter != null ? txtBatchIdFilter.getValue().trim() : "";
        Date   from     = dtFromFailed     != null ? dtFromFailed.getValue()             : null;
        Date   to       = dtToFailed       != null ? dtToFailed.getValue()               : null;
        loadFailedCheques(keyword, from, to, batchId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD: Tab 1 — Pending Batches
    // Columns: BATCH ID | COUNT | MICR ERRORS | PRESENTING BANKS | STATUS | ACTION
    // ─────────────────────────────────────────────────────────────────────────

    private void loadPendingBatches(String keyword) {
        lstPending.getItems().clear();

        List<InwardBatch> batches;

        try {
            batches = verificationService.getPendingBatches(keyword);
        } catch (Exception e) {
            System.err.println("[loadPendingBatches] Error: " + e.getMessage());
            e.printStackTrace();
            Messagebox.show(
                "Failed to load pending batches:\n" + e.getMessage(),
                "Error", Messagebox.OK, Messagebox.ERROR
            );
            return;
        }

        if (batches.isEmpty()) {
            // emptyMessage on listbox handles the empty state — nothing to do
            return;
        }

        for (InwardBatch batch : batches) {

            Listitem item = new Listitem();

            // Cell 1 — Batch ID (bold mono style)
            Listcell cellBatchId = new Listcell();
            Label lblBatchId = new Label(batch.getBatchId());
            lblBatchId.setSclass("ci-bold-cell");
            cellBatchId.appendChild(lblBatchId);

            // Cell 2 — Total cheque count
            Listcell cellCount = new Listcell(
                String.valueOf(batch.getTotalCheques())
            );

            // Cell 3 — MICR error count
            Listcell cellMicr = new Listcell(
                String.valueOf(batch.getMicrErrorCount())
            );

            // Cell 4 — Presenting banks (comma-separated, deduplicated)
            Listcell cellBanks = new Listcell(
                getPresentingBanks(batch)
            );

            // Cell 5 — Status badge
            Listcell cellStatus = new Listcell();
            Label lblStatus = new Label(batch.getStatus());
            lblStatus.setSclass(resolveStatusBadge(batch.getStatus()));
            cellStatus.appendChild(lblStatus);

            // Cell 6 — Process button
            Listcell cellAction = new Listcell();
            Button btnProcess = new Button("Process");
            btnProcess.setSclass("btn bp");
            btnProcess.addEventListener("onClick",
                event -> onProcessBatch(batch.getBatchId())
            );
            cellAction.appendChild(btnProcess);

            // Assemble row
            item.appendChild(cellBatchId);
            item.appendChild(cellCount);
            item.appendChild(cellMicr);
            item.appendChild(cellBanks);
            item.appendChild(cellStatus);
            item.appendChild(cellAction);

            lstPending.appendChild(item);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD: Tab 2 — Cleared Batches
    // Columns: BATCH ID | TOTAL | ACCEPTED | RETURNED | CLEARED BY | CLEARED AT
    // ─────────────────────────────────────────────────────────────────────────

    private void loadClearedBatches(String keyword, Date from, Date to) {
        lstCleared.getItems().clear();

        List<InwardBatch> batches;

        try {
            batches = verificationService.getClearedBatches(keyword, from, to);
        } catch (Exception e) {
            System.err.println("[loadClearedBatches] Error: " + e.getMessage());
            e.printStackTrace();
            Messagebox.show(
                "Failed to load cleared batches:\n" + e.getMessage(),
                "Error", Messagebox.OK, Messagebox.ERROR
            );
            return;
        }

        if (batches.isEmpty()) return;

        for (InwardBatch batch : batches) {

            Listitem item = new Listitem();

            int acceptedCount = countActions(batch, "ACCEPTED");
            int returnedCount = countActions(batch, "RETURNED");
            String clearedBy  = getLastCheckerName(batch);

            // updatedAt can be null — fall back to createdAt.
            // Show date only (dd/MM/yyyy) — no time needed.
            String clearedAt = "—";
            if (batch.getUpdatedAt() != null) {
                clearedAt = formatDateOnly(batch.getUpdatedAt().toLocalDate().toString());
            } else if (batch.getCreatedAt() != null) {
                clearedAt = formatDateOnly(batch.getCreatedAt().toLocalDate().toString());
            }

            // Cell 1 — Batch ID
            Listcell cellBatchId = new Listcell();
            Label lblBatchId = new Label(batch.getBatchId());
            lblBatchId.setSclass("ci-bold-cell");
            cellBatchId.appendChild(lblBatchId);

            // Cell 2 — Total
            Listcell cellTotal = new Listcell(
                String.valueOf(batch.getTotalCheques())
            );

            // Cell 3 — Accepted count (green badge)
            Listcell cellAccepted = new Listcell();
            Label lblAccepted = new Label(String.valueOf(acceptedCount));
            lblAccepted.setSclass("badge b-pass");
            cellAccepted.appendChild(lblAccepted);

            // Cell 4 — Returned count (red badge)
            Listcell cellReturned = new Listcell();
            Label lblReturned = new Label(String.valueOf(returnedCount));
            lblReturned.setSclass(returnedCount > 0 ? "badge b-fail" : "badge b-grey");
            cellReturned.appendChild(lblReturned);

            // Cell 5 — Cleared by
            Listcell cellClearedBy = new Listcell(clearedBy);

            // Cell 6 — Cleared at
            Listcell cellClearedAt = new Listcell(clearedAt);

            // Assemble row
            item.appendChild(cellBatchId);
            item.appendChild(cellTotal);
            item.appendChild(cellAccepted);
            item.appendChild(cellReturned);
            item.appendChild(cellClearedBy);
            item.appendChild(cellClearedAt);

            lstCleared.appendChild(item);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD: Tab 3 — Failed / Returned Cheques
    // Columns: BATCH ID | CHEQUE NO | AMOUNT | PRESENTING BANK |
    //          RETURN REASON | RETURNED BY | TIME
    // ─────────────────────────────────────────────────────────────────────────

    private void loadFailedCheques(String keyword, Date from, Date to, String batchId) {
        lstFailed.getItems().clear();

        List<InwardCheque> cheques;

        try {
            cheques = verificationService.getReturnedCheques(keyword, from, to, batchId);
        } catch (Exception e) {
            System.err.println("[loadFailedCheques] Error: " + e.getMessage());
            e.printStackTrace();
            Messagebox.show(
                "Failed to load returned cheques:\n" + e.getMessage(),
                "Error", Messagebox.OK, Messagebox.ERROR
            );
            return;
        }

        if (cheques.isEmpty()) return;

        for (InwardCheque cheque : cheques) {

            Listitem item = new Listitem();

            // Resolve batch ID safely
            String batchIdValue = (cheque.getBatch() != null)
                    ? cheque.getBatch().getBatchId() : "—";

            // Resolve last checker action safely
            InwardCheckerAction lastAction = getLastCheckerAction(cheque);
            String returnReason = (lastAction != null && lastAction.getReasonText() != null)
                    ? lastAction.getReasonText() : "—";
            String returnedBy   = (lastAction != null && lastAction.getChecker() != null)
                    ? lastAction.getChecker().getUserLoginId() : "—";
            // Show date only (dd/MM/yyyy) — no time needed
            String returnTime   = (lastAction != null && lastAction.getActionedAt() != null)
                    ? formatDateOnly(lastAction.getActionedAt().toLocalDate().toString()) : "—";
            String bankName     = (cheque.getPresentingBankName() != null)
                    ? cheque.getPresentingBankName() : "—";
            String amount       = (cheque.getAmount() != null)
                    ? "₹ " + cheque.getAmount().toPlainString() : "—";

            // Cell 1 — Batch ID
            Listcell cellBatchId = new Listcell();
            Label lblBatchId = new Label(batchIdValue);
            lblBatchId.setSclass("ci-bold-cell");
            cellBatchId.appendChild(lblBatchId);

            // Cell 2 — Cheque No
            Listcell cellChequeNo = new Listcell(cheque.getChequeNo());

            // Cell 3 — Amount
            Listcell cellAmount = new Listcell(amount);

            // Cell 4 — Presenting bank
            Listcell cellBank = new Listcell(bankName);

            // Cell 5 — Return reason
            Listcell cellReason = new Listcell(returnReason);

            // Cell 6 — Returned by
            Listcell cellReturnedBy = new Listcell(returnedBy);

            // Cell 7 — Time
            Listcell cellTime = new Listcell(returnTime);

            // Assemble row
            item.appendChild(cellBatchId);
            item.appendChild(cellChequeNo);
            item.appendChild(cellAmount);
            item.appendChild(cellBank);
            item.appendChild(cellReason);
            item.appendChild(cellReturnedBy);
            item.appendChild(cellTime);

            lstFailed.appendChild(item);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NAVIGATION — go to processBatch page
    // ─────────────────────────────────────────────────────────────────────────

    private void onProcessBatch(String batchId) {
        Sessions.getCurrent().setAttribute("selectedBatchId", batchId);
        org.zkoss.zk.ui.Executions.getCurrent()
            .sendRedirect("/inward/inwardChecker/processBatch.zul");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Maps batch status string to the correct badge CSS class.
     * Uses the unified badge system (badge b-*) from inward-reports.css.
     */
    private String resolveStatusBadge(String status) {
        if (status == null) return "badge b-grey";
        switch (status.toUpperCase()) {
            case "RECEIVED":
            case "PENDING":
                return "badge b-pend";
            case "CLEARED":
            case "ACCEPTED":
            case "CBS_PROCESSED":
                return "badge b-pass";
            case "RETURNED":
            case "REJECTED":
                return "badge b-fail";
            case "PROCESSING":
                return "badge b-info";
            default:
                return "badge b-grey";
        }
    }

    /**
     * Builds a deduplicated comma-separated list of presenting bank names
     * from all cheques in the batch.
     */
    private String getPresentingBanks(InwardBatch batch) {
        if (batch.getCheques() == null || batch.getCheques().isEmpty()) return "—";

        StringBuilder banks = new StringBuilder();
        for (InwardCheque cheque : batch.getCheques()) {
            if (cheque.getPresentingBankName() != null) {
                String name = cheque.getPresentingBankName().trim();
                if (banks.indexOf(name) == -1) {
                    if (banks.length() > 0) banks.append(", ");
                    banks.append(name);
                }
            }
        }
        return banks.length() > 0 ? banks.toString() : "—";
    }

    /**
     * Counts how many checker actions of a given type exist on a batch.
     * actionType: "ACCEPTED" or "RETURNED"
     */
    private int countActions(InwardBatch batch, String actionType) {
        if (batch.getCheckerActions() == null) return 0;
        int count = 0;
        for (InwardCheckerAction action : batch.getCheckerActions()) {
            if (actionType.equalsIgnoreCase(action.getAction())) count++;
        }
        return count;
    }

    /**
     * Returns the userLoginId of the last checker who acted on the batch.
     */
    private String getLastCheckerName(InwardBatch batch) {
        List<InwardCheckerAction> actions = batch.getCheckerActions();
        if (actions == null || actions.isEmpty()) return "—";
        InwardCheckerAction last = actions.get(actions.size() - 1);
        return (last.getChecker() != null) ? last.getChecker().getUserLoginId() : "—";
    }

    /**
     * Returns the last InwardCheckerAction recorded on a cheque.
     * Returns null if no actions exist.
     */
    private InwardCheckerAction getLastCheckerAction(InwardCheque cheque) {
        List<InwardCheckerAction> actions = cheque.getCheckerActions();
        if (actions == null || actions.isEmpty()) return null;
        return actions.get(actions.size() - 1);
    }

    /**
     * Converts ISO date string "yyyy-MM-dd" → display format "dd/MM/yyyy".
     * Ensures only the date part is shown — no time component ever appears.
     */
    private String formatDateOnly(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return isoDate != null ? isoDate : "—";
        try {
            String[] parts = isoDate.split("-");
            if (parts.length == 3) return parts[2] + "/" + parts[1] + "/" + parts[0];
        } catch (Exception ignored) {}
        return isoDate;
    }
}