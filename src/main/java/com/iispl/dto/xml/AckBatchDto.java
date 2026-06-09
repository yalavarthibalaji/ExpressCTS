package com.iispl.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

/**
 * File    : com/iispl/dto/xml/AckBatchDto.java
 * Purpose : JAXB root DTO for ACK.xml.
 *           Contains batch-level metadata and a list of accepted cheques only.
 *
 * Sample output:
 * {@code
 * <AckBatch>
 *   <BatchId>BATCH001</BatchId>
 *   <BatchDate>2024-06-09</BatchDate>
 *   <TotalAccepted>5</TotalAccepted>
 *   <GeneratedAt>2024-06-09T10:30:00</GeneratedAt>
 *   <Cheques>
 *     <Cheque>...</Cheque>
 *   </Cheques>
 * </AckBatch>
 * }
 */
@XmlRootElement(name = "AckBatch")
@XmlAccessorType(XmlAccessType.FIELD)
public class AckBatchDto {

    @XmlElement(name = "BatchId")
    private String batchId;

    @XmlElement(name = "BatchDate")
    private String batchDate;

    @XmlElement(name = "TotalAccepted")
    private int totalAccepted;

    @XmlElement(name = "GeneratedAt")
    private String generatedAt;

    @XmlElementWrapper(name = "Cheques")
    @XmlElement(name = "Cheque")
    private List<AckChequeDto> cheques;

    // ── Getters and Setters ──────────────────────────────────────────────────

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getBatchDate() { return batchDate; }
    public void setBatchDate(String batchDate) { this.batchDate = batchDate; }

    public int getTotalAccepted() { return totalAccepted; }
    public void setTotalAccepted(int totalAccepted) { this.totalAccepted = totalAccepted; }

    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }

    public List<AckChequeDto> getCheques() { return cheques; }
    public void setCheques(List<AckChequeDto> cheques) { this.cheques = cheques; }
}
