package com.iispl.service;

import java.util.Date;
import java.util.List;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;

public interface CheckerInwardVerificationService {
	
	List<InwardBatch> getPendingBatches(String keyword);

    List<InwardBatch> getClearedBatches(String keyword, Date fromDate, Date toDate);

    List<InwardCheque> getReturnedCheques(String keyword, Date fromDate, Date toDate, String batchId);
}
