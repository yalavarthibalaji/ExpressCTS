package com.iispl.dto;

import java.math.BigDecimal;

/**
 * File : com/iispl/dto/CbsValidationResult.java Holds CBS lookup result fetched
 * from Firebase Realtime Database.
 */
public class CbsValidationResult {

	private boolean found = false;
	private String accountHolder = null;
	private String bankCode = null;
	private String branchCode = null;
	private BigDecimal balance = BigDecimal.ZERO;
	private boolean isActive = false;
	private String accountType = null;

	// --- getters / setters ---
	public boolean isFound() {
		return found;
	}

	public void setFound(boolean f) {
		this.found = f;
	}

	public String getAccountHolder() {
		return accountHolder;
	}

	public void setAccountHolder(String s) {
		this.accountHolder = s;
	}

	public String getBankCode() {
		return bankCode;
	}

	public void setBankCode(String s) {
		this.bankCode = s;
	}

	public String getBranchCode() {
		return branchCode;
	}

	public void setBranchCode(String s) {
		this.branchCode = s;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal b) {
		this.balance = b;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean a) {
		this.isActive = a;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String s) {
		this.accountType = s;
	}
}