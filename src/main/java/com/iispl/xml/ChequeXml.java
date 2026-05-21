package com.iispl.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

// maps to each <Cheque> inside <Cheques>
@XmlRootElement(name = "Cheque")
public class ChequeXml {

    private String chequeNumber;
    private String bankName;
    private String branchName;
    private String ifscCode;
    private String micrCode;
    private String micrStatus;
    private String chequeDate;
    private String presentationDate;
    private String drawerName;
    private String drawerAccountNumber;
    private String payeeName;
    private String amountInWords;
    private long amountInFigures;
    private String depositorAccountNumber;
    private String imageFrontPath;
    private String imageBackPath;
    private String iqaStatus;

    @XmlElement(name = "ChequeNumber")
    public String getChequeNumber() {
        return chequeNumber;
    }

    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }

    @XmlElement(name = "BankName")
    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    @XmlElement(name = "BranchName")
    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    @XmlElement(name = "IfscCode")
    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    @XmlElement(name = "MicrCode")
    public String getMicrCode() {
        return micrCode;
    }

    public void setMicrCode(String micrCode) {
        this.micrCode = micrCode;
    }

    @XmlElement(name = "MicrStatus")
    public String getMicrStatus() {
        return micrStatus;
    }

    public void setMicrStatus(String micrStatus) {
        this.micrStatus = micrStatus;
    }

    @XmlElement(name = "ChequeDate")
    public String getChequeDate() {
        return chequeDate;
    }

    public void setChequeDate(String chequeDate) {
        this.chequeDate = chequeDate;
    }

    @XmlElement(name = "PresentationDate")
    public String getPresentationDate() {
        return presentationDate;
    }

    public void setPresentationDate(String presentationDate) {
        this.presentationDate = presentationDate;
    }

    @XmlElement(name = "DrawerName")
    public String getDrawerName() {
        return drawerName;
    }

    public void setDrawerName(String drawerName) {
        this.drawerName = drawerName;
    }

    @XmlElement(name = "DrawerAccountNumber")
    public String getDrawerAccountNumber() {
        return drawerAccountNumber;
    }

    public void setDrawerAccountNumber(String drawerAccountNumber) {
        this.drawerAccountNumber = drawerAccountNumber;
    }

    @XmlElement(name = "PayeeName")
    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    @XmlElement(name = "AmountInWords")
    public String getAmountInWords() {
        return amountInWords;
    }

    public void setAmountInWords(String amountInWords) {
        this.amountInWords = amountInWords;
    }

    @XmlElement(name = "AmountInFigures")
    public long getAmountInFigures() {
        return amountInFigures;
    }

    public void setAmountInFigures(long amountInFigures) {
        this.amountInFigures = amountInFigures;
    }

    @XmlElement(name = "DepositorAccountNumber")
    public String getDepositorAccountNumber() {
        return depositorAccountNumber;
    }

    public void setDepositorAccountNumber(String depositorAccountNumber) {
        this.depositorAccountNumber = depositorAccountNumber;
    }

    @XmlElement(name = "ImageFrontPath")
    public String getImageFrontPath() {
        return imageFrontPath;
    }

    public void setImageFrontPath(String imageFrontPath) {
        this.imageFrontPath = imageFrontPath;
    }

    @XmlElement(name = "ImageBackPath")
    public String getImageBackPath() {
        return imageBackPath;
    }

    public void setImageBackPath(String imageBackPath) {
        this.imageBackPath = imageBackPath;
    }

    @XmlElement(name = "IqaStatus")
    public String getIqaStatus() {
        return iqaStatus;
    }

    public void setIqaStatus(String iqaStatus) {
        this.iqaStatus = iqaStatus;
    }
}