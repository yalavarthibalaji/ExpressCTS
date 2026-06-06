package com.iispl.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Flat DTO for InwardCheque — used in ViewBatchesComposer (inward).
 * Avoids LazyInitializationException by detaching from Hibernate session.
 */
public class InwardChequeDto {

    private Long       id;
    private int        seqNo;
    private String     chequeNo;
    private LocalDate  chequeDate;
    private BigDecimal amount;
    private String     amountInWords;
    private LocalDate  chequeDateOcr;
    private BigDecimal amountOcr;
    private String     cityCode;
    private String     bankCode;
    private String     branchCode;
    private String     micrCodeRaw;
    private String     micrCodeCorrected;
    private boolean    isMicrError;
    private String     presentingBankCode;
    private String     presentingBankName;
    private String     draweeAccountNumber;
    private String     draweeAccountHolder;
    private BigDecimal accountBalance;
    private Boolean    isAccountValid;
    private Boolean    isBankMatched;
    private String     payeeName;
    private String     iqaStatus;
    private String     status;
    private String     repairStatus;
    private String     frontImagePath;
    private String     backImagePath;
    private String     remarks;

    public InwardChequeDto() {}

    /** Factory method — maps entity to DTO inside an open Hibernate session. */
    public static InwardChequeDto from(com.iispl.entity.inward.InwardCheque e) {
        InwardChequeDto d = new InwardChequeDto();
        d.id                 = e.getId();
        d.seqNo              = e.getSeqNo();
        d.chequeNo           = e.getChequeNo();
        d.chequeDate         = e.getChequeDate();
        d.amount             = e.getAmount();
        d.amountInWords      = e.getAmountInWords();
        d.chequeDateOcr      = e.getChequeDateOcr();
        d.amountOcr          = e.getAmountOcr();
        d.cityCode           = e.getCityCode();
        d.bankCode           = e.getBankCode();
        d.branchCode         = e.getBranchCode();
        d.micrCodeRaw        = e.getMicrCodeRaw();
        d.micrCodeCorrected  = e.getMicrCodeCorrected();
        d.isMicrError        = e.isMicrError();
        d.presentingBankCode = e.getPresentingBankCode();
        d.presentingBankName = e.getPresentingBankName();
        d.draweeAccountNumber= e.getDraweeAccountNumber();
        d.draweeAccountHolder= e.getDraweeAccountHolder();
        d.accountBalance     = e.getAccountBalance();
        d.isAccountValid     = e.getIsAccountValid();
        d.isBankMatched      = e.getIsBankMatched();
        d.payeeName          = e.getPayeeName();
        d.iqaStatus          = e.getIqaStatus();
        d.status             = e.getStatus();
        d.repairStatus       = e.getRepairStatus();
        d.frontImagePath     = e.getFrontImagePath();
        d.backImagePath      = e.getBackImagePath();
        d.remarks            = e.getRemarks();
        return d;
    }

    // ── Getters ──

    public Long       getId()                 { return id; }
    public int        getSeqNo()              { return seqNo; }
    public String     getChequeNo()           { return chequeNo; }
    public LocalDate  getChequeDate()         { return chequeDate; }
    public BigDecimal getAmount()             { return amount; }
    public String     getAmountInWords()      { return amountInWords; }
    public LocalDate  getChequeDateOcr()      { return chequeDateOcr; }
    public BigDecimal getAmountOcr()          { return amountOcr; }
    public String     getCityCode()           { return cityCode; }
    public String     getBankCode()           { return bankCode; }
    public String     getBranchCode()         { return branchCode; }
    public String     getMicrCodeRaw()        { return micrCodeRaw; }
    public String     getMicrCodeCorrected()  { return micrCodeCorrected; }
    public boolean    isMicrError()           { return isMicrError; }
    public String     getPresentingBankCode() { return presentingBankCode; }
    public String     getPresentingBankName() { return presentingBankName; }
    public String     getDraweeAccountNumber(){ return draweeAccountNumber; }
    public String     getDraweeAccountHolder(){ return draweeAccountHolder; }
    public BigDecimal getAccountBalance()     { return accountBalance; }
    public Boolean    getIsAccountValid()     { return isAccountValid; }
    public Boolean    getIsBankMatched()      { return isBankMatched; }
    public String     getPayeeName()          { return payeeName; }
    public String     getIqaStatus()          { return iqaStatus; }
    public String     getStatus()             { return status; }
    public String     getRepairStatus()       { return repairStatus; }
    public String     getFrontImagePath()     { return frontImagePath; }
    public String     getBackImagePath()      { return backImagePath; }
    public String     getRemarks()            { return remarks; }
}