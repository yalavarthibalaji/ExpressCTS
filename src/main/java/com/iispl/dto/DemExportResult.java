package com.iispl.dto;

/**
 * File    : com/iispl/dto/DemExportResult.java
 * Purpose : Result DTO returned by DemExportService.exportBatch().
 *
 * Holds success/failure flag plus the absolute paths of both generated
 * files (CXF and CIGF) so the UI can show them as download links.
 */
public class DemExportResult {

    private final boolean success;
    private final String  batchId;
    private final String  cxfFilePath;
    private final String  cigfFilePath;
    private final int     exportedChequeCount;
    private final String  errorMessage;

    private DemExportResult(boolean success,
                              String  batchId,
                              String  cxfFilePath,
                              String  cigfFilePath,
                              int     exportedChequeCount,
                              String  errorMessage) {
        this.success             = success;
        this.batchId             = batchId;
        this.cxfFilePath         = cxfFilePath;
        this.cigfFilePath        = cigfFilePath;
        this.exportedChequeCount = exportedChequeCount;
        this.errorMessage        = errorMessage;
    }

    public static DemExportResult success(String batchId,
                                            String cxfPath,
                                            String cigfPath,
                                            int    exportedCount) {
        return new DemExportResult(true, batchId, cxfPath, cigfPath,
                                     exportedCount, null);
    }

    public static DemExportResult failure(String batchId, String error) {
        return new DemExportResult(false, batchId, null, null, 0, error);
    }

    // ── Getters ──
    public boolean isSuccess()                { return success; }
    public String  getBatchId()               { return batchId; }
    public String  getCxfFilePath()           { return cxfFilePath; }
    public String  getCigfFilePath()          { return cigfFilePath; }
    public int     getExportedChequeCount()   { return exportedChequeCount; }
    public String  getErrorMessage()          { return errorMessage; }
}