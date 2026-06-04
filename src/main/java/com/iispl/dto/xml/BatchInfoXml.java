package com.iispl.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * File    : com/iispl/dto/xml/BatchInfoXml.java
 * Purpose : Maps the <batchInfo> element inside <batch>.
 *           Contains total cheque count and total amount declared in the file.
 *           These values are used to verify against user-entered expected values.
 *
 * XML maps to:
 *   <batchInfo>
 *       <totalCheques>3</totalCheques>
 *       <totalAmount>150000.00</totalAmount>
 *   </batchInfo>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BatchInfoXml {

    @XmlElement(name = "totalCheques")
    private int totalCheques;

    @XmlElement(name = "totalAmount")
    private double totalAmount;

    // ── Getters and Setters ──

    public int getTotalCheques() {
        return totalCheques;
    }

    public void setTotalCheques(int totalCheques) {
        this.totalCheques = totalCheques;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}