package com.iispl.dao;

import com.iispl.entity.inward.InwardCheque;
import java.util.List;

public interface InwardChequeDao {

    void save(InwardCheque cheque);
    void saveAll(List<InwardCheque> cheques);
    InwardCheque findById(Long id);
    List<InwardCheque> findByBatchId(Long batchId);
    void update(InwardCheque cheque);
}