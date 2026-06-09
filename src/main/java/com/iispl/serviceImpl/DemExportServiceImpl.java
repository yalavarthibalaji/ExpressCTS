package com.iispl.serviceImpl;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.iispl.dao.OutwardBatchDao;
import com.iispl.dao.OutwardChequeDao;
import com.iispl.dao.OutwardExportDao;
import com.iispl.daoImpl.OutwardBatchDaoImpl;
import com.iispl.daoImpl.OutwardChequeDaoImpl;
import com.iispl.daoImpl.OutwardExportDaoImpl;
import com.iispl.dto.DemExportResult;
import com.iispl.entity.User;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.entity.outward.OutwardExport;
import com.iispl.service.AuditService;
import com.iispl.service.DemExportService;
import com.iispl.util.CigfFileGenerator;
import com.iispl.util.CxfFileGenerator;

/**
 * File    : com/iispl/serviceImpl/DemExportServiceImpl.java
 * Purpose : Implements the DEM Export workflow.
 *
 * Output folder layout:
 *   /opt/cts/exports/outward/{batchId}/{batchId}.cxf
 *   /opt/cts/exports/outward/{batchId}/{batchId}.cigf
 *
 * If /opt/cts/exports is not writable on the dev machine, change
 * EXPORT_BASE_DIR to a path that is writable (e.g. /tmp/cts-exports).
 *
 * Phase F3-B additions:
 *   - AuditService logs EXPORT_GENERATED for each generated file (CXF + CIGF)
 *   - AuditService logs BATCH_EXPORTED at the batch level when status moves to EXPORTED
 *   - markTransmitted() logs EXPORT_TRANSMITTED (using the export row's
 *     generatedBy as the actor, since this method has no explicit user param)
 */
public class DemExportServiceImpl implements DemExportService {

    /** Base directory where CXF and CIGF files are written. */
    private static final String EXPORT_BASE_DIR = "/opt/cts/exports/outward";

    private final OutwardBatchDao  batchDao     = new OutwardBatchDaoImpl();
    private final OutwardChequeDao chequeDao    = new OutwardChequeDaoImpl();
    private final OutwardExportDao exportDao    = new OutwardExportDaoImpl();
    private final AuditService     auditService = new AuditServiceImpl();

    // ════════════════════════════════════════════════════
    //  Queue Loading
    // ════════════════════════════════════════════════════

