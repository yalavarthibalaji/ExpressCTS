package com.iispl.dto.reports;

import java.math.BigDecimal;

/**
 * File    : com/iispl/dto/reports/RejectedChequeReportRow.java
 * Purpose : Java Bean row for the Maker Rejected Cheques PDF report.
 *           JRBeanCollectionDataSource reads fields via getters.
 *           Each getter name must match the field name in rejectedChequeReport.jrxml
 */
public class RejectedChequeReportRow {

    private Integer    serialNo;
    private String     batchId;
    private String     chequeNo;
    private String     payeeName;
    private BigDecimal amount;
    private String     reasonCode;
    private String     remarks;
    private String     rejectedAt;

    public RejectedChequeReportRow(Integer    serialNo,
                                   String     batchId,
                                   String     chequeNo,
                                   String     payeeName,
                                   BigDecimal amount,
                                   String     reasonCode,
                                   String     remarks,
                                   String     rejectedAt) {
        this.serialNo   = serialNo;
        this.batchId    = batchId;
        this.chequeNo   = chequeNo;
        this.payeeName  = payeeName;
        this.amount     = amount;
        this.reasonCode = reasonCode;
        this.remarks    = remarks;
        this.rejectedAt = rejectedAt;
    }

    public Integer    getSerialNo()   { return serialNo;   }
    public String     getBatchId()    { return batchId;    }
    public String     getChequeNo()   { return chequeNo;   }
    public String     getPayeeName()  { return payeeName;  }
    public BigDecimal getAmount()     { return amount;     }
    public String     getReasonCode() { return reasonCode; }
    public String     getRemarks()    { return remarks;    }
    public String     getRejectedAt() { return rejectedAt; }
}