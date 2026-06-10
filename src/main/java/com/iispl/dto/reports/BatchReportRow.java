package com.iispl.dto.reports;

import java.math.BigDecimal;

/**
 * File    : com/iispl/dto/BatchReportRow.java
 * Purpose : Java Bean row for My Batches PDF report.
 *           JRBeanCollectionDataSource reads fields via getters.
 *           Each getter name must match field name in myBatchReport.jrxml
 */
public class BatchReportRow {

    private Integer    serialNo;
    private String     batchId;
    private Integer    chequeCount;
    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private String     status;
    private String     createdAt;

    public BatchReportRow(Integer    serialNo,
                          String     batchId,
                          Integer    chequeCount,
                          BigDecimal expectedAmount,
                          BigDecimal actualAmount,
                          String     status,
                          String     createdAt) {
        this.serialNo       = serialNo;
        this.batchId        = batchId;
        this.chequeCount    = chequeCount;
        this.expectedAmount = expectedAmount;
        this.actualAmount   = actualAmount;
        this.status         = status;
        this.createdAt      = createdAt;
    }

    public Integer    getSerialNo()       { return serialNo;       }
    public String     getBatchId()        { return batchId;        }
    public Integer    getChequeCount()    { return chequeCount;    }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public BigDecimal getActualAmount()   { return actualAmount;   }
    public String     getStatus()         { return status;         }
    public String     getCreatedAt()      { return createdAt;      }
}