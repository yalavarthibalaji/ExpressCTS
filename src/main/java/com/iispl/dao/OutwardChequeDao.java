package com.iispl.dao;

import com.iispl.entity.OutwardCheque;
import java.util.List;

public interface OutwardChequeDao {

    // save a new cheque to master table
    void saveCheque(OutwardCheque cheque);

    // update an existing cheque (used by maker to save verified data)
    void updateCheque(OutwardCheque cheque);

    // find all cheques belonging to a batch
    List<OutwardCheque> findAllByBatchId(Long batchId);

    // find cheque by cheque id (string like CHQ-...)
    OutwardCheque findByChequeId(String chequeId);

}