package com.iispl.dto.xml.cxf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * File    : com/iispl/dto/xml/cxf/CxfRootXml.java
 * Purpose : Root JAXB DTO for the outgoing CXF (Cheque eXchange File).
 *           Marshalled by CxfFileGenerator and written to disk for transmission to NPCI.
 *
 * XML structure produced:
 *
 *   <CXF xmlns="urn:npci:cts:cxf:v1.0" version="1.0">
 *     <BatchInfo> ... </BatchInfo>
 *     <ChequeList>
 *       <Cheque> ... </Cheque>
 *       ...
 *     </ChequeList>
 *   </CXF>
 *
 * This follows NPCI's CTS CXF specification (version 1.0).
 */
@XmlRootElement(name = "CXF", namespace = "urn:npci:cts:cxf:v1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class CxfRootXml {

    @XmlAttribute(name = "version")
    private String version = "1.0";

    @XmlElement(name = "BatchInfo", namespace = "urn:npci:cts:cxf:v1.0")
    private CxfBatchInfoXml batchInfo;

    @XmlElement(name = "ChequeList", namespace = "urn:npci:cts:cxf:v1.0")
    private CxfChequeListXml chequeList;

    // ── Getters and Setters ──
    public String getVersion() { return version; }
    public void   setVersion(String version) { this.version = version; }

    public CxfBatchInfoXml getBatchInfo() { return batchInfo; }
    public void            setBatchInfo(CxfBatchInfoXml batchInfo) { this.batchInfo = batchInfo; }

    public CxfChequeListXml getChequeList() { return chequeList; }
    public void             setChequeList(CxfChequeListXml chequeList) { this.chequeList = chequeList; }
}