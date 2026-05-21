package com.iispl.entity;

/**
 * OutwardCheque.java Entity class representing one cheque in the outward
 * clearing batch.
 *
 * Package : com.iispl.entity Pattern : MVC — Entity / Model layer
 *
 * Fields are mapped 1-to-1 with XML tag names from the scanner file. Additional
 * runtime fields (makerStatus, checkerStatus, etc.) track workflow state.
 */
public class OutwardChequeDummy {

	// ── XML / Scanner fields (auto-read from image/MICR) ────────────
	private String id; // e.g. "CHQ001"
	private String transactionId; // e.g. "TXN20250415001"
	private String chequeNumber; // e.g. "000123456"
	private String bankName; // e.g. "State Bank of India"
	private String branchName; // e.g. "MG Road Branch"
	private String ifscCode; // e.g. "SBIN0001234"
	private String micrCode; // e.g. "600002010"
	private String chequeDate; // e.g. "15-Apr-2025"
	private String presentationDate; // e.g. "15-Apr-2025"
	private String drawerName; // e.g. "Rajesh Kumar"
	private String drawerAccountNumber; // e.g. "50100123456789"
	private String payeeName; // e.g. "ABC Traders"
	private String amountInWords; // e.g. "Two Lakh Fifty Thousand Rupees Only"
	private long amountInFigures; // e.g. 250000
	private String chequeStatus; // "ACTIVE" | "BOUNCED" | "STALE"
	private String bounceReason; // populated if chequeStatus == BOUNCED
	private String depositorAcc; // depositing account number
	private String iqaStatus; // "PASS" | "FAIL"

	// ── Batch assignment ─────────────────────────────────────────────
	private String batchId; // which batch this cheque belongs to

	// ── Workflow state ────────────────────────────────────────────────
	private String makerStatus; // "pending" | "done"
	private String checkerStatus; // "pending" | "approved" | "rejected"
	private boolean reviewed;
	private String makerRemarks;
	private String checkerRemarks;
	private String makerFlag; // e.g. "stale", "postdated", "mismatch"

	// ── IQA exception flags ──────────────────────────────────────────
	private boolean iqaManualEntry; // true if this cheque failed IQA → manual entry
	private String iqaOfficerRemarks; // remarks from IQA officer

	// ================================================================
	// Constructors
	// ================================================================
	public OutwardChequeDummy() {
	}

	// ================================================================
	// Getters & Setters
	// ================================================================
	public String getId() {
		return id;
	}

	public void setId(String v) {
		this.id = v;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String v) {
		this.transactionId = v;
	}

	public String getChequeNumber() {
		return chequeNumber;
	}

	public void setChequeNumber(String v) {
		this.chequeNumber = v;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String v) {
		this.bankName = v;
	}

	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String v) {
		this.branchName = v;
	}

	public String getIfscCode() {
		return ifscCode;
	}

	public void setIfscCode(String v) {
		this.ifscCode = v;
	}

	public String getMicrCode() {
		return micrCode;
	}

	public void setMicrCode(String v) {
		this.micrCode = v;
	}

	public String getChequeDate() {
		return chequeDate;
	}

	public void setChequeDate(String v) {
		this.chequeDate = v;
	}

	public String getPresentationDate() {
		return presentationDate;
	}

	public void setPresentationDate(String v) {
		this.presentationDate = v;
	}

	public String getDrawerName() {
		return drawerName;
	}

	public void setDrawerName(String v) {
		this.drawerName = v;
	}

	public String getDrawerAccountNumber() {
		return drawerAccountNumber;
	}

	public void setDrawerAccountNumber(String v) {
		this.drawerAccountNumber = v;
	}

	public String getPayeeName() {
		return payeeName;
	}

	public void setPayeeName(String v) {
		this.payeeName = v;
	}

	public String getAmountInWords() {
		return amountInWords;
	}

	public void setAmountInWords(String v) {
		this.amountInWords = v;
	}

	public long getAmountInFigures() {
		return amountInFigures;
	}

	public void setAmountInFigures(long v) {
		this.amountInFigures = v;
	}

	public String getChequeStatus() {
		return chequeStatus;
	}

	public void setChequeStatus(String v) {
		this.chequeStatus = v;
	}

	public String getBounceReason() {
		return bounceReason;
	}

	public void setBounceReason(String v) {
		this.bounceReason = v;
	}

	public String getDepositorAcc() {
		return depositorAcc;
	}

	public void setDepositorAcc(String v) {
		this.depositorAcc = v;
	}

	public String getIqaStatus() {
		return iqaStatus;
	}

	public void setIqaStatus(String v) {
		this.iqaStatus = v;
	}

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String v) {
		this.batchId = v;
	}

	public String getMakerStatus() {
		return makerStatus;
	}

	public void setMakerStatus(String v) {
		this.makerStatus = v;
	}

	public String getCheckerStatus() {
		return checkerStatus;
	}

	public void setCheckerStatus(String v) {
		this.checkerStatus = v;
	}

	public boolean isReviewed() {
		return reviewed;
	}

	public void setReviewed(boolean v) {
		this.reviewed = v;
	}

	public String getMakerRemarks() {
		return makerRemarks;
	}

	public void setMakerRemarks(String v) {
		this.makerRemarks = v;
	}

	public String getCheckerRemarks() {
		return checkerRemarks;
	}

	public void setCheckerRemarks(String v) {
		this.checkerRemarks = v;
	}

	public String getMakerFlag() {
		return makerFlag;
	}

	public void setMakerFlag(String v) {
		this.makerFlag = v;
	}

	public boolean isIqaManualEntry() {
		return iqaManualEntry;
	}

	public void setIqaManualEntry(boolean v) {
		this.iqaManualEntry = v;
	}

	public String getIqaOfficerRemarks() {
		return iqaOfficerRemarks;
	}

	public void setIqaOfficerRemarks(String v) {
		this.iqaOfficerRemarks = v;
	}

	/** Convenience: true if amount >= 5,00,000 (High Value threshold) */
	public boolean isHighValue() {
		return amountInFigures >= 500000;
	}

	@Override
	public String toString() {
		return "OutwardCheque{id='" + id + "', chequeNumber='" + chequeNumber + "', amount=" + amountInFigures
				+ ", makerStatus='" + makerStatus + "'}";
	}
}