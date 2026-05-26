package com.iispl.dao;

import com.iispl.entity.MicrRepairEntry;
import com.iispl.entity.OutwardBatch;

import java.util.List;

public interface MicrRepairDao {

    // Load all MICR repair batches (isMicrRepairBatch = true)
    List<OutwardBatch> getMicrRepairBatches();

    // Load all repair entries for a given batch
    List<MicrRepairEntry> getRepairEntriesByBatchId(Long batchId);

    // Load a single repair entry by its id
    MicrRepairEntry getRepairEntryById(Long id);

    // Save corrected data entered by MICR operator
    void saveRepairEntry(MicrRepairEntry entry);

    // Update repair status and corrected fields
    void updateRepairEntry(MicrRepairEntry entry);
}