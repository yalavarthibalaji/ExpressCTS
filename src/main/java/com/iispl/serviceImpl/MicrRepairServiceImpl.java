package com.iispl.serviceImpl;

import com.iispl.dao.MicrRepairDao;
import com.iispl.daoImpl.MicrRepairDaoImpl;
import com.iispl.entity.MicrRepairEntry;
import com.iispl.entity.OutwardBatch;
import com.iispl.service.MicrRepairService;

import java.time.LocalDateTime;
import java.util.List;

public class MicrRepairServiceImpl implements MicrRepairService {

    private final MicrRepairDao micrRepairDao = new MicrRepairDaoImpl();

    @Override
    public List<OutwardBatch> getMicrRepairBatches() {
        return micrRepairDao.getMicrRepairBatches();
    }

    @Override
    public List<MicrRepairEntry> getRepairEntriesByBatchId(Long batchId) {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
        return micrRepairDao.getRepairEntriesByBatchId(batchId);
    }

    @Override
    public MicrRepairEntry getRepairEntryById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Entry ID cannot be null");
        }
        return micrRepairDao.getRepairEntryById(id);
    }

    @Override
    public void submitRepair(Long entryId,
                             String correctedMicrCode,
                             String correctedIfscCode,
                             String correctedAccountNumber,
                             String correctedAmount,
                             String correctedChequeDate,
                             String repairRemarks,
                             String repairedBy) {

        if (entryId == null) {
            throw new IllegalArgumentException("Entry ID cannot be null");
        }
        if (repairRemarks == null || repairRemarks.trim().isEmpty()) {
            throw new IllegalArgumentException("Repair remarks are required");
        }
        if (repairedBy == null || repairedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Repaired by user ID is required");
        }

        MicrRepairEntry entry = micrRepairDao.getRepairEntryById(entryId);
        if (entry == null) {
            throw new RuntimeException("Repair entry not found for ID: " + entryId);
        }

        // Save corrected values (only update fields that are not blank)
        if (correctedMicrCode != null && !correctedMicrCode.trim().isEmpty()) {
            entry.setCorrectedMicrCode(correctedMicrCode.trim());
        }
        if (correctedIfscCode != null && !correctedIfscCode.trim().isEmpty()) {
            entry.setCorrectedIfscCode(correctedIfscCode.trim());
        }
        if (correctedAccountNumber != null && !correctedAccountNumber.trim().isEmpty()) {
            entry.setCorrectedAccountNumber(correctedAccountNumber.trim());
        }
        if (correctedAmount != null && !correctedAmount.trim().isEmpty()) {
            entry.setCorrectedAmount(correctedAmount.trim());
        }
        if (correctedChequeDate != null && !correctedChequeDate.trim().isEmpty()) {
            entry.setCorrectedChequeDate(correctedChequeDate.trim());
        }

        entry.setRepairRemarks(repairRemarks.trim());
        entry.setRepairedBy(repairedBy.trim());
        entry.setRepairedAt(LocalDateTime.now());
        entry.setRepairStatus("REPAIRED");

        micrRepairDao.updateRepairEntry(entry);
    }

    @Override
    public void flagEntry(Long entryId, String flagReason, String repairedBy) {
        if (entryId == null) {
            throw new IllegalArgumentException("Entry ID cannot be null");
        }
        if (flagReason == null || flagReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Flag reason is required");
        }

        MicrRepairEntry entry = micrRepairDao.getRepairEntryById(entryId);
        if (entry == null) {
            throw new RuntimeException("Repair entry not found for ID: " + entryId);
        }

        entry.setFlagReason(flagReason.trim());
        if (repairedBy != null && !repairedBy.trim().isEmpty()) {
            entry.setRepairedBy(repairedBy.trim());
        }
        entry.setRepairedAt(LocalDateTime.now());
        entry.setRepairStatus("FLAGGED");

        micrRepairDao.updateRepairEntry(entry);
    }

    @Override
    public void saveCheckerVerification(MicrRepairEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entry cannot be null");
        }
        // Directly update the full entry — checker has already set the fields
        micrRepairDao.updateRepairEntry(entry);
    }
}