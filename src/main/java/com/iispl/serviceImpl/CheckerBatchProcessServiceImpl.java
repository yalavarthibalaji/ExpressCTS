package com.iispl.serviceImpl;

import java.util.List;

import com.iispl.dao.CheckerBatchProcessDao;
import com.iispl.daoImpl.CheckerBatchProcessDaoImpl;
import com.iispl.entity.User;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.CheckerBatchProcessService;
import com.iispl.util.InwardReturnReason;

public class CheckerBatchProcessServiceImpl implements CheckerBatchProcessService {

	// ── DAO Dependency ────────────────────────────────────────────────────────
	private CheckerBatchProcessDao checkerBatchProcessDao = new CheckerBatchProcessDaoImpl();

	// ── Load Batch ────────────────────────────────────────────────────────────

	@Override
	public InwardBatch loadBatchForProcessing(String batchId) {

		if (batchId == null || batchId.trim().isEmpty()) {
			throw new IllegalArgumentException("Batch ID cannot be empty.");
		}

		InwardBatch batch = checkerBatchProcessDao.findBatchWithCheques(batchId);

		if (batch == null) {
			throw new IllegalArgumentException("No batch found with ID: " + batchId);
		}

		return batch;
	}

	// ── Submit Batch ──────────────────────────────────────────────────────────

	@Override
	public void submitBatch(InwardBatch batch, List<InwardCheckerAction> actions, User checker) {

		if (batch == null) {
			throw new IllegalArgumentException("Batch cannot be null.");
		}

		if (actions == null || actions.isEmpty()) {
			throw new IllegalArgumentException("No actions provided. Please action all cheques before submitting.");
		}

		if (checker == null) {
			throw new IllegalArgumentException("Checker information is missing. Please log in again.");
		}

		List<InwardCheque> cheques = batch.getCheques();

		if (cheques == null || cheques.isEmpty()) {
			throw new IllegalArgumentException("This batch has no cheques to process.");
		}

		if (actions.size() != cheques.size()) {
			throw new IllegalArgumentException("All cheques must be actioned before submitting. " + "Expected: "
					+ cheques.size() + ", Actioned: " + actions.size());
		}

		// Validate action types
		for (InwardCheckerAction action : actions) {
			String actionType = action.getAction();

			if (actionType == null || actionType.trim().isEmpty()) {
				throw new IllegalArgumentException("One or more cheques have no action selected. "
						+ "Please select Accept, Return, or Send Back for every cheque.");
			}

			if (!isValidActionType(actionType)) {
				throw new IllegalArgumentException(
						"Invalid action type: " + actionType + ". Allowed values are ACCEPTED, RETURNED, SEND_BACK.");
			}
		}

		// Validate return reasons
		for (InwardCheckerAction action : actions) {
			if ("RETURNED".equalsIgnoreCase(action.getAction())) {

				if (action.getReasonCode() == null || action.getReasonCode().trim().isEmpty()) {
					throw new IllegalArgumentException("A return reason must be selected for all returned cheques. "
							+ "Cheque: " + getChequeNo(action));
				}

				if (action.getReasonText() == null || action.getReasonText().trim().isEmpty()) {
					String reasonText = InwardReturnReason.getReasonText(action.getReasonCode());
					action.setReasonText(reasonText);
				}
			}
		}

		// Set checker and batch reference on every action
		for (InwardCheckerAction action : actions) {
			action.setChecker(checker);
			action.setInwardBatch(batch);
		}

		// Persist to DB
		checkerBatchProcessDao.saveCheckerActions(actions);
		checkerBatchProcessDao.updateBatchStatus(batch);
	}

	// ── Private Helpers ───────────────────────────────────────────────────────

	private boolean isValidActionType(String action) {
		return "ACCEPTED".equalsIgnoreCase(action) || "RETURNED".equalsIgnoreCase(action)
				|| "SEND_BACK".equalsIgnoreCase(action);
	}

	private String getChequeNo(InwardCheckerAction action) {
		if (action.getInwardCheque() != null && action.getInwardCheque().getChequeNo() != null) {
			return action.getInwardCheque().getChequeNo();
		}
		return "Unknown";
	}
}