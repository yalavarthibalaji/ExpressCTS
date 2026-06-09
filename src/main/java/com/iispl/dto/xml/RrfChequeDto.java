package com.iispl.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * File    : com/iispl/dto/xml/RrfChequeDto.java
 * Purpose : JAXB DTO representing a single cheque entry in RRF.xml.
 *           Includes both accepted and returned/rejected cheques.
 *           Rejection fields (reasonCode, reasonText) are populated only
 *           for returned cheques — they remain null for accepted ones.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class RrfChequeDto {

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

    /**
     * Cheque disposition: ACCEPTED or RETURNED.
     * Matches InwardCheckerAction.action values.
     */
    @XmlElement(name = "CheckerAction")
    private String checkerAction;

    /**
     * NPCI return reason code (e.g. "01" = Funds Insufficient).
     * Populated only for RETURNED cheques; null for ACCEPTED.
     */
    @XmlElement(name = "ReturnReasonCode")
    private String returnReasonCode;

    /**
     * Human-readable return reason text (e.g. "Funds Insufficient").
     * Populated only for RETURNED cheques; null for ACCEPTED.
     */
    @XmlElement(name = "ReturnReasonText")
    private String returnReasonText;

    /**
     * Checker remarks, if any were entered during the return action.
     */
    @XmlElement(name = "Remarks")
    private String remarks;

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

    public String getCheckerAction() { return checkerAction; }
    public void setCheckerAction(String checkerAction) { this.checkerAction = checkerAction; }

    public String getReturnReasonCode() { return returnReasonCode; }
    public void setReturnReasonCode(String returnReasonCode) { this.returnReasonCode = returnReasonCode; }

    public String getReturnReasonText() { return returnReasonText; }
    public void setReturnReasonText(String returnReasonText) { this.returnReasonText = returnReasonText; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
