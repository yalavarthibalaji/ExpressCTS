package com.iispl.service;

import com.iispl.dto.InwardBatchDto;
import com.iispl.entity.inward.InwardBatch;
import java.util.List;

public interface InwardBatchService {
    List<InwardBatch> getAllBatches();
    public List<InwardBatchDto> getAllBatcheDtos();
}