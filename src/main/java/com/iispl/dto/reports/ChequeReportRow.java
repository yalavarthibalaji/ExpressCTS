package com.iispl.dto.reports;

import java.math.BigDecimal;

/**
 * File    : com/iispl/dto/ChequeReportRow.java
 * Purpose : Java Bean row for the per-batch Cheque Detail PDF report.
 *           JRBeanCollectionDataSource reads fields via getters.
 *           Each getter name must match the field name in batchChequeReport.jrxml
 */
public class ChequeReportRow {

    private Integer    serialNo;
    private String     chequeNo;
    private String     accountNo;
    private String     accountHolder;
    private String     payeeName;
    private BigDecimal amount;
    private String     chequeDate;
    private String     micrCode;
    private String     status;
    private String     repairStatus;

    public ChequeReportRow(Integer    serialNo,
                           String     chequeNo,
                           String     accountNo,
                           String     accountHolder,
                           String     payeeName,
                           BigDecimal amount,
                           String     chequeDate,
                           String     micrCode,
                           String     status,
                           String     repairStatus) {
        this.serialNo      = serialNo;
        this.chequeNo      = chequeNo;
        this.accountNo     = accountNo;
        this.accountHolder = accountHolder;
        this.payeeName     = payeeName;
        this.amount        = amount;
        this.chequeDate    = chequeDate;
        this.micrCode      = micrCode;
        this.status        = status;
        this.repairStatus  = repairStatus;
    }

    public Integer    getSerialNo()      { return serialNo;      }
    public String     getChequeNo()      { return chequeNo;      }
    public String     getAccountNo()     { return accountNo;     }
    public String     getAccountHolder() { return accountHolder; }
    public String     getPayeeName()     { return payeeName;     }
    public BigDecimal getAmount()        { return amount;        }
    public String     getChequeDate()    { return chequeDate;    }
    public String     getMicrCode()      { return micrCode;      }
    public String     getStatus()        { return status;        }
    public String     getRepairStatus()  { return repairStatus;  }
}