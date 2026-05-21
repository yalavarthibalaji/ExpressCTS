package com.iispl.dao;

import com.iispl.entity.OutwardCheque;
import java.util.List;

public interface OutwardChequeDao {

    // save a single cheque to master table
    void saveCheque(OutwardCheque cheque);

    // find all cheques by batch id
    List<OutwardCheque> findAllByBatchId(Long batchId);

    // find cheque by cheque id
    OutwardCheque findByChequeId(String chequeId);

}