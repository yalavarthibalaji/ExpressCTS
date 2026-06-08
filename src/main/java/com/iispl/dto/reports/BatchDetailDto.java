package com.iispl.dto.reports;

import java.math.BigDecimal;

/**
 * File    : com/iispl/dto/reports/BatchDetailDto.java
 * Purpose : One row in the Batch-wise Detail Report.
 *           Each row = one outward_batch with maker + checker names joined in.
 */
public class BatchDetailDto {

    private String     batchId;
    private String     status;
    private int        chequeCount;
    private BigDecimal totalAmount;
    private String     makerName;
    private String     checkerName;
    private String     createdAt;     // formatted
    private String     submittedAt;
    private String     verifiedAt;

    public BatchDetailDto() {}

    public String     getBatchId()      { return batchId; }
    public void       setBatchId(String s) { this.batchId = s; }
    public String     getStatus()       { return status; }
    public void       setStatus(String s) { this.status = s; }
    public int        getChequeCount()  { return chequeCount; }
    public void       setChequeCount(int n) { this.chequeCount = n; }
    public BigDecimal getTotalAmount()  { return totalAmount; }
    public void       setTotalAmount(BigDecimal a) { this.totalAmount = a; }
    public String     getMakerName()    { return makerName; }
    public void       setMakerName(String s) { this.makerName = s; }
    public String     getCheckerName()  { return checkerName; }
    public void       setCheckerName(String s) { this.checkerName = s; }
    public String     getCreatedAt()    { return createdAt; }
    public void       setCreatedAt(String s) { this.createdAt = s; }
    public String     getSubmittedAt()  { return submittedAt; }
    public void       setSubmittedAt(String s) { this.submittedAt = s; }
    public String     getVerifiedAt()   { return verifiedAt; }
    public void       setVerifiedAt(String s) { this.verifiedAt = s; }
}