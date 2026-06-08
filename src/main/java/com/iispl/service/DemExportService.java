package com.iispl.service;

import com.iispl.dto.DemExportResult;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardExport;

import java.util.List;

/**
 * File    : com/iispl/service/DemExportService.java
 * Purpose : Service interface for the DEM Export workflow.
 *
 * Workflow:
 *   1. Checker approves a batch (status = CHECKER_APPROVED).
 *   2. Checker opens the DEM Export screen.
 *   3. Approved batches are listed (getExportableBatches()).
 *   4. Checker clicks Export → exportBatch(batchDbId, checkerId) runs:
 *        a. Generate CXF file (XML)
 *        b. Generate CIGF file (image manifest XML)
 *        c. Save 2 rows into outward_exports table
 *        d. Update batch.status = EXPORTED
 *   5. Checker may then click Transmit → markTransmitted(exportId).
 */
public interface DemExportService {

    /**
     * Returns all batches that are ready for DEM Export.
     * Status filter: CHECKER_APPROVED only.
     */
    List<OutwardBatch> getExportableBatches();

    /**
     * Returns all batches already exported.
     * Status filter: EXPORTED.
     */
    List<OutwardBatch> getExportedBatches();

    /**
     * Returns all export-file records (CXF + CIGF) for a single batch.
     */
    List<OutwardExport> getExportsForBatch(Long batchDbId);

    /**
     * Performs the full export for one batch:
     *   1. Generates CXF and CIGF files on disk.
     *   2. Inserts 2 rows into outward_exports.
     *   3. Updates batch.status to EXPORTED.
     *
     * @param batchDbId  primary key of the outward_batch row
     * @param checkerId  user id of the checker triggering the export
     * @return DemExportResult with file paths or error message
     */
    DemExportResult exportBatch(Long batchDbId, Long checkerId);

    /**
     * Marks a specific export row as transmitted to NPCI.
     * In real environment, would trigger SFTP/MQ transmission.
     * Here we just flip the status flag.
     */
    boolean markTransmitted(Long exportId);
}