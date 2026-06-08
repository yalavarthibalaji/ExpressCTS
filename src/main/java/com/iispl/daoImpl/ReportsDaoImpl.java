package com.iispl.daoImpl;

import com.iispl.dao.ReportsDao;

import com.iispl.dto.reports.*;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * File    : com/iispl/daoImpl/ReportsDaoImpl.java
 * Purpose : Implementation of ReportsDao.
 *           Each query is a single native SQL with aggregation/joins.
 *           Object[] result rows are mapped manually to DTOs to keep
 *           the code beginner-friendly (no ResultTransformer magic).
 */
public class ReportsDaoImpl implements ReportsDao {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ════════════════════════════════════════════════════
    //  1. Daily Summary
    // ════════════════════════════════════════════════════

    @Override
    public List<DailySummaryDto> getDailySummary(LocalDate fromDate,
                                                  LocalDate toDate) {
        List<DailySummaryDto> result = new ArrayList<>();
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql =
                "SELECT CAST(created_at AS DATE) AS d, "
              + "       COUNT(*)                                            AS batches, "
              + "       COALESCE(SUM(cheque_count), 0)                      AS cheques, "
              + "       COALESCE(SUM(actual_amount), 0)                     AS amount, "
              + "       SUM(CASE WHEN status = 'EXPORTED' THEN 1 ELSE 0 END) AS exported, "
              + "       SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected "
              + "FROM outward_batch "
              + "WHERE created_at >= :fromDate "
              + "  AND created_at <  :toDatePlus1 "
              + "GROUP BY CAST(created_at AS DATE) "
              + "ORDER BY d DESC";

            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("fromDate",    fromDate.atStartOfDay());
            q.setParameter("toDatePlus1", toDate.plusDays(1).atStartOfDay());

            for (Object row : q.list()) {
                Object[] r = (Object[]) row;
                DailySummaryDto dto = new DailySummaryDto();
                dto.setReportDate(String.valueOf(r[0]));
                dto.setBatchCount(toInt(r[1]));
                dto.setTotalCheques(toInt(r[2]));
                dto.setTotalAmount(toBig(r[3]));
                dto.setExportedBatches(toInt(r[4]));
                dto.setRejectedBatches(toInt(r[5]));
                result.add(dto);
            }
            System.out.println("ReportsDao → getDailySummary: " + result.size() + " rows");
        } catch (Exception e) {
            System.err.println("ReportsDao → getDailySummary failed: " + e.getMessage());
        } finally {
            session.close();
        }
        return result;
    }

    // ════════════════════════════════════════════════════
    //  2. Batch Details
    // ════════════════════════════════════════════════════

    @Override
    public List<BatchDetailDto> getBatchDetails(LocalDate fromDate, LocalDate toDate) {
        List<BatchDetailDto> result = new ArrayList<>();
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql =
                "SELECT b.batch_id, b.status, b.cheque_count, b.actual_amount, "
              + "       um.full_name AS maker, "
              + "       uc.full_name AS checker, "
              + "       b.created_at, b.submitted_at, b.verified_at "
              + "FROM outward_batch b "
              + "LEFT JOIN users um ON b.created_by  = um.id "
              + "LEFT JOIN users uc ON b.verified_by = uc.id "
              + "WHERE b.created_at >= :fromDate "
              + "  AND b.created_at <  :toDatePlus1 "
              + "ORDER BY b.created_at DESC";

            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("fromDate",    fromDate.atStartOfDay());
            q.setParameter("toDatePlus1", toDate.plusDays(1).atStartOfDay());

            for (Object row : q.list()) {
                Object[] r = (Object[]) row;
                BatchDetailDto dto = new BatchDetailDto();
                dto.setBatchId(toStr(r[0]));
                dto.setStatus(toStr(r[1]));
                dto.setChequeCount(toInt(r[2]));
                dto.setTotalAmount(toBig(r[3]));
                dto.setMakerName(toStr(r[4]));
                dto.setCheckerName(toStr(r[5]));
                dto.setCreatedAt(toDateTime(r[6]));
                dto.setSubmittedAt(toDateTime(r[7]));
                dto.setVerifiedAt(toDateTime(r[8]));
                result.add(dto);
            }
            System.out.println("ReportsDao → getBatchDetails: " + result.size() + " rows");
        } catch (Exception e) {
            System.err.println("ReportsDao → getBatchDetails failed: " + e.getMessage());
        } finally {
            session.close();
        }
        return result;
    }

    // ════════════════════════════════════════════════════
    //  3. Checker Actions  (from outward_checker_actions audit table)
    // ════════════════════════════════════════════════════

    @Override
    public List<CheckerActionDto> getCheckerActions(LocalDate fromDate, LocalDate toDate) {
        List<CheckerActionDto> result = new ArrayList<>();
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql =
                "SELECT u.full_name, "
              + "       SUM(CASE WHEN a.action = 'PASS'   THEN 1 ELSE 0 END) AS p, "
              + "       SUM(CASE WHEN a.action = 'REJECT' THEN 1 ELSE 0 END) AS r, "
              + "       SUM(CASE WHEN a.action = 'REFER'  THEN 1 ELSE 0 END) AS f, "
              + "       COUNT(*)                                              AS total, "
              + "       COUNT(DISTINCT a.outward_batch_id)                    AS batches "
              + "FROM outward_checker_actions a "
              + "JOIN users u ON a.checker_id = u.id "
              + "WHERE a.actioned_at >= :fromDate "
              + "  AND a.actioned_at <  :toDatePlus1 "
              + "GROUP BY u.full_name "
              + "ORDER BY total DESC";

            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("fromDate",    fromDate.atStartOfDay());
            q.setParameter("toDatePlus1", toDate.plusDays(1).atStartOfDay());

            for (Object row : q.list()) {
                Object[] r = (Object[]) row;
                CheckerActionDto dto = new CheckerActionDto();
                dto.setCheckerName(toStr(r[0]));
                dto.setPassedCount(toInt(r[1]));
                dto.setRejectedCount(toInt(r[2]));
                dto.setReferredCount(toInt(r[3]));
                dto.setTotalActions(toInt(r[4]));
                dto.setBatchesHandled(toInt(r[5]));
                result.add(dto);
            }
            System.out.println("ReportsDao → getCheckerActions: " + result.size() + " rows");
        } catch (Exception e) {
            System.err.println("ReportsDao → getCheckerActions failed: " + e.getMessage());
        } finally {
            session.close();
        }
        return result;
    }

    // ════════════════════════════════════════════════════
    //  4. Maker Performance
    // ════════════════════════════════════════════════════

    @Override
    public List<MakerPerformanceDto> getMakerPerformance(LocalDate fromDate,
                                                          LocalDate toDate) {
        List<MakerPerformanceDto> result = new ArrayList<>();
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql =
                "SELECT u.full_name, "
              + "       COUNT(DISTINCT b.id)                                       AS batches, "
              + "       COALESCE(SUM(b.cheque_count), 0)                           AS cheques, "
              + "       COALESCE(SUM(b.actual_amount), 0)                          AS amount, "
              + "       SUM(CASE WHEN b.status = 'REJECTED'   THEN 1 ELSE 0 END)   AS rej, "
              + "       SUM(CASE WHEN b.status = 'REFER_BACK' THEN 1 ELSE 0 END)   AS ref "
              + "FROM outward_batch b "
              + "JOIN users u ON b.created_by = u.id "
              + "WHERE b.created_at >= :fromDate "
              + "  AND b.created_at <  :toDatePlus1 "
              + "GROUP BY u.full_name "
              + "ORDER BY batches DESC";

            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("fromDate",    fromDate.atStartOfDay());
            q.setParameter("toDatePlus1", toDate.plusDays(1).atStartOfDay());

            for (Object row : q.list()) {
                Object[] r = (Object[]) row;
                MakerPerformanceDto dto = new MakerPerformanceDto();
                dto.setMakerName(toStr(r[0]));
                dto.setBatchesUploaded(toInt(r[1]));
                dto.setChequesUploaded(toInt(r[2]));
                dto.setTotalAmount(toBig(r[3]));
                dto.setBatchesRejected(toInt(r[4]));
                dto.setBatchesReferred(toInt(r[5]));
                result.add(dto);
            }
            System.out.println("ReportsDao → getMakerPerformance: " + result.size() + " rows");
        } catch (Exception e) {
            System.err.println("ReportsDao → getMakerPerformance failed: " + e.getMessage());
        } finally {
            session.close();
        }
        return result;
    }

    // ════════════════════════════════════════════════════
    //  5. Rejection Report
    // ════════════════════════════════════════════════════

    @Override
    public List<RejectionDto> getRejections(LocalDate fromDate, LocalDate toDate) {
        List<RejectionDto> result = new ArrayList<>();
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql =
                "SELECT c.cheque_no, b.batch_id, "
              + "       CASE WHEN c.status = 'REJECTED'         THEN 'MAKER' "
              + "            WHEN c.status = 'CHECKER_REJECTED' THEN 'CHECKER' "
              + "            ELSE 'UNKNOWN' END                         AS stage, "
              + "       c.rejected_reason_code, "
              + "       c.remarks, "
              + "       u.full_name, "
              + "       c.rejected_at, "
              + "       c.amount "
              + "FROM outward_cheque c "
              + "JOIN outward_batch b ON c.batch_id = b.id "
              + "LEFT JOIN users u    ON c.rejected_by = u.id "
              + "WHERE c.status IN ('REJECTED', 'CHECKER_REJECTED') "
              + "  AND c.rejected_at >= :fromDate "
              + "  AND c.rejected_at <  :toDatePlus1 "
              + "ORDER BY c.rejected_at DESC";

            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("fromDate",    fromDate.atStartOfDay());
            q.setParameter("toDatePlus1", toDate.plusDays(1).atStartOfDay());

            for (Object row : q.list()) {
                Object[] r = (Object[]) row;
                RejectionDto dto = new RejectionDto();
                dto.setChequeNo(toStr(r[0]));
                dto.setBatchId(toStr(r[1]));
                dto.setRejectStage(toStr(r[2]));
                dto.setReasonCode(toStr(r[3]));
                dto.setRemarks(toStr(r[4]));
                dto.setRejectedBy(toStr(r[5]));
                dto.setRejectedAt(toDateTime(r[6]));
                dto.setAmount(toBig(r[7]));
                result.add(dto);
            }
            System.out.println("ReportsDao → getRejections: " + result.size() + " rows");
        } catch (Exception e) {
            System.err.println("ReportsDao → getRejections failed: " + e.getMessage());
        } finally {
            session.close();
        }
        return result;
    }

    // ════════════════════════════════════════════════════
    //  Type-safe converters (PostgreSQL → Java)
    // ════════════════════════════════════════════════════

    private int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); }
        catch (Exception e) { return 0; }
    }

    private BigDecimal toBig(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number)     return BigDecimal.valueOf(((Number) o).doubleValue());
        try { return new BigDecimal(o.toString()); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }

    private String toStr(Object o) {
        return o == null ? "" : o.toString();
    }

    private String toDateTime(Object o) {
        if (o == null) return "";
        if (o instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) o).toLocalDateTime().format(DT_FMT);
        }
        if (o instanceof java.time.LocalDateTime) {
            return ((java.time.LocalDateTime) o).format(DT_FMT);
        }
        return o.toString();
    }
}