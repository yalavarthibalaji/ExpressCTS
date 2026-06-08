package com.iispl.dto.xml.cxf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.math.BigDecimal;

/**
 * File : com/iispl/dto/xml/cxf/CxfChequeXml.java Purpose : Maps a single
 * <Cheque> element inside the CXF ChequeList.
 *
 * Every cheque in a checker-approved batch is exported as this structure. Only
 * CHECKER_PASSED cheques are included — rejected and referred cheques are
 * excluded from the outgoing file (per NPCI spec).
 *
 * XML produced: <Cheque> <SeqNo>1</SeqNo> <ChequeNo>123456</ChequeNo>
 * <MicrCode>...</MicrCode> <CityCode>987</CityCode> <BankCode>321</BankCode>
 * <BranchCode>098</BranchCode> <BaseNumber>456789</BaseNumber>
 * <TransactionCode>10</TransactionCode> <AccountNo>8896532164956784</AccountNo>
 * <AccountHolder>B. Saraswathi</AccountHolder> <Amount>5000.00</Amount>
 * <ChequeDate>2026-06-08</ChequeDate> <PayeeName>Self</PayeeName>
 * <FrontImageRef>001-front.jpg</FrontImageRef>
 * <BackImageRef>001-back.jpg</BackImageRef> <IqaStatus>PASS</IqaStatus>
 * </Cheque>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CxfChequeXml {

	@XmlElement(name = "SeqNo", namespace = "urn:npci:cts:cxf:v1.0")
	private int seqNo;

	@XmlElement(name = "ChequeNo", namespace = "urn:npci:cts:cxf:v1.0")
	private String chequeNo;

	@XmlElement(name = "MicrCode", namespace = "urn:npci:cts:cxf:v1.0")
	private String micrCode;

	@XmlElement(name = "CityCode", namespace = "urn:npci:cts:cxf:v1.0")
	private String cityCode;

	@XmlElement(name = "BankCode", namespace = "urn:npci:cts:cxf:v1.0")
	private String bankCode;

	@XmlElement(name = "BranchCode", namespace = "urn:npci:cts:cxf:v1.0")
	private String branchCode;

	@XmlElement(name = "BaseNumber", namespace = "urn:npci:cts:cxf:v1.0")
	private String baseNumber;

	@XmlElement(name = "TransactionCode", namespace = "urn:npci:cts:cxf:v1.0")
	private String transactionCode;

	@XmlElement(name = "AccountNo", namespace = "urn:npci:cts:cxf:v1.0")
	private String accountNo;

	@XmlElement(name = "AccountHolder", namespace = "urn:npci:cts:cxf:v1.0")
	private String accountHolder;

	@XmlElement(name = "Amount", namespace = "urn:npci:cts:cxf:v1.0")
	private BigDecimal amount;

	@XmlElement(name = "ChequeDate", namespace = "urn:npci:cts:cxf:v1.0")
	private String chequeDate; // YYYY-MM-DD

	@XmlElement(name = "PayeeName", namespace = "urn:npci:cts:cxf:v1.0")
	private String payeeName;

	@XmlElement(name = "FrontImageRef", namespace = "urn:npci:cts:cxf:v1.0")
	private String frontImageRef;

	@XmlElement(name = "BackImageRef", namespace = "urn:npci:cts:cxf:v1.0")
	private String backImageRef;

	@XmlElement(name = "IqaStatus", namespace = "urn:npci:cts:cxf:v1.0")
	private String iqaStatus;

	// ── Getters and Setters ──
	public int getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(int n) {
		this.seqNo = n;
	}

	public String getChequeNo() {
		return chequeNo;
	}

	public void setChequeNo(String s) {
		this.chequeNo = s;
	}

	public String getMicrCode() {
		return micrCode;
	}

	public void setMicrCode(String s) {
		this.micrCode = s;
	}

	public String getCityCode() {
		return cityCode;
	}

	public void setCityCode(String s) {
		this.cityCode = s;
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

	public String getBaseNumber() {
		return baseNumber;
	}

	public void setBaseNumber(String s) {
		this.baseNumber = s;
	}

	public String getTransactionCode() {
		return transactionCode;
	}

	public void setTransactionCode(String s) {
		this.transactionCode = s;
	}

	public String getAccountNo() {
		return accountNo;
	}

	public void setAccountNo(String s) {
		this.accountNo = s;
	}

	public String getAccountHolder() {
		return accountHolder;
	}

	public void setAccountHolder(String s) {
		this.accountHolder = s;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal a) {
		this.amount = a;
	}

	public String getChequeDate() {
		return chequeDate;
	}

	public void setChequeDate(String s) {
		this.chequeDate = s;
	}

	public String getPayeeName() {
		return payeeName;
	}

	public void setPayeeName(String s) {
		this.payeeName = s;
	}

	public String getFrontImageRef() {
		return frontImageRef;
	}

	public void setFrontImageRef(String s) {
		this.frontImageRef = s;
	}

	public String getBackImageRef() {
		return backImageRef;
	}

	public void setBackImageRef(String s) {
		this.backImageRef = s;
	}

	public String getIqaStatus() {
		return iqaStatus;
	}

	public void setIqaStatus(String s) {
		this.iqaStatus = s;
	}
}