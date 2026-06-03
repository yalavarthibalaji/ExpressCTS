package com.iispl.dao;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;

import java.util.Date;
import java.util.List;

public interface CheckerInwardVerificationDao {

	List<InwardBatch> findPendingBatches();

	List<InwardBatch> findClearedBatches(Date fromDate, Date toDate);

	List<InwardCheque> findReturnedCheques(Date fromDate, Date toDate, String batchId);
}