    @Override
    public List<OutwardBatch> getExportableBatches() {
        try {
            List<OutwardBatch> list = batchDao.findCheckerApprovedBatches();
            System.out.println("DemExportService → getExportableBatches: " + list.size());
            return list;
        } catch (Exception e) {
            System.err.println("DemExportService → getExportableBatches failed: "
                    + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<OutwardBatch> getExportedBatches() {
        try {
            // Reuse the existing findAll filter via countByStatus pattern —
            // here we filter the full list since we need entities, not counts.
            List<OutwardBatch> all = batchDao.findAll();
            java.util.List<OutwardBatch> exported = new java.util.ArrayList<>();
            for (OutwardBatch b : all) {
                if ("EXPORTED".equals(b.getStatus())) exported.add(b);
            }
            System.out.println("DemExportService → getExportedBatches: " + exported.size());
            return exported;
        } catch (Exception e) {
            System.err.println("DemExportService → getExportedBatches failed: "
                    + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<OutwardExport> getExportsForBatch(Long batchDbId) {
        if (batchDbId == null) return Collections.emptyList();
        return exportDao.findByBatchId(batchDbId);
    }

    // ════════════════════════════════════════════════════
    //  Main Export Workflow
    // ════════════════════════════════════════════════════

    @Override
    public DemExportResult exportBatch(Long batchDbId, Long checkerId) {

        if (batchDbId == null) {
            return DemExportResult.failure(null, "Batch ID is required.");
        }
        if (checkerId == null) {
            return DemExportResult.failure(null, "Checker session expired.");
        }

        // ── Step 1: Load batch and cheques ──
        OutwardBatch batch;
        List<OutwardCheque> cheques;
        try {
            // Find the batch from the approved list (already includes those eligible)
            List<OutwardBatch> approved = batchDao.findCheckerApprovedBatches();
            batch = null;
            for (OutwardBatch b : approved) {
                if (b.getId().equals(batchDbId)) { batch = b; break; }
            }
            if (batch == null) {
                return DemExportResult.failure(null,
                    "Batch is not in CHECKER_APPROVED state. "
                    + "Only approved batches can be exported.");
            }

            cheques = chequeDao.findAllByBatchDbId(batchDbId);
            if (cheques == null || cheques.isEmpty()) {
                return DemExportResult.failure(batch.getBatchId(),
                    "No cheques found for batch " + batch.getBatchId());
            }
        } catch (Exception e) {
            System.err.println("DemExportService → load failed: " + e.getMessage());
            return DemExportResult.failure(null,
                "Failed to load batch data: " + e.getMessage());
        }

        // ── Step 2: Ensure output directory exists ──
        File outputDir = new File(EXPORT_BASE_DIR, batch.getBatchId());

        // ── Step 3: Generate CXF file ──
        String cxfPath;
        try {
            cxfPath = CxfFileGenerator.generate(batch, cheques, outputDir, checkerId);
        } catch (Exception e) {
            System.err.println("DemExportService → CXF generation failed: " + e.getMessage());
            return DemExportResult.failure(batch.getBatchId(),
                "CXF file generation failed: " + e.getMessage());
        }

        // ── Step 4: Generate CIGF file ──
        String cigfPath;
        try {
            cigfPath = CigfFileGenerator.generate(batch, cheques, outputDir);
        } catch (Exception e) {
            System.err.println("DemExportService → CIGF generation failed: " + e.getMessage());
            return DemExportResult.failure(batch.getBatchId(),
                "CIGF file generation failed: " + e.getMessage());
        }

        // ── Step 5: Record both files in outward_exports ──
        OutwardExport cxfExport;
        OutwardExport cigfExport;
        try {
            cxfExport  = saveExportRow(batch, "CXF",  cxfPath,  checkerId);
            cigfExport = saveExportRow(batch, "CIGF", cigfPath, checkerId);
        } catch (Exception e) {
            System.err.println("DemExportService → DB record failed: " + e.getMessage());
            return DemExportResult.failure(batch.getBatchId(),
                "Files generated but DB record failed: " + e.getMessage());
        }

        // ── Step 6: Update batch status to EXPORTED ──
        boolean statusUpdated = batchDao.updateStatus(batchDbId, "EXPORTED");
        if (!statusUpdated) {
            System.err.println("DemExportService → batch status update to EXPORTED failed");
            // Continue — files are generated, audit rows saved.
            // Status will be corrected by manual intervention if needed.
        }

        // ── Step 7: Count exported cheques for the result ──
        int exportedCount = 0;
        for (OutwardCheque c : cheques) {
            if ("CHECKER_PASSED".equalsIgnoreCase(c.getStatus())) exportedCount++;
        }

        // ── Step 8 (F3-B): Audit log ──
        try {
            // One row per generated file
            if (cxfExport != null && cxfExport.getId() != null) {
                auditService.log(
                    checkerId,
                    AuditService.M_DEM_EXPORT,
                    AuditService.A_EXPORT_GENERATED,
                    AuditService.E_EXPORT_FILE,
                    cxfExport.getId(),
                    null,
                    "type=CXF, file=" + cxfExport.getFileName()
                    + ", batchId=" + batch.getBatchId());
            }
            if (cigfExport != null && cigfExport.getId() != null) {
                auditService.log(
                    checkerId,
                    AuditService.M_DEM_EXPORT,
                    AuditService.A_EXPORT_GENERATED,
                    AuditService.E_EXPORT_FILE,
                    cigfExport.getId(),
                    null,
                    "type=CIGF, file=" + cigfExport.getFileName()
                    + ", batchId=" + batch.getBatchId());
            }
            // Batch-level state change row
            if (statusUpdated) {
                auditService.log(
                    checkerId,
                    AuditService.M_DEM_EXPORT,
                    "BATCH_EXPORTED",
                    AuditService.E_OUTWARD_BATCH,
                    batch.getId(),
                    "status=CHECKER_APPROVED",
                    "status=EXPORTED, exportedCheques=" + exportedCount);
            }
        } catch (Exception e) {
            // Audit failure must never break the export flow
            System.err.println("DemExportService → audit log failed (non-critical): "
                    + e.getMessage());
        }

        System.out.println("DemExportService → Export complete for batch "
                + batch.getBatchId() + " | exportedCheques=" + exportedCount);

        return DemExportResult.success(batch.getBatchId(),
                                        cxfPath, cigfPath, exportedCount);
    }

    // ════════════════════════════════════════════════════
    //  Transmit
    // ════════════════════════════════════════════════════

    @Override
    public boolean markTransmitted(Long exportId) {
        if (exportId == null) return false;

        boolean ok = exportDao.markTransmitted(exportId);

        if (ok) {
            // F3-B audit log — userId is null because markTransmitted has no
            // explicit user context. AuditServiceImpl handles null userId by
            // falling back to a "system" row.
            // The EXPORT_GENERATED audit row already records who generated the
            // file, so the actor for the transmit event is implicit.
            try {
                auditService.log(
                    null,
                    AuditService.M_DEM_EXPORT,
                    AuditService.A_EXPORT_TRANSMITTED,
                    AuditService.E_EXPORT_FILE,
                    exportId,
                    "status=GENERATED",
                    "status=TRANSMITTED");
            } catch (Exception e) {
                System.err.println("DemExportService → markTransmitted audit failed "
                        + "(non-critical): " + e.getMessage());
            }
        }

        return ok;
    }
    // ════════════════════════════════════════════════════
    //  Private Helpers
    // ════════════════════════════════════════════════════

    /**
     * Inserts a row in outward_exports for one generated file.
     *
     * Phase F3-B change:
     *   - Returns the persisted OutwardExport so callers can read its id
     *     for audit logging. Previously returned void.
     */
    private OutwardExport saveExportRow(OutwardBatch batch,
                                          String        fileType,
                                          String        filePath,
                                          Long          checkerId) {
        OutwardExport export = new OutwardExport();

        // Reference batch and user by id-only proxy (no full load required)
        OutwardBatch batchRef = new OutwardBatch();
        batchRef.setId(batch.getId());
        export.setBatch(batchRef);

        User userRef = new User();
        userRef.setId(checkerId);
        export.setGeneratedBy(userRef);

        export.setFileType(fileType);
        export.setFileName(new File(filePath).getName());
        export.setFilePath(filePath);
        export.setStatus("GENERATED");
        export.setGeneratedAt(LocalDateTime.now());

        exportDao.save(export);
        // After session.persist(), Hibernate populates the id on the entity
        return export;
    }
}