package com.iispl.daoImpl;

import com.iispl.dao.CheckerInwardReportsDao;
import com.iispl.dto.InwardReportDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * File    : com/iispl/daoImpl/CheckerInwardReportsDaoImpl.java
 * Purpose : Pure-JDBC implementation of CheckerInwardReportsDao.
 *
 *   CHANGES vs previous version:
 *     - Query now selects: accepted_count, returned_count,
 *       presenting_banks (distinct bank names comma-joined via STRING_AGG)
 *     - Legacy columns (micr_error_count, iqa_fails, passed_count,
 *       rejected_count, referred_count) are still fetched for XML export
 *     - mapRow() populates new DTO fields
 */
public class CheckerInwardReportsDaoImpl implements CheckerInwardReportsDao {

    private static final Logger log = LoggerFactory.getLogger(CheckerInwardReportsDaoImpl.class);

    private static final String JDBC_URL  = "jdbc:postgresql://localhost:5432/expressCTS";
    private static final String JDBC_USER = "postgres";
    private static final String JDBC_PASS = "iispl660";

    private static final List<String> ALL_STATUSES = Arrays.asList(
            "PENDING_CHECKER", "ACCEPTED", "RETURNED", "REJECTED"
    );

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<InwardReportDTO> findReports(String batchIdSearch,
                                              Date   fromDate,
                                              Date   toDate,
                                              String status,
                                              int    pageNo,
                                              int    pageSize) {

        List<InwardReportDTO> results = new ArrayList<>();

        if (pageNo   < 1) pageNo   = 1;
        if (pageSize < 1) pageSize = 20;
        int offset = (pageNo - 1) * pageSize;

        String sql = buildSelectSql(batchIdSearch, fromDate, toDate, status)
                   + " ORDER BY ib.batch_date DESC, ib.created_at DESC"
                   + " LIMIT ? OFFSET ?";

        log.debug("findReports SQL: {}", sql);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = bindParams(ps, batchIdSearch, fromDate, toDate, status, 1);
            ps.setInt(idx++, pageSize);
            ps.setInt(idx,   offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            log.error("findReports failed: {}", e.getMessage(), e);
        }

        return results;
    }

