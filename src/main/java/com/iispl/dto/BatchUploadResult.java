package com.iispl.dto;

import java.util.ArrayList;
import java.util.List;

import com.iispl.entity.outward.OutwardCheque;

/**
 * File    : com/iispl/dto/BatchUploadResult.java
 * Purpose : Result object returned from BatchUploadServiceImpl to the composer.
 *
 *           Two creation paths:
 *             - success(...) → all fields populated, errorMessage = null
 *             - failure(msg) → only errorMessage set, isSuccess() = false
 *
 *           Mutable: composer updates fields after rejecting individual cheques.
 */
public class BatchUploadResult {

    // ── Batch identity ──
    private String  batchId;
    private Long    batchDbId;

    // ── Parsed totals (mutable — updated after cheque rejection) ──
    private int     parsedChequeCount;
    private double  parsedTotalAmount;

    // ── User-entered expected values (read-only) ──
    private int     expectedChequeCount;
    private double  expectedTotalAmount;

    // ── Workflow flags (mutable) ──
    private boolean hasMismatch;
    private boolean hasMicrErrors;

    // ── Cheque list — mutable so composer can remove rejected cheques ──
    private List<OutwardCheque> cheques;

    // ── Error message — non-null only on failure ──
    private String  errorMessage;

    // ════════════════════════════════════════════════════
    //  Factory Methods
    // ════════════════════════════════════════════════════

    /** Builds a success result. */
    public static BatchUploadResult success(String  batchId,
                                             Long    batchDbId,
                                             int     parsedChequeCount,
                                             double  parsedTotalAmount,
                                             int     expectedChequeCount,
                                             double  expectedTotalAmount,
                                             boolean hasMismatch,
                                             boolean hasMicrErrors,
                                             List<OutwardCheque> cheques) {
        BatchUploadResult r = new BatchUploadResult();
        r.batchId             = batchId;
        r.batchDbId           = batchDbId;
        r.parsedChequeCount   = parsedChequeCount;
        r.parsedTotalAmount   = parsedTotalAmount;
        r.expectedChequeCount = expectedChequeCount;
        r.expectedTotalAmount = expectedTotalAmount;
        r.hasMismatch         = hasMismatch;
        r.hasMicrErrors       = hasMicrErrors;
        // Always wrap in a mutable ArrayList so composer can call removeIf()
        r.cheques             = (cheques != null) ? new ArrayList<>(cheques) : new ArrayList<>();
        r.errorMessage        = null;
        return r;
    }

    /** Builds a failure result with only an error message. */
    public static BatchUploadResult failure(String errorMessage) {
        BatchUploadResult r = new BatchUploadResult();
        r.errorMessage = errorMessage;
        r.cheques      = new ArrayList<>();
        return r;
    }

    /** Quick check whether the result is a success or a failure. */
    public boolean isSuccess() {
        return errorMessage == null;
    }

    // ════════════════════════════════════════════════════
    //  Getters
    // ════════════════════════════════════════════════════

    public String              getBatchId()             { return batchId; }
    public Long                getBatchDbId()           { return batchDbId; }
    public int                 getParsedChequeCount()   { return parsedChequeCount; }
    public double              getParsedTotalAmount()   { return parsedTotalAmount; }
    public int                 getExpectedChequeCount() { return expectedChequeCount; }
    public double              getExpectedTotalAmount() { return expectedTotalAmount; }
    public boolean             isHasMismatch()          { return hasMismatch; }
    public boolean             isHasMicrErrors()        { return hasMicrErrors; }
    public List<OutwardCheque> getCheques()             { return cheques; }
    public String              getErrorMessage()        { return errorMessage; }

    // ════════════════════════════════════════════════════
    //  Setters (used by composer to live-update after rejection)
    // ════════════════════════════════════════════════════

    public void setBatchId(String batchId)                   { this.batchId = batchId; }
    public void setBatchDbId(Long batchDbId)                 { this.batchDbId = batchDbId; }
    public void setParsedChequeCount(int v)                  { this.parsedChequeCount = v; }
    public void setParsedTotalAmount(double v)               { this.parsedTotalAmount = v; }
    public void setExpectedChequeCount(int v)                { this.expectedChequeCount = v; }
    public void setExpectedTotalAmount(double v)             { this.expectedTotalAmount = v; }
    public void setHasMismatch(boolean v)                    { this.hasMismatch = v; }
    public void setHasMicrErrors(boolean v)                  { this.hasMicrErrors = v; }
    public void setCheques(List<OutwardCheque> cheques)      { this.cheques = cheques; }
    public void setErrorMessage(String errorMessage)         { this.errorMessage = errorMessage; }
}