package com.iispl.dto.reports;

import java.math.BigDecimal;

/**
 * File    : com/iispl/dto/reports/CheckerChequeActionReportRow.java
 * Purpose : Java Bean row for the Checker — Cheque Action Log PDF report.
 *
 * JasperReports uses JRBeanCollectionDataSource to fill the report.
 * It reads values from this bean using getter method names.
 * Each getter name must exactly match the <field name="..."> declared
 * in checkerChequeActionReport.jrxml (Step 8).
 *
 * This DTO represents one row in the exception/audit report.
 * Each row = one action (REJECTED or REFERRED) taken by the Checker
 * on one cheque, sourced from the outward_checker_actions table.
 *
 * Fields sourced from:
 *   serialNo    → generated in service layer (row counter)
 *   batchId     → outward_checker_actions.outward_batch_id → outward_batch.batch_id
 *   chequeNo    → outward_checker_actions.outward_cheque_id → outward_cheque.cheque_no
 *   payeeName   → outward_cheque.payee_name
 *   amount      → outward_cheque.amount
 *   action      → outward_checker_actions.action  (REJECTED / REFERRED)
 *   reasonCode  → outward_checker_actions.reason_code
 *   remarks     → outward_checker_actions.remarks
 *   actionedAt  → outward_checker_actions.actioned_at (formatted as String)
 */
public class CheckerChequeActionReportRow {

    private Integer    serialNo;
    private String     batchId;
    private String     chequeNo;
    private String     payeeName;
    private BigDecimal amount;
    private String     action;
    private String     reasonCode;
    private String     remarks;
    private String     actionedAt;

    public CheckerChequeActionReportRow(Integer    serialNo,
                                         String     batchId,
                                         String     chequeNo,
                                         String     payeeName,
                                         BigDecimal amount,
                                         String     action,
                                         String     reasonCode,
                                         String     remarks,
                                         String     actionedAt) {
        this.serialNo   = serialNo;
        this.batchId    = batchId;
        this.chequeNo   = chequeNo;
        this.payeeName  = payeeName;
        this.amount     = amount;
        this.action     = action;
        this.reasonCode = reasonCode;
        this.remarks    = remarks;
        this.actionedAt = actionedAt;
    }

    public Integer    getSerialNo()   { return serialNo;   }
    public String     getBatchId()    { return batchId;    }
    public String     getChequeNo()   { return chequeNo;   }
    public String     getPayeeName()  { return payeeName;  }
    public BigDecimal getAmount()     { return amount;     }
    public String     getAction()     { return action;     }
    public String     getReasonCode() { return reasonCode; }
    public String     getRemarks()    { return remarks;    }
    public String     getActionedAt() { return actionedAt; }
}