package com.iispl.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

/**
 * File    : com/iispl/dto/xml/RrfBatchDto.java
 * Purpose : JAXB root DTO for RRF.xml.
 *           Contains batch-level metadata plus all cheques (accepted and returned).
 *           Return/rejection details are embedded inside each RrfChequeDto.
 *
 * Sample output:
 * {@code
 * <RrfBatch>
 *   <BatchId>BATCH001</BatchId>
 *   <BatchDate>2024-06-09</BatchDate>
 *   <TotalCheques>10</TotalCheques>
 *   <TotalAccepted>7</TotalAccepted>
 *   <TotalReturned>3</TotalReturned>
 *   <GeneratedAt>2024-06-09T10:30:00</GeneratedAt>
 *   <Cheques>
 *     <Cheque>...</Cheque>
 *   </Cheques>
 * </RrfBatch>
 * }
 */
@XmlRootElement(name = "RrfBatch")
@XmlAccessorType(XmlAccessType.FIELD)
public class RrfBatchDto {

    @XmlElement(name = "BatchId")
    private String batchId;

    @XmlElement(name = "BatchDate")
    private String batchDate;

    @XmlElement(name = "TotalCheques")
    private int totalCheques;

    @XmlElement(name = "TotalAccepted")
    private int totalAccepted;

    @XmlElement(name = "TotalReturned")
    private int totalReturned;

    @XmlElement(name = "GeneratedAt")
    private String generatedAt;

    @XmlElementWrapper(name = "Cheques")
    @XmlElement(name = "Cheque")
    private List<RrfChequeDto> cheques;

    // ── Getters and Setters ──────────────────────────────────────────────────

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getBatchDate() { return batchDate; }
    public void setBatchDate(String batchDate) { this.batchDate = batchDate; }

    public int getTotalCheques() { return totalCheques; }
    public void setTotalCheques(int totalCheques) { this.totalCheques = totalCheques; }

    public int getTotalAccepted() { return totalAccepted; }
    public void setTotalAccepted(int totalAccepted) { this.totalAccepted = totalAccepted; }

    public int getTotalReturned() { return totalReturned; }
    public void setTotalReturned(int totalReturned) { this.totalReturned = totalReturned; }

    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }

    public List<RrfChequeDto> getCheques() { return cheques; }
    public void setCheques(List<RrfChequeDto> cheques) { this.cheques = cheques; }
}