    @Override
    public int countReports(String batchIdSearch,
                             Date   fromDate,
                             Date   toDate,
                             String status) {

        String baseSql  = buildSelectSql(batchIdSearch, fromDate, toDate, status);
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";

        log.debug("countReports SQL: {}", countSql);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {

            bindParams(ps, batchIdSearch, fromDate, toDate, status, 1);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            log.error("countReports failed: {}", e.getMessage(), e);
        }

        return 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Build the SELECT … FROM … WHERE … GROUP BY portion.
     *
     * NEW columns added to target UI:
     *   accepted_count  — cheques with ic.status = 'ACCEPTED'
     *   returned_count  — cheques with ic.status = 'RETURNED'
     *   presenting_banks — comma-joined DISTINCT presenting_bank_name values
     *
     * Legacy columns retained for XML export:
     *   micr_error_count, iqa_fails, passed_count, rejected_count, referred_count
     */
    private String buildSelectSql(String batchIdSearch,
                                   Date   fromDate,
                                   Date   toDate,
                                   String status) {

        StringBuilder sb = new StringBuilder();
        sb.append(
            "SELECT " +
            "  ib.batch_id, " +
            "  TO_CHAR(ib.batch_date, 'DD/MM/YYYY') AS batch_date_fmt, " +
            "  ib.total_cheques, " +
            "  ib.micr_error_count, " +
            "  ib.status, " +
            // ── Target grid columns ───────────────────────────────────────
            "  COALESCE(SUM(CASE WHEN ic.status = 'ACCEPTED' THEN 1 ELSE 0 END), 0) AS accepted_count, " +
            "  COALESCE(SUM(CASE WHEN ic.status = 'RETURNED' THEN 1 ELSE 0 END), 0) AS returned_count, " +
            // STRING_AGG produces comma-joined distinct bank names, e.g. "SBI, HDFC"
            "  COALESCE(STRING_AGG(DISTINCT ic.presenting_bank_name, ', ' ORDER BY ic.presenting_bank_name), '—') AS presenting_banks, " +
            // ── Legacy columns (kept for XML export) ─────────────────────
            "  COALESCE(SUM(CASE WHEN ic.iqa_status = 'FAIL'     THEN 1 ELSE 0 END), 0) AS iqa_fails, " +
            "  COALESCE(SUM(CASE WHEN ic.status     = 'REJECTED' THEN 1 ELSE 0 END), 0) AS rejected_count, " +
            "  COALESCE(SUM(CASE WHEN ic.status     = 'REFERRED' THEN 1 ELSE 0 END), 0) AS referred_count " +
            "FROM inward_batch ib " +
            "LEFT JOIN inward_cheque ic ON ic.batch_id = ib.id " +
            "WHERE 1=1 "
        );

        // ── Status filter ─────────────────────────────────────────────────
        if (isSpecificStatus(status)) {
            sb.append("AND ib.status = ? ");
        } else {
            sb.append("AND ib.status IN ('PENDING_CHECKER','ACCEPTED','RETURNED','REJECTED') ");
        }

        // ── Batch ID search ───────────────────────────────────────────────
        if (isNotBlank(batchIdSearch)) {
            sb.append("AND LOWER(ib.batch_id) LIKE LOWER(?) ");
        }

        // ── Date range ────────────────────────────────────────────────────
        if (fromDate != null) sb.append("AND ib.batch_date >= ? ");
        if (toDate   != null) sb.append("AND ib.batch_date <= ? ");

        // GROUP BY — note: presenting_bank_name uses STRING_AGG so it is NOT in GROUP BY
        sb.append(
            "GROUP BY " +
            "  ib.batch_id, ib.batch_date, ib.total_cheques, " +
            "  ib.micr_error_count, ib.status, ib.created_at "
        );

        return sb.toString();
    }

    private int bindParams(PreparedStatement ps,
                            String batchIdSearch,
                            Date   fromDate,
                            Date   toDate,
                            String status,
                            int    startIdx) throws SQLException {
        int idx = startIdx;
        if (isSpecificStatus(status))  ps.setString(idx++, status);
        if (isNotBlank(batchIdSearch)) ps.setString(idx++, "%" + batchIdSearch.trim() + "%");
        if (fromDate != null)          ps.setDate(idx++, new java.sql.Date(fromDate.getTime()));
        if (toDate   != null)          ps.setDate(idx++, new java.sql.Date(toDate.getTime()));
        return idx;
    }

    /** Map ResultSet row → InwardReportDTO. Populates both new + legacy fields. */
    private InwardReportDTO mapRow(ResultSet rs) throws SQLException {
        InwardReportDTO dto = new InwardReportDTO();
        dto.setBatchId       (rs.getString("batch_id"));
        dto.setBatchDate     (rs.getString("batch_date_fmt"));
        dto.setTotalCheques  (rs.getInt   ("total_cheques"));
        dto.setMicrErrors    (rs.getInt   ("micr_error_count"));
        dto.setStatus        (rs.getString("status"));

        // ── New target columns ────────────────────────────────────────────
        dto.setAcceptedCount (rs.getInt   ("accepted_count"));
        dto.setReturnedCount (rs.getInt   ("returned_count"));
        dto.setPresentingBanks(rs.getString("presenting_banks"));

        // ── Legacy (for XML export) ───────────────────────────────────────
        dto.setIqaFails      (rs.getInt   ("iqa_fails"));
        dto.setPassedCount   (rs.getInt   ("accepted_count")); // alias
        dto.setRejectedCount (rs.getInt   ("rejected_count"));
        dto.setReferredCount (rs.getInt   ("referred_count"));

        return dto;
    }

    private Connection getConnection() throws SQLException {
        try { Class.forName("org.postgresql.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("PostgreSQL driver not found", e); }
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
    }

    private boolean isSpecificStatus(String status) {
        return status != null && !status.isEmpty()
            && !status.equalsIgnoreCase("ALL")
            && ALL_STATUSES.contains(status);
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}