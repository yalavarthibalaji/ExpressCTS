package com.iispl.jaxb;

import com.iispl.util.LocalDateAdapter;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.math.BigDecimal;
import java.time.LocalDate;

@XmlAccessorType(XmlAccessType.FIELD)
public class BpxfCheque {

    @XmlElement(name = "SeqNo")
    private int seqNo;

    @XmlElement(name = "ChequeNo")
    private String chequeNo;

    @XmlElement(name = "ChequeDate")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate chequeDate;

    @XmlElement(name = "Amount")
    private BigDecimal amount;

    @XmlElement(name = "ChequeDateOcr")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate chequeDateOcr;

    @XmlElement(name = "AmountOcr")
    private BigDecimal amountOcr;

    @XmlElement(name = "AmountInWords")
    private String amountInWords;

    @XmlElement(name = "MicrCodeRaw")
    private String micrCodeRaw;

    @XmlElement(name = "MicrCodeCorrected")
    private String micrCodeCorrected;

    @XmlElement(name = "IsMicrError")
    private boolean isMicrError;
    
    @XmlElement(name = "IsDateAmountError")
    private boolean isDateAmountError;

    public boolean isDateAmountError() {
		return isDateAmountError;
	}
	public void setDateAmountError(boolean isDateAmountError) {
		this.isDateAmountError = isDateAmountError;
	}
	@XmlElement(name = "CityCode")
    private String cityCode;

    @XmlElement(name = "BankCode")
    private String bankCode;

    @XmlElement(name = "BranchCode")
    private String branchCode;

    @XmlElement(name = "DraweeAccountNumber")
    private String draweeAccountNumber;

    @XmlElement(name = "DraweeAccountHolder")
    private String draweeAccountHolder;

    @XmlElement(name = "AccountBalance")
    private BigDecimal accountBalance;

    @XmlElement(name = "IsAccountValid")
    private Boolean isAccountValid;

    @XmlElement(name = "IsBankMatched")
    private Boolean isBankMatched;

    @XmlElement(name = "PayeeName")
    private String payeeName;

    @XmlElement(name = "IqaStatus")
    private String iqaStatus;

    @XmlElement(name = "FrontImagePath")
    private String frontImagePath;

    @XmlElement(name = "BackImagePath")
    private String backImagePath;

    @XmlElement(name = "Remarks")
    private String remarks;

    // ── Getters and Setters ──

    public int getSeqNo()                                          { return seqNo; }
    public void setSeqNo(int seqNo)                                { this.seqNo = seqNo; }

    public String getChequeNo()                                    { return chequeNo; }
    public void setChequeNo(String chequeNo)                       { this.chequeNo = chequeNo; }

    public LocalDate getChequeDate()                               { return chequeDate; }
    public void setChequeDate(LocalDate chequeDate)                { this.chequeDate = chequeDate; }

    public BigDecimal getAmount()                                  { return amount; }
    public void setAmount(BigDecimal amount)                       { this.amount = amount; }

    public LocalDate getChequeDateOcr()                            { return chequeDateOcr; }
    public void setChequeDateOcr(LocalDate chequeDateOcr)          { this.chequeDateOcr = chequeDateOcr; }

    public BigDecimal getAmountOcr()                               { return amountOcr; }
    public void setAmountOcr(BigDecimal amountOcr)                 { this.amountOcr = amountOcr; }

    public String getAmountInWords()                               { return amountInWords; }
    public void setAmountInWords(String amountInWords)             { this.amountInWords = amountInWords; }

    public String getMicrCodeRaw()                                 { return micrCodeRaw; }
    public void setMicrCodeRaw(String micrCodeRaw)                 { this.micrCodeRaw = micrCodeRaw; }

    public String getMicrCodeCorrected()                           { return micrCodeCorrected; }
    public void setMicrCodeCorrected(String micrCodeCorrected)     { this.micrCodeCorrected = micrCodeCorrected; }

    public boolean isMicrError()                                   { return isMicrError; }
    public void setMicrError(boolean micrError)                    { this.isMicrError = micrError; }

    public String getCityCode()                                    { return cityCode; }
    public void setCityCode(String cityCode)                       { this.cityCode = cityCode; }

    public String getBankCode()                                    { return bankCode; }
    public void setBankCode(String bankCode)                       { this.bankCode = bankCode; }

    public String getBranchCode()                                  { return branchCode; }
    public void setBranchCode(String branchCode)                   { this.branchCode = branchCode; }

    public String getDraweeAccountNumber()                         { return draweeAccountNumber; }
    public void setDraweeAccountNumber(String draweeAccountNumber) { this.draweeAccountNumber = draweeAccountNumber; }

    public String getDraweeAccountHolder()                         { return draweeAccountHolder; }
    public void setDraweeAccountHolder(String draweeAccountHolder) { this.draweeAccountHolder = draweeAccountHolder; }

    public BigDecimal getAccountBalance()                          { return accountBalance; }
    public void setAccountBalance(BigDecimal accountBalance)       { this.accountBalance = accountBalance; }

    public Boolean getIsAccountValid()                             { return isAccountValid; }
    public void setIsAccountValid(Boolean isAccountValid)          { this.isAccountValid = isAccountValid; }

    public Boolean getIsBankMatched()                              { return isBankMatched; }
    public void setIsBankMatched(Boolean isBankMatched)            { this.isBankMatched = isBankMatched; }

    public String getPayeeName()                                   { return payeeName; }
    public void setPayeeName(String payeeName)                     { this.payeeName = payeeName; }

    public String getIqaStatus()                                   { return iqaStatus; }
    public void setIqaStatus(String iqaStatus)                     { this.iqaStatus = iqaStatus; }

    public String getFrontImagePath()                              { return frontImagePath; }
    public void setFrontImagePath(String frontImagePath)           { this.frontImagePath = frontImagePath; }

    public String getBackImagePath()                               { return backImagePath; }
    public void setBackImagePath(String backImagePath)             { this.backImagePath = backImagePath; }

    public String getRemarks()                                     { return remarks; }
    public void setRemarks(String remarks)                         { this.remarks = remarks; }
}