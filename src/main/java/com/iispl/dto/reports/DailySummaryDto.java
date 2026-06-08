package com.iispl.dto.reports;

import java.math.BigDecimal;

/**
 * File    : com/iispl/dto/reports/DailySummaryDto.java
 * Purpose : One row in the Daily Outward Summary report.
 *           Aggregates batch counts per calendar day.
 */
public class DailySummaryDto {

    private String     reportDate;       // YYYY-MM-DD
    private int        batchCount;
    private int        totalCheques;
    private BigDecimal totalAmount;
    private int        exportedBatches;
    private int        rejectedBatches;

    public DailySummaryDto() {}

    public DailySummaryDto(String reportDate, int batchCount, int totalCheques,
                           BigDecimal totalAmount, int exportedBatches,
                           int rejectedBatches) {
        this.reportDate      = reportDate;
        this.batchCount      = batchCount;
        this.totalCheques    = totalCheques;
        this.totalAmount     = totalAmount;
        this.exportedBatches = exportedBatches;
        this.rejectedBatches = rejectedBatches;
    }

    public String     getReportDate()      { return reportDate; }
    public void       setReportDate(String s) { this.reportDate = s; }
    public int        getBatchCount()      { return batchCount; }
    public void       setBatchCount(int n) { this.batchCount = n; }
    public int        getTotalCheques()    { return totalCheques; }
    public void       setTotalCheques(int n){ this.totalCheques = n; }
    public BigDecimal getTotalAmount()     { return totalAmount; }
    public void       setTotalAmount(BigDecimal a) { this.totalAmount = a; }
    public int        getExportedBatches() { return exportedBatches; }
    public void       setExportedBatches(int n) { this.exportedBatches = n; }
    public int        getRejectedBatches() { return rejectedBatches; }
    public void       setRejectedBatches(int n) { this.rejectedBatches = n; }
}