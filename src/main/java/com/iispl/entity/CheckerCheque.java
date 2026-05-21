package com.iispl.entity;

/**
 * CheckerCheque.java
 * Entity representing a single cheque inside a checker batch.
 *
 * Package  : com.iispl.entity
 * Pattern  : MVC — Entity layer (pure data, no logic)
 *
 * Split into 3 logical groups:
 *   1. Auto-filled from MICR / XML scan
 *   2. Maker-entered fields (highlighted green in UI)
 *   3. Checker-decision fields (set by this module)
 */
public class CheckerCheque {

    // ── Group 1: Auto-filled from MICR / XML ──────────────────────
    private String chequeId;           // internal ID, e.g. "CHQ001"
    private String transactionId;      // e.g. "TXN20250415001"
    private String chequeNumber;       // e.g. "000123456"
    private String bankName;
    private String branchName;
    private String ifscCode;
    private String micrCode;
    private String drawerName;
    private String drawerAccountNumber;
    private String chequeStatus;       // ACTIVE | BOUNCED | STALE
    private String bounceReason;
    private String iqaStatus;          // PASS | FAIL
    private String batchId;            // parent batch

    // ── Group 2: Maker-entered fields ─────────────────────────────
    private long   amountInFigures;
    private String amountInWords;
    private String chequeDate;
    private String presentationDate;
    private String payeeName;
    private String depositorAccount;
    private String makerFlag;          // e.g. "STALE", "HIGH_VALUE", ""
    private String makerRemarks;
    private String makerStatus;        // done | pending

    // ── Group 3: Checker-decision fields ──────────────────────────
    private String checkerStatus;      // pending | approved | rejected
    private String checkerRemarks;
    private boolean reviewed;

    // ── Constructors ────────────────────────────────────────────────

    public CheckerCheque() {
        this.checkerStatus = "pending";
        this.makerStatus   = "done";
        this.reviewed      = false;
    }

    // ── Getters & Setters ────────────────────────────────────────────

    public String getChequeId()                 { return chequeId; }
    public void   setChequeId(String v)         { this.chequeId = v; }

    public String getTransactionId()            { return transactionId; }
    public void   setTransactionId(String v)    { this.transactionId = v; }

    public String getChequeNumber()             { return chequeNumber; }
    public void   setChequeNumber(String v)     { this.chequeNumber = v; }

    public String getBankName()                 { return bankName; }
    public void   setBankName(String v)         { this.bankName = v; }

    public String getBranchName()               { return branchName; }
    public void   setBranchName(String v)       { this.branchName = v; }

    public String getIfscCode()                 { return ifscCode; }
    public void   setIfscCode(String v)         { this.ifscCode = v; }

    public String getMicrCode()                 { return micrCode; }
    public void   setMicrCode(String v)         { this.micrCode = v; }

    public String getDrawerName()               { return drawerName; }
    public void   setDrawerName(String v)       { this.drawerName = v; }

    public String getDrawerAccountNumber()      { return drawerAccountNumber; }
    public void   setDrawerAccountNumber(String v){ this.drawerAccountNumber = v; }

    public String getChequeStatus()             { return chequeStatus; }
    public void   setChequeStatus(String v)     { this.chequeStatus = v; }

    public String getBounceReason()             { return bounceReason; }
    public void   setBounceReason(String v)     { this.bounceReason = v; }

    public String getIqaStatus()                { return iqaStatus; }
    public void   setIqaStatus(String v)        { this.iqaStatus = v; }

    public String getBatchId()                  { return batchId; }
    public void   setBatchId(String v)          { this.batchId = v; }

    public long   getAmountInFigures()          { return amountInFigures; }
    public void   setAmountInFigures(long v)    { this.amountInFigures = v; }

    public String getAmountInWords()            { return amountInWords; }
    public void   setAmountInWords(String v)    { this.amountInWords = v; }

    public String getChequeDate()               { return chequeDate; }
    public void   setChequeDate(String v)       { this.chequeDate = v; }

    public String getPresentationDate()         { return presentationDate; }
    public void   setPresentationDate(String v) { this.presentationDate = v; }

    public String getPayeeName()                { return payeeName; }
    public void   setPayeeName(String v)        { this.payeeName = v; }

    public String getDepositorAccount()         { return depositorAccount; }
    public void   setDepositorAccount(String v) { this.depositorAccount = v; }

    public String getMakerFlag()                { return makerFlag; }
    public void   setMakerFlag(String v)        { this.makerFlag = v; }

    public String getMakerRemarks()             { return makerRemarks; }
    public void   setMakerRemarks(String v)     { this.makerRemarks = v; }

    public String getMakerStatus()              { return makerStatus; }
    public void   setMakerStatus(String v)      { this.makerStatus = v; }

    public String getCheckerStatus()            { return checkerStatus; }
    public void   setCheckerStatus(String v)    { this.checkerStatus = v; }

    public String getCheckerRemarks()           { return checkerRemarks; }
    public void   setCheckerRemarks(String v)   { this.checkerRemarks = v; }

    public boolean isReviewed()                 { return reviewed; }
    public void    setReviewed(boolean v)       { this.reviewed = v; }

    /** True if amount >= 5,00,000 — shown as High Value warning in UI. */
    public boolean isHighValue() {
        return amountInFigures >= 500000;
    }

    /** Formatted amount string for display, e.g. "₹2,50,000" */
    public String getFormattedAmount() {
        // Simple Indian comma format
        return "\u20B9" + String.format("%,d", amountInFigures)
                               .replace(",", ",");
    }

    @Override
    public String toString() {
        return "CheckerCheque{chequeId='" + chequeId + "', chequeNumber='" + chequeNumber +
               "', checkerStatus='" + checkerStatus + "'}";
    }
}
