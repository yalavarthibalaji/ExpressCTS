package com.iispl.dto.reports;

import java.math.BigDecimal;

/**
 * File    : com/iispl/dto/reports/RejectionDto.java
 * Purpose : One row in the Rejection Report — every cheque
 *           with status REJECTED or CHECKER_REJECTED in date range.
 */
public class RejectionDto {

    private String     chequeNo;
    private String     batchId;
    private String     rejectStage;     // MAKER, CHECKER
    private String     reasonCode;
    private String     remarks;
    private String     rejectedBy;
    private String     rejectedAt;
    private BigDecimal amount;

    public RejectionDto() {}

    public String     getChequeNo()    { return chequeNo; }
    public void       setChequeNo(String s) { this.chequeNo = s; }
    public String     getBatchId()     { return batchId; }
    public void       setBatchId(String s) { this.batchId = s; }
    public String     getRejectStage() { return rejectStage; }
    public void       setRejectStage(String s) { this.rejectStage = s; }
    public String     getReasonCode()  { return reasonCode; }
    public void       setReasonCode(String s) { this.reasonCode = s; }
    public String     getRemarks()     { return remarks; }
    public void       setRemarks(String s) { this.remarks = s; }
    public String     getRejectedBy()  { return rejectedBy; }
    public void       setRejectedBy(String s) { this.rejectedBy = s; }
    public String     getRejectedAt()  { return rejectedAt; }
    public void       setRejectedAt(String s) { this.rejectedAt = s; }
    public BigDecimal getAmount()      { return amount; }
    public void       setAmount(BigDecimal a) { this.amount = a; }
}