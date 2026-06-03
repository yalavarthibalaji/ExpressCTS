package com.iispl.composer;

import java.util.Date;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.CheckerInwardVerificationService;
import com.iispl.serviceImpl.CheckerInwardVerificationServiceImpl;
import com.iispl.util.SessionUtil;

public class CheckerInwardVerificationComposer extends SelectorComposer<Component> {

	// ── Pending Batches wires ─────────────────────────────────────────────────
	@Wire
	private Textbox txtSearchPending;

	@Wire
	private Rows rowsPending;

	// ── Cleared Batches wires ─────────────────────────────────────────────────
	@Wire
	private Textbox txtSearchCleared;

	@Wire
	private Datebox dtFromCleared;

	@Wire
	private Datebox dtToCleared;

	@Wire
	private Rows rowsCleared;

	// ── Failed / Returned wires ───────────────────────────────────────────────
	@Wire
	private Textbox txtSearchFailed;

	@Wire
	private Datebox dtFromFailed;

	@Wire
	private Datebox dtToFailed;

	@Wire
	private Textbox txtBatchIdFilter;

	@Wire
	private Rows rowsFailed;

	@Wire
	private Label userAvatar;

	@Wire
	private Label userName;

	@Wire
	private Label userRole;

	private CheckerInwardVerificationService checkerInwardVerificationService = new CheckerInwardVerificationServiceImpl();

//	@Override
//	public void doAfterCompose(Component comp) throws Exception {
//		super.doAfterCompose(comp);
//		// User loggedInUser = (User)
//		// Sessions.getCurrent().getAttribute("loggedInUser");
//		LoginDTO loggedInUser = (LoginDTO) Sessions.getCurrent().getAttribute(SessionUtil.SESSION_KEY);
//		loadPendingBatches("");
//
//		if (loggedInUser != null) {
//			userAvatar.setValue(String.valueOf(loggedInUser.getUserLoginId().charAt(0)).toUpperCase());
//			userName.setValue(loggedInUser.getFullName());
//			userRole.setValue(loggedInUser.getRoleCode());
//		}
//	}
	
	@Override
	public void doAfterCompose(Component comp) throws Exception {
	    super.doAfterCompose(comp);

	    loadPendingBatches("");
	}

	public void onTabChange() {
	}

	public void onSearchPending() {
		loadPendingBatches(txtSearchPending.getValue().trim());
	}

	private void loadPendingBatches(String keyword) {
		rowsPending.getChildren().clear();

		List<InwardBatch> batches = checkerInwardVerificationService.getPendingBatches(keyword);

		for (InwardBatch batch : batches) {
			Row row = new Row();

			// Batch ID — bold
			Label batchIdLabel = new Label(batch.getBatchId());
			batchIdLabel.setSclass("ci-bold-cell");

			// Status badge
			Label statusBadge = new Label(batch.getStatus());
			statusBadge.setSclass(getStatusSclass(batch.getStatus()));

			// Process button
			Button btnProcess = new Button("Process");
			btnProcess.setSclass("ci-btn-process");
			btnProcess.addEventListener("onClick", event -> onProcessBatch(batch.getBatchId()));

			// Presenting banks — derived from cheques list
			String presentingBanks = getPresentingBanks(batch);

			row.appendChild(batchIdLabel);
			row.appendChild(new Label(String.valueOf(batch.getTotalCheques())));
			row.appendChild(new Label(String.valueOf(batch.getMicrErrorCount())));
			row.appendChild(new Label(presentingBanks));
			row.appendChild(statusBadge);
			row.appendChild(btnProcess);

			rowsPending.appendChild(row);
		}
	}

	public void onSearchCleared() {
		Date fromDate = dtFromCleared.getValue();
		Date toDate = dtToCleared.getValue();
		loadClearedBatches(txtSearchCleared.getValue().trim(), fromDate, toDate);
	}

	private void loadClearedBatches(String keyword, Date from, Date to) {
		rowsCleared.getChildren().clear();

		List<InwardBatch> batches = checkerInwardVerificationService.getClearedBatches(keyword, from, to);

		for (InwardBatch batch : batches) {
			Row row = new Row();

			// Accepted and returned counts — computed from checkerActions
			int acceptedCount = countActions(batch, "ACCEPTED");
			int returnedCount = countActions(batch, "RETURNED");

			// Cleared by — checker who took the last action on this batch
			String clearedBy = getLastCheckerName(batch);
			String clearedAt = batch.getUpdatedAt() != null ? batch.getUpdatedAt().toString() : "—";

			row.appendChild(new Label(batch.getBatchId()));
			row.appendChild(new Label(String.valueOf(batch.getTotalCheques())));
			row.appendChild(new Label(String.valueOf(acceptedCount)));
			row.appendChild(new Label(String.valueOf(returnedCount)));
			row.appendChild(new Label(clearedBy));
			row.appendChild(new Label(clearedAt));

			rowsCleared.appendChild(row);
		}
	}

