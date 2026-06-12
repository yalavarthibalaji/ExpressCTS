package com.iispl.serviceImpl;

import com.iispl.dao.InwardBatchDao;
import com.iispl.daoImpl.InwardBatchDaoImpl;
import com.iispl.dto.InwardBatchDto;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.service.InwardBatchService;

import java.util.ArrayList;
import java.util.List;

public class InwardBatchServiceImpl implements InwardBatchService {

    private final InwardBatchDao inwardBatchDao = new InwardBatchDaoImpl();

    @Override
    public List<InwardBatch> getAllBatches() {
        return inwardBatchDao.findAll();
    }
    
    @Override
    public List<InwardBatchDto> getAllBatcheDtos() {
        List<InwardBatch>    batches = inwardBatchDao.findAll();
        List<InwardBatchDto> dtos    = new ArrayList<>();
        for (InwardBatch b : batches) {
            InwardBatchDto dto = new InwardBatchDto(
                    b.getId(),
                    b.getBatchId(),
                    b.getSourceFileName(),
                    b.getTotalCheques(),
                    b.getMicrErrorCount(),
                    b.getParsedAt(),
                    b.getStatus()
            );
            dto.setPendingRepairCount(inwardBatchDao.countSendBackCheques(b.getBatchId()));
            dtos.add(dto);
        }
        return dtos;
    }
}