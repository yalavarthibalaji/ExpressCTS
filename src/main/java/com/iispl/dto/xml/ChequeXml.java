package com.iispl.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * File    : com/iispl/dto/xml/ChequeXml.java
 * Purpose : Maps a single <cheque> element from the XML file.
 *           Every field in the XML has a matching field here.
 *           JAXB automatically fills these values during parsing.
 *
 * XML maps to:
 *   <cheque>
 *       <chequeNo>000123</chequeNo>
 *       <micrCode>00012360000200112345612</micrCode>
 *       <cityCode>600</cityCode>
 *       <bankCode>002</bankCode>
 *       <branchCode>001</branchCode>
 *       <baseNumber>123456</baseNumber>
 *       <transactionCode>12</transactionCode>
 *       <amount>50000.00</amount>
 *       <chequeDate>2026-06-01</chequeDate>
 *       <accountNo>1234567890</accountNo>
 *       <payeeName>Rajesh Kumar</payeeName>
 *       <iqaStatus>PASS</iqaStatus>
 *       <frontImage>images/CHQ001_FRONT.jpg</frontImage>
 *       <backImage>images/CHQ001_BACK.jpg</backImage>
 *   </cheque>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ChequeXml {

    /** 6-digit cheque number */
    @XmlElement(name = "chequeNo")
    private String chequeNo;

    /**
     * Full 23-character MICR band from scanner.
     * Format: chequeNo(6) + cityCode(3) + bankCode(3)
     *         + branchCode(3) + baseNumber(6) + transactionCode(2)
     */
    @XmlElement(name = "micrCode")
    private String micrCode;

    /** 3-digit city code */
    @XmlElement(name = "cityCode")
    private String cityCode;

    /** 3-digit bank code */
    @XmlElement(name = "bankCode")
    private String bankCode;

    /** 3-digit branch code */
    @XmlElement(name = "branchCode")
    private String branchCode;

    /** 6-digit base number */
    @XmlElement(name = "baseNumber")
    private String baseNumber;

    /** 2-digit transaction code */
    @XmlElement(name = "transactionCode")
    private String transactionCode;

    /** Cheque amount */
    @XmlElement(name = "amount")
    private double amount;

    /** Cheque date in YYYY-MM-DD format */
    @XmlElement(name = "chequeDate")
    private String chequeDate;

    /** Account number to be debited */
    @XmlElement(name = "accountNo")
    private String accountNo;

    /** Payee name on the cheque */
    @XmlElement(name = "payeeName")
    private String payeeName;
    
    /**
     * MICR error flag from XML.
     * Values: PASS or FAIL
     * FAIL means 2 sub-fields in the MICR band have wrong values.
     * Maker will correct them manually by viewing the cheque image.
     */
    @XmlElement(name = "isMicrError")
    private String isMicrError;

    /**
     * IQA status set by scanner software in the XML itself.
     * Values: PASS or FAIL
     * No image-processing code needed — we read this directly.
     */
    @XmlElement(name = "iqaStatus")
    private String iqaStatus;

    /** Relative path to front image inside the ZIP (e.g. images/CHQ001_FRONT.jpg) */
    @XmlElement(name = "frontImage")
    private String frontImage;

    /** Relative path to back image inside the ZIP (e.g. images/CHQ001_BACK.jpg) */
    @XmlElement(name = "backImage")
    private String backImage;

    // ── Getters and Setters ──

    public String getChequeNo() {
        return chequeNo;
    }

    public void setChequeNo(String chequeNo) {
        this.chequeNo = chequeNo;
    }

    public String getMicrCode() {
        return micrCode;
    }

    public void setMicrCode(String micrCode) {
        this.micrCode = micrCode;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getBaseNumber() {
        return baseNumber;
    }

    public void setBaseNumber(String baseNumber) {
        this.baseNumber = baseNumber;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getChequeDate() {
        return chequeDate;
    }

    public void setChequeDate(String chequeDate) {
        this.chequeDate = chequeDate;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getIqaStatus() {
        return iqaStatus;
    }

    public void setIqaStatus(String iqaStatus) {
        this.iqaStatus = iqaStatus;
    }

    public String getFrontImage() {
        return frontImage;
    }

    public void setFrontImage(String frontImage) {
        this.frontImage = frontImage;
    }

    public String getBackImage() {
        return backImage;
    }

    public void setBackImage(String backImage) {
        this.backImage = backImage;
    }
    
    public String getIsMicrError() { return isMicrError; }
    public void setIsMicrError(String isMicrError) { this.isMicrError = isMicrError; }
}