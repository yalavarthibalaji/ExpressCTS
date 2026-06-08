package com.iispl.dao;

import com.iispl.entity.outward.OutwardExport;
import java.util.List;

/**
 * File    : com/iispl/dao/OutwardExportDao.java
 * Purpose : Database operations for the outward_exports table.
 *
 * Each row in outward_exports represents ONE generated file
 * (CXF or CIGF) for ONE outward batch, including its file path,
 * status, and generation/transmission timestamps.
 */
public interface OutwardExportDao {

    /**
     * Saves a single OutwardExport row.
     *
     * @return the saved entity with id populated, or null on failure
     */
    OutwardExport save(OutwardExport export);

    /**
     * Find all exports for a given batch (CXF + CIGF together).
     */
    List<OutwardExport> findByBatchId(Long batchDbId);

    /**
     * Find the most recent export of a given fileType for a batch.
     *
     * @param fileType "CXF" or "CIGF"
     * @return the matching export or null if none
     */
    OutwardExport findLatestByBatchAndType(Long batchDbId, String fileType);

    /**
     * Marks an export as transmitted to NPCI.
     * Updates status = 'TRANSMITTED' and transmitted_at = NOW().
     */
    boolean markTransmitted(Long exportId);

    /**
     * Count of exports of a given fileType across all batches.
     * Used in dashboard KPIs.
     */
    int countByFileType(String fileType);
}