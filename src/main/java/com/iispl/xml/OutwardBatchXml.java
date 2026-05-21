package com.iispl.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

// maps to the root element <OutwardBatch> in XML
@XmlRootElement(name = "OutwardBatch")
public class OutwardBatchXml {

    private BatchHeaderXml batchHeader;
    private ChequeListXml cheques;

    @XmlElement(name = "BatchHeader")
    public BatchHeaderXml getBatchHeader() {
        return batchHeader;
    }

    public void setBatchHeader(BatchHeaderXml batchHeader) {
        this.batchHeader = batchHeader;
    }

    @XmlElement(name = "Cheques")
    public ChequeListXml getCheques() {
        return cheques;
    }

    public void setCheques(ChequeListXml cheques) {
        this.cheques = cheques;
    }
}