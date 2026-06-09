package com.iispl.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * File    : com/iispl/dto/xml/AckChequeDto.java
 * Purpose : JAXB DTO representing a single accepted cheque entry in ACK.xml.
 *           Only accepted cheques are included in the ACK file.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AckChequeDto {

    @XmlElement(name = "SeqNo")
    private int seqNo;

    @XmlElement(name = "ChequeNo")
    private String chequeNo;

    @XmlElement(name = "ChequeDate")
    private String chequeDate;

    @XmlElement(name = "Amount")
    private String amount;

    @XmlElement(name = "MicrCode")
    private String micrCode;

    @XmlElement(name = "CityCode")
    private String cityCode;

    @XmlElement(name = "BankCode")
    private String bankCode;

    @XmlElement(name = "BranchCode")
    private String branchCode;

    @XmlElement(name = "PresentingBankCode")
    private String presentingBankCode;

    @XmlElement(name = "PresentingBankName")
    private String presentingBankName;

    @XmlElement(name = "DraweeAccountNumber")
    private String draweeAccountNumber;

    @XmlElement(name = "DraweeAccountHolder")
    private String draweeAccountHolder;

    @XmlElement(name = "PayeeName")
    private String payeeName;

    @XmlElement(name = "Status")
    private String status;

    // ── Getters and Setters ──────────────────────────────────────────────────

    public int getSeqNo() { return seqNo; }
    public void setSeqNo(int seqNo) { this.seqNo = seqNo; }

    public String getChequeNo() { return chequeNo; }
    public void setChequeNo(String chequeNo) { this.chequeNo = chequeNo; }

    public String getChequeDate() { return chequeDate; }
    public void setChequeDate(String chequeDate) { this.chequeDate = chequeDate; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getMicrCode() { return micrCode; }
    public void setMicrCode(String micrCode) { this.micrCode = micrCode; }

    public String getCityCode() { return cityCode; }
    public void setCityCode(String cityCode) { this.cityCode = cityCode; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getPresentingBankCode() { return presentingBankCode; }
    public void setPresentingBankCode(String presentingBankCode) { this.presentingBankCode = presentingBankCode; }

    public String getPresentingBankName() { return presentingBankName; }
    public void setPresentingBankName(String presentingBankName) { this.presentingBankName = presentingBankName; }

    public String getDraweeAccountNumber() { return draweeAccountNumber; }
    public void setDraweeAccountNumber(String draweeAccountNumber) { this.draweeAccountNumber = draweeAccountNumber; }

    public String getDraweeAccountHolder() { return draweeAccountHolder; }
    public void setDraweeAccountHolder(String draweeAccountHolder) { this.draweeAccountHolder = draweeAccountHolder; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
