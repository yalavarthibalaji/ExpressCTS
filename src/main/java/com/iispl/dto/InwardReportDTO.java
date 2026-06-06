package com.iispl.dto;

import java.math.BigDecimal;

/**
 * File : com/iispl/dto/InwardReportDTO.java Purpose : Data transfer object for
 * one row of the Checker Inward Reports grid.
 *
 * Columns displayed: batchId → inward_batch.batch_id batchDate →
 * inward_batch.batch_date (formatted dd/MM/yyyy) totalCheques →
 * inward_batch.total_cheques totalAmount → inward_batch.total_amount status →
 * inward_batch.status (Pending | Processing | Completed | Failed)
 *
 * debitEligible flag drives the "Generate to Debit" button enable/disable
 * state.
 */
public class InwardReportDTO {

	// ── Display fields ────────────────────────────────────────────────────────
	private String batchId;
	private String batchDate; // formatted dd/MM/yyyy
	private int totalCheques;
	private BigDecimal totalAmount;
	private String status; // Pending | Processing | Completed | Failed

	// ── Business logic flag ───────────────────────────────────────────────────
	/** True when the batch is eligible for Generate to Debit (status = PENDING). */
	private boolean debitEligible;

	// ── Constructors ──────────────────────────────────────────────────────────

	public InwardReportDTO() {
	}

	// ── Getters & Setters ─────────────────────────────────────────────────────

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public String getBatchDate() {
		return batchDate;
	}

	public void setBatchDate(String batchDate) {
		this.batchDate = batchDate;
	}

	public int getTotalCheques() {
		return totalCheques;
	}

	public void setTotalCheques(int totalCheques) {
		this.totalCheques = totalCheques;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isDebitEligible() {
		return debitEligible;
	}

	public void setDebitEligible(boolean debitEligible) {
		this.debitEligible = debitEligible;
	}

	@Override
	public String toString() {
		return "InwardReportDTO{batchId='" + batchId + "', status='" + status + "', totalCheques=" + totalCheques
				+ ", totalAmount=" + totalAmount + "}";
	}
}