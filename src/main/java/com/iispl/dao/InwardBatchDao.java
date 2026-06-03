package com.iispl.dao;

import com.iispl.entity.inward.InwardBatch;
import java.util.List;

public interface InwardBatchDao {
    List<InwardBatch> findAll();
}