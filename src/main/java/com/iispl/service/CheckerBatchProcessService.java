package com.iispl.service;

import java.util.List;

import com.iispl.entity.User;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;
import com.iispl.entity.inward.InwardCheque;

public interface CheckerBatchProcessService {

    InwardBatch loadBatchForProcessing(String batchId);

    void submitBatch(InwardBatch batch, List<InwardCheckerAction> actions, User checker);

    void confirmReturn(InwardBatch batch, InwardCheque cheque,
                       String reasonCode, User checker);

    void confirmReferBack(InwardBatch batch, InwardCheque cheque,
                          String reasonCode, String targetModule,
                          String remarks, User checker);

    // Decrements total_cheques count in DB after a refer-back removal
    void decrementBatchChequeCount(InwardBatch batch);
}