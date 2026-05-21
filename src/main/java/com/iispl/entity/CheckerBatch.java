package com.iispl.entity;

/**
 * CheckerBatch.java
 * Entity representing a batch that is in the Checker Verification stage.
 *
 * Package  : com.iispl.entity
 * Pattern  : MVC — Entity layer (pure data, no logic)
 *
 * In a real project this maps 1-to-1 with a DB table row.
 * For training, data is loaded from a hardcoded utility.
 *
 * Fields:
 *   batchId       — e.g. "BATCH15042025001"
 *   totalCheques  — number of cheques in the batch
 *   totalAmount   — sum of all cheque amounts
 *   status        — "CHECKER_PENDING" or "APPROVED"
 *   approvedCount — how many individual cheques spot-checked
 *   rejectedCount — how many flagged as exceptions
 *   pendingCount  — not yet reviewed
 *   checkerDone   — true when batch-level approval done
 */
public class CheckerBatch {

    private String batchId;
    private int    totalCheques;
    private long   totalAmount;
    private String status;          // CHECKER_PENDING | APPROVED | RETURNED
    private int    approvedCount;
    private int    rejectedCount;
    private int    pendingCount;
    private boolean checkerDone;
    private String checkerRemarks;  // remarks when returned to maker

    // ── Constructors ────────────────────────────────────────────────

    public CheckerBatch() { }

    public CheckerBatch(String batchId, int totalCheques, long totalAmount, String status) {
        this.batchId      = batchId;
        this.totalCheques = totalCheques;
        this.totalAmount  = totalAmount;
        this.status       = status;
        this.approvedCount = 0;
        this.rejectedCount = 0;
        this.pendingCount  = totalCheques;
        this.checkerDone   = false;
    }

    // ── Getters & Setters ────────────────────────────────────────────

    public String getBatchId()               { return batchId; }
    public void   setBatchId(String v)       { this.batchId = v; }

    public int    getTotalCheques()          { return totalCheques; }
    public void   setTotalCheques(int v)     { this.totalCheques = v; }

    public long   getTotalAmount()           { return totalAmount; }
    public void   setTotalAmount(long v)     { this.totalAmount = v; }

    public String getStatus()               { return status; }
    public void   setStatus(String v)       { this.status = v; }

    public int    getApprovedCount()        { return approvedCount; }
    public void   setApprovedCount(int v)   { this.approvedCount = v; }

    public int    getRejectedCount()        { return rejectedCount; }
    public void   setRejectedCount(int v)   { this.rejectedCount = v; }

    public int    getPendingCount()         { return pendingCount; }
    public void   setPendingCount(int v)    { this.pendingCount = v; }

    public boolean isCheckerDone()          { return checkerDone; }
    public void    setCheckerDone(boolean v){ this.checkerDone = v; }

    public String getCheckerRemarks()       { return checkerRemarks; }
    public void   setCheckerRemarks(String v){ this.checkerRemarks = v; }

    // ── Helper ───────────────────────────────────────────────────────

    /** Formatted total amount for display, e.g. "₹29,52,500" */
    public String getFormattedAmount() {
        java.text.NumberFormat nf =
            java.text.NumberFormat.getInstance(new java.util.Locale("en", "IN"));
        return "\u20B9" + nf.format(totalAmount);
    }

    /** Progress percentage (0–100) for the progress bar in the table. */
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
