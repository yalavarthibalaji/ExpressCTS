package com.iispl.dto.reports;

import java.math.BigDecimal;

/**
 * File    : com/iispl/dto/reports/CheckerBatchReportRow.java
 * Purpose : Java Bean row for the Checker — Verified Batches PDF report.
 *
 * JasperReports uses JRBeanCollectionDataSource to fill the report.
 * It reads values from this bean using getter method names.
 * Each getter name must exactly match the <field name="..."> declared
 * in checkerBatchReport.jrxml (Step 7).
 *
 * This DTO is Checker-specific. It differs from BatchReportRow (Maker)
 * in the following ways:
 *   - Has makerName  instead of expectedAmount
 *     (Checker sees WHO made the batch, not the expected amount)
 *   - Has verifiedAt instead of createdAt
 *     (Checker sees WHEN they approved it, not when it was uploaded)
 *   - No expectedAmount field (not relevant to Checker view)
 */
public class CheckerBatchReportRow {

    private Integer    serialNo;
    private String     batchId;
    private Integer    chequeCount;
    private String     makerName;
    private BigDecimal actualAmount;
    private String     verifiedAt;
    private String     status;

    public CheckerBatchReportRow(Integer    serialNo,
                                  String     batchId,
                                  Integer    chequeCount,
                                  String     makerName,
                                  BigDecimal actualAmount,
                                  String     verifiedAt,
                                  String     status) {
        this.serialNo     = serialNo;
        this.batchId      = batchId;
        this.chequeCount  = chequeCount;
        this.makerName    = makerName;
        this.actualAmount = actualAmount;
        this.verifiedAt   = verifiedAt;
        this.status       = status;
    }

    public Integer    getSerialNo()    { return serialNo;    }
    public String     getBatchId()     { return batchId;     }
    public Integer    getChequeCount() { return chequeCount; }
    public String     getMakerName()   { return makerName;   }
    public BigDecimal getActualAmount(){ return actualAmount; }
    public String     getVerifiedAt()  { return verifiedAt;  }
    public String     getStatus()      { return status;      }
}