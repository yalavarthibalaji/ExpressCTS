package com.iispl.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * File    : com/iispl/dto/xml/BatchXml.java
 * Purpose : Root JAXB class. Maps the <batch> root element in the XML file.
 *           Contains batch summary info and the list of cheques.
 *
 * XML maps to:
 *   <batch>
 *       <batchInfo> ... </batchInfo>
 *       <cheques>   ... </cheques>
 *   </batch>
 */
@XmlRootElement(name = "batch")
@XmlAccessorType(XmlAccessType.FIELD)
public class BatchXml {

    @XmlElement(name = "batchInfo")
    private BatchInfoXml batchInfo;

    @XmlElement(name = "cheques")
    private ChequeListXml cheques;

    // ── Getters and Setters ──

    public BatchInfoXml getBatchInfo() {
        return batchInfo;
    }

    public void setBatchInfo(BatchInfoXml batchInfo) {
        this.batchInfo = batchInfo;
    }

    public ChequeListXml getCheques() {
        return cheques;
    }

    public void setCheques(ChequeListXml cheques) {
        this.cheques = cheques;
    }
}