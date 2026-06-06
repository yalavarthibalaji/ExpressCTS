package com.iispl.dao;

import java.util.List;
import com.iispl.entity.inward.InwardCheque;

public interface DateAmountDao {

	// Step 2 + Step 3 — list
	List<InwardCheque> findChequesByBatchId(String batchId);

	List<InwardCheque> findChequesByBatchAndStatus(String batchId, String repairStatus);

	// Single record
	InwardCheque findChequeById(Long chequeId); // ← Long, not String

	// Step 2 actions
	int updateDateAndAmount(InwardCheque cheque); // updates chequeDateOcr + amountOcr

	int rejectCheque(Long chequeId, String rejectReason, String remarks);

	int referChequeBack(Long chequeId, String referReason, String remarks);

	// Step 3 action
	int updatePayeeAndAccount(InwardCheque cheque); // updates payeeName + draweeAccountNumber

	// Submit batch to Checker
	int submitBatchToChecker(String batchId);
}