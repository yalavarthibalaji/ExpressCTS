package com.iispl.dto;

import java.time.LocalDateTime;

public class InwardBatchDto {

    private Long          id;             // ← added: InwardBatch PK, needed to fetch cheques
    private String        batchId;
    private String        sourceFileName;
    private int           totalCheques;
    private int           micrErrorCount;
    private LocalDateTime parsedAt;
    private String        status;

    public InwardBatchDto() {}

    public InwardBatchDto(Long id, String batchId, String sourceFileName,
                          int totalCheques, int micrErrorCount,
                          LocalDateTime parsedAt, String status) {
        this.id             = id;
        this.batchId        = batchId;
        this.sourceFileName = sourceFileName;
        this.totalCheques   = totalCheques;
        this.micrErrorCount = micrErrorCount;
        this.parsedAt       = parsedAt;
        this.status         = status;
    }

    public Long          getId()             { return id; }
    public String        getBatchId()        { return batchId; }
    public String        getSourceFileName() { return sourceFileName; }
    public int           getTotalCheques()   { return totalCheques; }
    public int           getMicrErrorCount() { return micrErrorCount; }
    public LocalDateTime getParsedAt()       { return parsedAt; }
    public String        getStatus()         { return status; }

    public void setId(Long id)                          { this.id = id; }
    public void setBatchId(String batchId)              { this.batchId = batchId; }
    public void setSourceFileName(String sourceFileName){ this.sourceFileName = sourceFileName; }
    public void setTotalCheques(int totalCheques)       { this.totalCheques = totalCheques; }
    public void setMicrErrorCount(int micrErrorCount)   { this.micrErrorCount = micrErrorCount; }
    public void setParsedAt(LocalDateTime parsedAt)     { this.parsedAt = parsedAt; }
    public void setStatus(String status)                { this.status = status; }
}