package com.iispl.dao;

import com.iispl.dto.InwardReportDTO;

import java.util.Date;
import java.util.List;

/**
 * File    : com/iispl/dao/CheckerInwardReportsDao.java
 * Purpose : DAO contract for Checker Inward Reports.
 *           All methods query the inward_batch table (plus inward_cheque for
 *           aggregate counts) using JDBC — no Hibernate ORM objects are
 *           returned here; results come back as InwardReportDTO.
 */
public interface CheckerInwardReportsDao {

    /**
     * Fetch inward batch report rows matching the supplied filters.
     *
     * @param batchIdSearch partial / exact batch_id search string (nullable)
     * @param fromDate      lower bound on batch_date (nullable)
     * @param toDate        upper bound on batch_date (nullable)
     * @param status        one of ALL | PENDING_CHECKER | ACCEPTED | RETURNED | REJECTED (nullable → ALL)
     * @param pageNo        1-based page number
     * @param pageSize      number of rows per page
     * @return list of matching InwardReportDTO rows (never null)
     */
    List<InwardReportDTO> findReports(String batchIdSearch,
                                      Date fromDate,
                                      Date toDate,
                                      String status,
                                      int pageNo,
                                      int pageSize);

    /**
     * Count total rows matching the same filters (for pagination display).
     *
     * @param batchIdSearch partial / exact batch_id search string (nullable)
     * @param fromDate      lower bound on batch_date (nullable)
     * @param toDate        upper bound on batch_date (nullable)
     * @param status        one of ALL | PENDING_CHECKER | ACCEPTED | RETURNED | REJECTED
     * @return total row count
     */
    int countReports(String batchIdSearch,
                     Date fromDate,
                     Date toDate,
                     String status);
}
