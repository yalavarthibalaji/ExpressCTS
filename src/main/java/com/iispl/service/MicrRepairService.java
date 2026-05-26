package com.iispl.service;

import com.iispl.entity.MicrRepairEntry;
import com.iispl.entity.OutwardBatch;

import java.util.List;

public interface MicrRepairService {

    // Get all MICR repair batches (isMicrRepairBatch = true)
    List<OutwardBatch> getMicrRepairBatches();

    // Get all repair entries for a batch
    List<MicrRepairEntry> getRepairEntriesByBatchId(Long batchId);

    // Get a single repair entry by its id
    MicrRepairEntry getRepairEntryById(Long id);

    // MICR operator submits corrected data for a cheque
    void submitRepair(Long entryId,
                      String correctedMicrCode,
                      String correctedIfscCode,
                      String correctedAccountNumber,
                      String correctedAmount,
                      String correctedChequeDate,
                      String repairRemarks,
                      String repairedBy);

    // MICR operator flags a cheque (cannot repair — return to depositor)
    void flagEntry(Long entryId, String flagReason, String repairedBy);

    // Checker approves or rejects a repair — saves checker fields back to DB
    void saveCheckerVerification(MicrRepairEntry entry);
}