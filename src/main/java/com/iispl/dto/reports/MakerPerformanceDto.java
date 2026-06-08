package com.iispl.dto.reports;

import java.math.BigDecimal;

/**
 * File    : com/iispl/dto/reports/MakerPerformanceDto.java
 * Purpose : One row in the Maker Performance Report.
 *           Aggregates upload activity per maker user.
 */
public class MakerPerformanceDto {

    private String     makerName;
    private int        batchesUploaded;
    private int        chequesUploaded;
    private BigDecimal totalAmount;
    private int        batchesRejected;
    private int        batchesReferred;

    public MakerPerformanceDto() {}

    public String     getMakerName()       { return makerName; }
    public void       setMakerName(String s) { this.makerName = s; }
    public int        getBatchesUploaded() { return batchesUploaded; }
    public void       setBatchesUploaded(int n) { this.batchesUploaded = n; }
    public int        getChequesUploaded() { return chequesUploaded; }
    public void       setChequesUploaded(int n) { this.chequesUploaded = n; }
    public BigDecimal getTotalAmount()     { return totalAmount; }
    public void       setTotalAmount(BigDecimal a) { this.totalAmount = a; }
    public int        getBatchesRejected() { return batchesRejected; }
    public void       setBatchesRejected(int n) { this.batchesRejected = n; }
    public int        getBatchesReferred() { return batchesReferred; }
    public void       setBatchesReferred(int n) { this.batchesReferred = n; }
}