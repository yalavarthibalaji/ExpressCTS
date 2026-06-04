package com.iispl.service;

import com.iispl.dto.InwardReportDTO;

import java.util.Date;
import java.util.List;

/**
 * File    : com/iispl/service/CheckerInwardReportsService.java
 * Purpose : Service contract for Checker Inward Reports.
 *           Sits between CheckerInwardReportsComposer (ZK UI layer) and
 *           CheckerInwardReportsDao (JDBC layer).
 *           Responsible for:
 *             - input validation (date range, null guards)
 *             - delegating to DAO
 *             - building CXF / BRF XML content for file export
 */
public interface CheckerInwardReportsService {

    /**
     * Fetch a page of inward batch report rows that match the supplied filters.
     *
     * @param batchIdSearch partial batch_id search string (null → no filter)
     * @param fromDate      lower bound on batch_date (null → no lower bound)
     * @param toDate        upper bound on batch_date (null → no upper bound)
     * @param status        "ALL" | "PENDING_CHECKER" | "ACCEPTED" | "RETURNED" | "REJECTED"
     * @param pageNo        1-based page number
     * @param pageSize      rows per page
     * @return list of matching rows (never null, may be empty)
     * @throws IllegalArgumentException if fromDate is after toDate
     */
    List<InwardReportDTO> getReports(String batchIdSearch,
                                     Date   fromDate,
                                     Date   toDate,
                                     String status,
                                     int    pageNo,
                                     int    pageSize);

    /**
     * Total number of rows matching the same filters — used for pagination.
     *
     * @param batchIdSearch partial batch_id search string (null → no filter)
     * @param fromDate      lower bound on batch_date (null → no lower bound)
     * @param toDate        upper bound on batch_date (null → no upper bound)
     * @param status        "ALL" | "PENDING_CHECKER" | "ACCEPTED" | "RETURNED" | "REJECTED"
     * @return total matching row count
     */
    int getTotalCount(String batchIdSearch,
                      Date   fromDate,
                      Date   toDate,
                      String status);

    /**
     * Build CXF XML content for the given batch.
     * The returned string is ready to be written to <batchId>.cxf.xml
     *
     * @param dto row whose batchId to use as root element attribute
     * @return UTF-8 XML string
     */
    String buildAckXml(InwardReportDTO dto);

    /**
     * Build BRF XML content for the given batch.
     * The returned string is ready to be written to <batchId>.brf.xml
     *
     * @param dto row whose batchId to use as root element attribute
     * @return UTF-8 XML string
     */
    String buildRrfXml(InwardReportDTO dto);
}
