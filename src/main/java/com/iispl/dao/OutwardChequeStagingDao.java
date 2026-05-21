package com.iispl.dao;

import com.iispl.entity.OutwardChequeStaging;
import java.util.List;

public interface OutwardChequeStagingDao {

    // save a single cheque to staging table
    void saveStagingCheque(OutwardChequeStaging stagingCheque);

    // get all staging cheques for a batch
    List<OutwardChequeStaging> findAllByBatchId(String batchId);

    // get all PENDING cheques for a batch
    List<OutwardChequeStaging> findPendingByBatchId(String batchId);

    // update staging status of a cheque (APPROVED or REJECTED)
    void updateStagingStatus(Long id, String status, String reviewedBy);

}