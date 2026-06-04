package com.iispl.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.math.BigDecimal;
import java.time.LocalDate;

@XmlAccessorType(XmlAccessType.FIELD)
public class BpxfHeader {

    @XmlElement(name = "BatchId")
    private String batchId;

    @XmlElement(name = "BatchDate")
    private String batchDate;       // String — parsed to LocalDate in service

    @XmlElement(name = "PresentingBankCode")
    private String presentingBankCode;

    @XmlElement(name = "PresentingBankName")
    private String presentingBankName;

    @XmlElement(name = "TotalCheques")
    private int totalCheques;

    @XmlElement(name = "TotalAmount")
    private BigDecimal totalAmount;

    public String getBatchId()                   { return batchId; }
    public void   setBatchId(String v)           { this.batchId = v; }

    public String getBatchDate()                 { return batchDate; }
    public void   setBatchDate(String v)         { this.batchDate = v; }

    public String getPresentingBankCode()        { return presentingBankCode; }
    public void   setPresentingBankCode(String v){ this.presentingBankCode = v; }

    public String getPresentingBankName()        { return presentingBankName; }
    public void   setPresentingBankName(String v){ this.presentingBankName = v; }

    public int    getTotalCheques()              { return totalCheques; }
    public void   setTotalCheques(int v)         { this.totalCheques = v; }

    public BigDecimal getTotalAmount()           { return totalAmount; }
    public void       setTotalAmount(BigDecimal v){ this.totalAmount = v; }
}