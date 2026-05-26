package com.iispl.dto;

/**
 * CheckerBatch.java
 * Entity representing a batch in the Checker Verification stage.
 *
 * Package : com.iispl.entity
 * Pattern : MVC — Entity layer (pure data, no logic)
 *
 * FIXED:
 *   Added missing fields used by checkerDashboard.zul template:
 *     scannedAt, iqaPass, iqaFail, isMicrRepairBatch,
 *     route, cxfGenerated, actionLabel, statusLabel, statusBadgeClass
 */
public class CheckerBatch {

    // ── Core batch fields ────────────────────────────────────────────
    private String  batchId;
    private int     totalCheques;
    private long    totalAmount;
    private String  status;           // CHECKER_PENDING | APPROVED | RETURNED
    private int     approvedCount;
    private int     rejectedCount;
    private int     pendingCount;
    private boolean checkerDone;
    private String  checkerRemarks;

    // ── Fields used by Batch Management ZUL template ─────────────────
    private String  scannedAt;         // e.g. "15-Apr-2025 10:30"
    private int     iqaPass;           // number of cheques that passed IQA
    private int     iqaFail;           // number of cheques that failed IQA
    private boolean isMicrRepairBatch; // true if this is a MICR repair exception batch
    private String  route;             // "DIRECT_DEM" or "MAKER_CHECKER_DEM"
    private boolean cxfGenerated;      // true if CXF/CIBF file is generated
    private String  actionLabel;       // label for the action button in the table

    // ── Constructors ─────────────────────────────────────────────────

    public CheckerBatch() { }

    public CheckerBatch(String batchId, int totalCheques, long totalAmount, String status) {
        this.batchId       = batchId;
        this.totalCheques  = totalCheques;
        this.totalAmount   = totalAmount;
        this.status        = status;
        this.approvedCount = 0;
        this.rejectedCount = 0;
        this.pendingCount  = totalCheques;
        this.checkerDone   = false;
        this.iqaPass       = totalCheques; // default: all pass
        this.iqaFail       = 0;
        this.isMicrRepairBatch = false;
        this.route         = "MAKER_CHECKER_DEM";
        this.cxfGenerated  = false;
        this.scannedAt     = "";
        this.actionLabel   = "";
    }

    // ── Getters & Setters ─────────────────────────────────────────────

    public String getBatchId()                 { return batchId; }
    public void   setBatchId(String v)         { this.batchId = v; }

    public int    getTotalCheques()            { return totalCheques; }
    public void   setTotalCheques(int v)       { this.totalCheques = v; }

    public long   getTotalAmount()             { return totalAmount; }
    public void   setTotalAmount(long v)       { this.totalAmount = v; }

    public String getStatus()                  { return status; }
    public void   setStatus(String v)          { this.status = v; }

    public int    getApprovedCount()           { return approvedCount; }
    public void   setApprovedCount(int v)      { this.approvedCount = v; }

    public int    getRejectedCount()           { return rejectedCount; }
    public void   setRejectedCount(int v)      { this.rejectedCount = v; }

    public int    getPendingCount()            { return pendingCount; }
    public void   setPendingCount(int v)       { this.pendingCount = v; }

    public boolean isCheckerDone()             { return checkerDone; }
    public void    setCheckerDone(boolean v)   { this.checkerDone = v; }

    public String getCheckerRemarks()          { return checkerRemarks; }
    public void   setCheckerRemarks(String v)  { this.checkerRemarks = v; }

    // ── New fields used by ZUL template ──────────────────────────────

    public String getScannedAt()               { return scannedAt == null ? "" : scannedAt; }
    public void   setScannedAt(String v)       { this.scannedAt = v; }

    public int    getIqaPass()                 { return iqaPass; }
    public void   setIqaPass(int v)            { this.iqaPass = v; }

    public int    getIqaFail()                 { return iqaFail; }
    public void   setIqaFail(int v)            { this.iqaFail = v; }

    public boolean isMicrRepairBatch()         { return isMicrRepairBatch; }
    public void    setMicrRepairBatch(boolean v){ this.isMicrRepairBatch = v; }

    public String getRoute()                   { return route == null ? "MAKER_CHECKER_DEM" : route; }
    public void   setRoute(String v)           { this.route = v; }

    public boolean isCxfGenerated()            { return cxfGenerated; }
    public void    setCxfGenerated(boolean v)  { this.cxfGenerated = v; }

    public String getActionLabel()             { return actionLabel == null ? "" : actionLabel; }
    public void   setActionLabel(String v)     { this.actionLabel = v; }

    // ── Computed display helpers (used in ZUL as ${each.statusLabel} etc.) ──

    /**
     * Returns a human-readable status label for display in the table.
     * Used in ZUL: ${each.statusLabel}
     */
    public String getStatusLabel() {
        if (status == null) return "Unknown";
        switch (status) {
            case "CHECKER_PENDING": return "Pending Verification";
            case "APPROVED":        return "✓ Approved";
            case "RETURNED":        return "↩ Returned to Maker";
            case "DISPATCHED":      return "Dispatched";
            default:                return status.replace("_", " ");
        }
    }

    /**
     * Returns a CSS sclass for the status badge.
     * Used in ZUL: ${each.statusBadgeClass}
     */
    public String getStatusBadgeClass() {
        if (status == null) return "cts-badge cts-badge-gray";
        switch (status) {
            case "CHECKER_PENDING": return "cts-badge cts-badge-orange";
            case "APPROVED":        return "cts-badge cts-badge-green";
            case "RETURNED":        return "cts-badge cts-badge-red";
            case "DISPATCHED":      return "cts-badge cts-badge-blue";
            default:                return "cts-badge cts-badge-gray";
        }
    }

    /**
     * Formatted total amount for display, e.g. "₹29,52,500"
     */
    public String getFormattedAmount() {
        java.text.NumberFormat nf =
            java.text.NumberFormat.getInstance(new java.util.Locale("en", "IN"));
        return "\u20B9" + nf.format(totalAmount);
    }

    /**
     * Progress percentage (0–100) for the progress bar in the table.
     */
    public int getProgressPercent() {
        if (totalCheques == 0) return 0;
        return (int) Math.round((approvedCount * 100.0) / totalCheques);
    }

    @Override
    public String toString() {
        return "CheckerBatch{batchId='" + batchId + "', status='" + status +
               "', total=" + totalCheques + ", approved=" + approvedCount + "}";
    }
}