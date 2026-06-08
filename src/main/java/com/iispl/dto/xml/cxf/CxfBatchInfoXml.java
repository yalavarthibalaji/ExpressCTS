package com.iispl.dto.xml.cxf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.math.BigDecimal;

/**
 * File    : com/iispl/dto/xml/cxf/CxfBatchInfoXml.java
 * Purpose : Maps the <BatchInfo> block inside the CXF root.
 *
 * Contains presenting-bank metadata, clearing house, session,
 * and totals required by NPCI for validation on receive.
 *
 * XML produced:
 *   <BatchInfo>
 *     <BatchId>B-2026-0608-001</BatchId>
 *     <PresentingBankCode>IISPL</PresentingBankCode>
 *     <PresentingBankName>IISPL Bank</PresentingBankName>
 *     <ClearingHouseCode>MUMBAI_GRID</ClearingHouseCode>
 *     <SessionCode>MORNING</SessionCode>
 *     <SessionDate>2026-06-08</SessionDate>
 *     <ChequeCount>10</ChequeCount>
 *     <TotalAmount>500000.00</TotalAmount>
 *     <GeneratedAt>2026-06-08T14:30:00</GeneratedAt>
 *     <GeneratedBy>1</GeneratedBy>
 *   </BatchInfo>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CxfBatchInfoXml {

    @XmlElement(name = "BatchId",            namespace = "urn:npci:cts:cxf:v1.0")
    private String batchId;

    @XmlElement(name = "PresentingBankCode", namespace = "urn:npci:cts:cxf:v1.0")
    private String presentingBankCode;

    @XmlElement(name = "PresentingBankName", namespace = "urn:npci:cts:cxf:v1.0")
    private String presentingBankName;

    @XmlElement(name = "ClearingHouseCode",  namespace = "urn:npci:cts:cxf:v1.0")
    private String clearingHouseCode;

    @XmlElement(name = "SessionCode",        namespace = "urn:npci:cts:cxf:v1.0")
    private String sessionCode;

    @XmlElement(name = "SessionDate",        namespace = "urn:npci:cts:cxf:v1.0")
    private String sessionDate;            // YYYY-MM-DD

    @XmlElement(name = "ChequeCount",        namespace = "urn:npci:cts:cxf:v1.0")
    private int chequeCount;

    @XmlElement(name = "TotalAmount",        namespace = "urn:npci:cts:cxf:v1.0")
    private BigDecimal totalAmount;

    @XmlElement(name = "GeneratedAt",        namespace = "urn:npci:cts:cxf:v1.0")
    private String generatedAt;            // ISO timestamp

    @XmlElement(name = "GeneratedBy",        namespace = "urn:npci:cts:cxf:v1.0")
    private Long generatedBy;

    // ── Getters and Setters ──
    public String getBatchId()            { return batchId; }
    public void   setBatchId(String s)    { this.batchId = s; }

    public String getPresentingBankCode() { return presentingBankCode; }
    public void   setPresentingBankCode(String s) { this.presentingBankCode = s; }

    public String getPresentingBankName() { return presentingBankName; }
    public void   setPresentingBankName(String s) { this.presentingBankName = s; }

    public String getClearingHouseCode()  { return clearingHouseCode; }
    public void   setClearingHouseCode(String s) { this.clearingHouseCode = s; }

    public String getSessionCode()        { return sessionCode; }
    public void   setSessionCode(String s){ this.sessionCode = s; }

    public String getSessionDate()        { return sessionDate; }
    public void   setSessionDate(String s){ this.sessionDate = s; }

    public int    getChequeCount()        { return chequeCount; }
    public void   setChequeCount(int n)   { this.chequeCount = n; }

    public BigDecimal getTotalAmount()         { return totalAmount; }
    public void       setTotalAmount(BigDecimal a) { this.totalAmount = a; }

    public String getGeneratedAt()        { return generatedAt; }
    public void   setGeneratedAt(String s){ this.generatedAt = s; }

    public Long   getGeneratedBy()        { return generatedBy; }
    public void   setGeneratedBy(Long id) { this.generatedBy = id; }
}