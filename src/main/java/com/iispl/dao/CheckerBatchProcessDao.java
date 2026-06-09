package com.iispl.dao;

import java.util.List;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;

public interface CheckerBatchProcessDao {
	
	InwardBatch findBatchWithCheques(String batchId);
	void saveCheckerActions(List<InwardCheckerAction> actions);
	void updateBatchStatus(InwardBatch batch);

	/**
	 * Persists a single RETURNED checker action and updates the
	 * linked InwardCheque status to "RETURNED" — in one transaction.
	 */
	void saveReturnAction(InwardCheckerAction action);

	/**
	 * Updates the InwardBatch status to the supplied statusValue
	 * (e.g. "RETURNED", "Verified") — in its own transaction.
	 */
	void updateBatchStatusTo(InwardBatch batch, String statusValue);
	
}