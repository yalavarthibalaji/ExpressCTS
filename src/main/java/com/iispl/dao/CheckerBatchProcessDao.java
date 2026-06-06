package com.iispl.dao;

import java.util.List;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;

public interface CheckerBatchProcessDao {
	
	InwardBatch findBatchWithCheques(String batchId);
	void saveCheckerActions(List<InwardCheckerAction> actions);
	void updateBatchStatus(InwardBatch batch);
	
}
