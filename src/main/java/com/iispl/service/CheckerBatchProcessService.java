package com.iispl.service;

import java.util.List;

import com.iispl.entity.User;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;

public interface CheckerBatchProcessService {
	
	InwardBatch loadBatchForProcessing(String batchId);
	void submitBatch(InwardBatch batch, List<InwardCheckerAction> actions, User checker);
}
