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

        if (batch == null)
            throw new IllegalArgumentException("Batch cannot be null.");
        if (actions == null || actions.isEmpty())
            throw new IllegalArgumentException("No actions provided. Please action all cheques before submitting.");
        if (checker == null)
            throw new IllegalArgumentException("Checker information is missing. Please log in again.");

        // ── REMOVED: count check against batch.getCheques() ──────────────────
        // batch.getCheques() is the original Hibernate-loaded list.
        // Its in-memory statuses get mutated by confirmReturn/confirmReferBack
        // in unpredictable ways across sessions, making the count unreliable.
        // The composer already manages the correct working list and only sends
        // actions for cheques that need to be submitted here.
        // ─────────────────────────────────────────────────────────────────────

        // Validate each action in the submitted list
        for (InwardCheckerAction action : actions) {
            String actionType = action.getAction();

            if (actionType == null || actionType.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "One or more cheques have no action selected. " +
                    "Please select Accept or Return for every cheque."
                );
            }

            if (!isValidSubmitActionType(actionType)) {
                throw new IllegalArgumentException(
                    "Invalid action type: " + actionType +
                    ". Allowed values are ACCEPTED, RETURNED."
                );
            }
        }

        // Validate return reasons for RETURNED cheques
        for (InwardCheckerAction action : actions) {
            if ("RETURNED".equalsIgnoreCase(action.getAction())) {

                if (action.getReasonCode() == null || action.getReasonCode().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        "A return reason must be selected for all returned cheques. " +
                        "Cheque: " + getChequeNo(action)
                    );
                }

                if (action.getReasonText() == null || action.getReasonText().trim().isEmpty()) {
                    action.setReasonText(InwardReturnReason.getReasonText(action.getReasonCode()));
                }
            }
        }

        // Set checker and batch reference on every action before saving
        for (InwardCheckerAction action : actions) {
            action.setChecker(checker);
            action.setInwardBatch(batch);
        }

        checkerBatchProcessDao.saveCheckerActions(actions);
        checkerBatchProcessDao.updateBatchStatus(batch);
    }

    // ── Confirm Return (per-cheque) ───────────────────────────────────────────

    @Override
    public void confirmReturn(InwardBatch batch, InwardCheque cheque,
                              String reasonCode, User checker) {

        if (batch == null) {
            throw new IllegalArgumentException("Batch cannot be null.");
        }
        if (cheque == null) {
            throw new IllegalArgumentException("Cheque cannot be null.");
        }
        if (checker == null) {
            throw new IllegalArgumentException("Checker information is missing. Please log in again.");
        }
        if (reasonCode == null || reasonCode.trim().isEmpty()) {
            throw new IllegalArgumentException("A return reason must be selected before confirming.");
        }

        String reasonText = InwardReturnReason.getReasonText(reasonCode.trim());

        InwardCheckerAction action = new InwardCheckerAction();
        action.setInwardCheque(cheque);
        action.setInwardBatch(batch);
        action.setChecker(checker);
        action.setAction("RETURNED");
        action.setReasonCode(reasonCode.trim());
        action.setReasonText(reasonText);

        checkerBatchProcessDao.saveReturnAction(action);

        // If every cheque in the batch is now actioned, mark batch as RETURNED
        List<InwardCheque> allCheques = batch.getCheques();
        if (allCheques != null && !allCheques.isEmpty()) {
            boolean allActioned = allCheques.stream()
                .allMatch(c -> c.getStatus() != null
                    && !c.getStatus().equalsIgnoreCase("RECEIVED")
                    && !c.getStatus().equalsIgnoreCase("PENDING"));
            if (allActioned) {
                checkerBatchProcessDao.updateBatchStatusTo(batch, "RETURNED");
            }
        }
    }

    // ── Confirm Refer Back (per-cheque) ───────────────────────────────────────

    @Override
    public void confirmReferBack(InwardBatch batch, InwardCheque cheque,
                                  String reasonCode, String targetModule,
                                  String remarks, User checker) {

        if (batch == null || cheque == null || checker == null) {
            throw new IllegalArgumentException("Batch, cheque, and checker are required.");
        }
        if (reasonCode == null || reasonCode.trim().isEmpty()) {
            throw new IllegalArgumentException("A reason must be selected.");
        }
        if (targetModule == null || targetModule.trim().isEmpty()) {
            throw new IllegalArgumentException("A target module must be selected.");
        }

        String reasonText = InwardReturnReason.getReasonText(reasonCode.trim());

        InwardCheckerAction action = new InwardCheckerAction();
        action.setInwardCheque(cheque);
        action.setInwardBatch(batch);
        action.setChecker(checker);
        action.setAction("SEND_BACK");
        action.setReasonCode(reasonCode.trim());
        action.setReasonText(reasonText);
        action.setRemarks(remarks);
        action.setReferBackModule(targetModule.trim());

        // Save action row + update cheque status and repair_status in DB
        checkerBatchProcessDao.saveReferBackAction(action);

        // Update in-memory cheque so batch-level checks work correctly
        cheque.setStatus("SEND_BACK");
        cheque.setReferBackModule(targetModule.trim());
        cheque.setRepairStatus(mapModuleToRepairStatus(targetModule.trim()));
    }


    // ── Private Helpers ───────────────────────────────────────────────────────

    private boolean isValidSubmitActionType(String action) {
        return "ACCEPTED".equalsIgnoreCase(action)
            || "RETURNED".equalsIgnoreCase(action);
    }

    private String getChequeNo(InwardCheckerAction action) {
        if (action.getInwardCheque() != null
                && action.getInwardCheque().getChequeNo() != null) {
            return action.getInwardCheque().getChequeNo();
        }
        return "Unknown";
    }

    private String mapModuleToRepairStatus(String module) {
        if (module == null) return "NEEDS_REPAIR";
        switch (module.toUpperCase()) {
            case "MICR_REPAIR":   return "REFERRED_MICR";
            case "DATE_AMOUNT":   return "REFERRED_DATEAMOUNT";
            case "PAYEE_ACCOUNT": return "REFERRED_PAYEEACCOUNT";
            default:              return "NEEDS_REPAIR";
        }
    }
}