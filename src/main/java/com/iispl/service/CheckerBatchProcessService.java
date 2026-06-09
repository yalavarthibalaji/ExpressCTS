package com.iispl.service;

import java.util.List;

import com.iispl.entity.User;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;

public interface CheckerBatchProcessService {
	
	InwardBatch loadBatchForProcessing(String batchId);
	void submitBatch(InwardBatch batch, List<InwardCheckerAction> actions, User checker);

	/**
	 * Confirms a Return action for a single cheque:
	 *   1. Validates the reasonCode is not blank.
	 *   2. Saves InwardCheckerAction with action=RETURNED.
	 *   3. Updates the cheque status to RETURNED.
	 *   4. If every cheque in the batch has been actioned, marks the
	 *      batch status as RETURNED.
	 *
	 * @param batch      the parent batch
	 * @param cheque     the cheque being returned
	 * @param reasonCode NPCI return reason code (must not be null/empty)
	 * @param checker    the logged-in checker user
	 */
	void confirmReturn(InwardBatch batch, com.iispl.entity.inward.InwardCheque cheque,
	                   String reasonCode, User checker);
}