	public void onSearchFailed() {
		Date fromDate = dtFromFailed.getValue();
		Date toDate = dtToFailed.getValue();
		String batchId = txtBatchIdFilter.getValue().trim();
		loadFailedCheques(txtSearchFailed.getValue().trim(), fromDate, toDate, batchId);
	}

	private void loadFailedCheques(String keyword, Date from, Date to, String batchId) {
		rowsFailed.getChildren().clear();

		List<InwardCheque> cheques = checkerInwardVerificationService.getReturnedCheques(keyword, from, to, batchId);

		for (InwardCheque cheque : cheques) {
			Row row = new Row();

			// batchId lives inside the batch object — not directly on cheque
			String batchIdValue = cheque.getBatch() != null ? cheque.getBatch().getBatchId() : "—";

			// returnReason, returnedBy, returnTime — all live in InwardCheckerAction
			InwardCheckerAction lastAction = getLastCheckerAction(cheque);

			String returnReason = lastAction != null ? lastAction.getReasonText() : "—";
			String returnedBy = (lastAction != null && lastAction.getChecker() != null)
					? lastAction.getChecker().getUserLoginId()
					: "—";
			String returnTime = lastAction != null ? lastAction.getActionedAt().toString() : "—";

			row.appendChild(new Label(batchIdValue));
			row.appendChild(new Label(cheque.getChequeNo()));
			row.appendChild(new Label(cheque.getAmount().toPlainString()));
			row.appendChild(new Label(cheque.getPresentingBankName() != null ? cheque.getPresentingBankName() : "—"));
			row.appendChild(new Label(returnReason));
			row.appendChild(new Label(returnedBy));
			row.appendChild(new Label(returnTime));

			rowsFailed.appendChild(row);
		}
	}

	private void onProcessBatch(String batchId) {
		// TODO: Navigate to process batch detail page
		Messagebox.show("Processing batch: " + batchId, "Info", Messagebox.OK, Messagebox.INFORMATION);
	}

	private String getStatusSclass(String status) {
		if (status == null)
			return "ci-badge-default";
		switch (status.toUpperCase()) {
		case "RECEIVED":
			return "ci-badge-pending";
		case "CLEARED":
			return "ci-badge-cleared";
		case "RETURNED":
			return "ci-badge-returned";
		default:
			return "ci-badge-default";
		}
	}

	private String getPresentingBanks(InwardBatch batch) {
		if (batch.getCheques() == null || batch.getCheques().isEmpty()) {
			return "—";
		}

		StringBuilder banks = new StringBuilder();
		for (InwardCheque cheque : batch.getCheques()) {
			if (cheque.getPresentingBankName() != null) {
				String bankName = cheque.getPresentingBankName().trim();
				// Only add if not already in the string (avoid duplicates)
				if (banks.indexOf(bankName) == -1) {
					if (banks.length() > 0) {
						banks.append(", ");
					}
					banks.append(bankName);
				}
			}
		}

		return banks.length() > 0 ? banks.toString() : "—";
	}

	private int countActions(InwardBatch batch, String actionType) {
		if (batch.getCheckerActions() == null)
			return 0;

		int count = 0;
		for (InwardCheckerAction action : batch.getCheckerActions()) {
			if (actionType.equalsIgnoreCase(action.getAction())) {
				count++;
			}
		}
		return count;
	}

	private String getLastCheckerName(InwardBatch batch) {
		List<InwardCheckerAction> actions = batch.getCheckerActions();
		if (actions == null || actions.isEmpty())
			return "—";

		InwardCheckerAction lastAction = actions.get(actions.size() - 1);
		if (lastAction.getChecker() != null) {
			return lastAction.getChecker().getUserLoginId();
		}
		return "—";
	}

	// Gets the last checker action for a specific cheque (for return
	// reason/by/time)
	private InwardCheckerAction getLastCheckerAction(InwardCheque cheque) {
		List<InwardCheckerAction> actions = cheque.getCheckerActions();
		if (actions == null || actions.isEmpty())
			return null;
		return actions.get(actions.size() - 1);
	}
}