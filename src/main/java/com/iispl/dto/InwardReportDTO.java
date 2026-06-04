package com.iispl.dto;

/**
 * File    : com/iispl/dto/InwardReportDTO.java
 * Purpose : Data transfer object for one row of the Inward Reports grid.
 *
 *   Redesigned columns (matching target UI screenshot):
 *     batchId          → inward_batch.batch_id
 *     batchDate        → inward_batch.batch_date (formatted dd/MM/yyyy)
 *     totalCheques     → inward_batch.total_cheques
 *     acceptedCount    → COUNT(ic) WHERE ic.status = 'ACCEPTED'
 *     returnedCount    → COUNT(ic) WHERE ic.status = 'RETURNED'
 *     presentingBanks  → distinct presenting_bank_name values (comma-joined)
 *     status           → inward_batch.status
 *
 *   Legacy fields (micrErrors, iqaFails, passedCount, rejectedCount,
 *   referredCount) are RETAINED for backward compatibility with
 *   XML export builders (buildCxfXml / buildBrfXml) — they are simply
 *   no longer displayed in the grid.
 */
public class InwardReportDTO {

    // ── Display fields (new target columns) ───────────────────────────────────
    private String batchId;
    private String batchDate;           // formatted for display
    private int    totalCheques;
    private int    acceptedCount;       // was passedCount — maps to ic.status='ACCEPTED'
    private int    returnedCount;       // new — maps to ic.status='RETURNED'
    private String presentingBanks;     // new — distinct bank names, comma-joined

    // ── Legacy fields (kept for XML export logic) ─────────────────────────────
    private int    micrErrors;
    private int    iqaFails;
    private int    passedCount;         // kept as alias for acceptedCount
    private int    rejectedCount;
    private int    referredCount;

    private String status;

    // ── Constructors ──────────────────────────────────────────────────────────

    public InwardReportDTO() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getBatchId()                              { return batchId; }
    public void   setBatchId(String batchId)                { this.batchId = batchId; }

    public String getBatchDate()                            { return batchDate; }
    public void   setBatchDate(String batchDate)            { this.batchDate = batchDate; }

    public int    getTotalCheques()                         { return totalCheques; }
    public void   setTotalCheques(int totalCheques)         { this.totalCheques = totalCheques; }

    public int    getAcceptedCount()                        { return acceptedCount; }
    public void   setAcceptedCount(int acceptedCount)       { this.acceptedCount = acceptedCount; }

    public int    getReturnedCount()                        { return returnedCount; }
    public void   setReturnedCount(int returnedCount)       { this.returnedCount = returnedCount; }

    public String getPresentingBanks()                      { return presentingBanks; }
    public void   setPresentingBanks(String presentingBanks){ this.presentingBanks = presentingBanks; }

    // Legacy — kept for XML export compatibility
    public int    getMicrErrors()                           { return micrErrors; }
    public void   setMicrErrors(int micrErrors)             { this.micrErrors = micrErrors; }

    public int    getIqaFails()                             { return iqaFails; }
    public void   setIqaFails(int iqaFails)                 { this.iqaFails = iqaFails; }

    public int    getPassedCount()                          { return passedCount; }
    public void   setPassedCount(int passedCount)           { this.passedCount = passedCount; }

    public int    getRejectedCount()                        { return rejectedCount; }
    public void   setRejectedCount(int rejectedCount)       { this.rejectedCount = rejectedCount; }

    public int    getReferredCount()                        { return referredCount; }
    public void   setReferredCount(int referredCount)       { this.referredCount = referredCount; }

    public String getStatus()                               { return status; }
    public void   setStatus(String status)                  { this.status = status; }

    @Override
    public String toString() {
        return "InwardReportDTO{batchId='" + batchId + "', status='" + status +
               "', totalCheques=" + totalCheques + ", accepted=" + acceptedCount +
               ", returned=" + returnedCount + ", banks='" + presentingBanks + "'}";
    }
}