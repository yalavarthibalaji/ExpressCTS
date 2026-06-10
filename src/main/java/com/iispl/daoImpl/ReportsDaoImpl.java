package com.iispl.daoImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;

import com.iispl.dao.ReportsDao;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheckerAction;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.util.HibernateUtil;

import jakarta.persistence.Query;

/**
 * File    : com/iispl/daoImpl/ReportsDaoImpl.java
 * Purpose : Hibernate implementation of ReportsDao — Maker and Checker reports.
 */
public class ReportsDaoImpl implements ReportsDao {

    // ════════════════════════════════════════════════════════════════
    //  MAKER REPORTS  (existing — not modified)
    // ════════════════════════════════════════════════════════════════

    @Override
    public List<OutwardBatch> getMyBatchesReport(Long makerId,
                                                   LocalDate fromDate,
                                                   LocalDate toDate) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            LocalDateTime toDateTime   = toDate.atTime(23, 59, 59);

            String hql = "FROM OutwardBatch b "
                       + "WHERE b.createdBy.id = :makerId "
                       + "AND b.createdAt >= :fromDateTime "
                       + "AND b.createdAt <= :toDateTime "
                       + "ORDER BY b.createdAt DESC";

            Query query = session.createQuery(hql, OutwardBatch.class);
            query.setParameter("makerId",      makerId);
            query.setParameter("fromDateTime", fromDateTime);
            query.setParameter("toDateTime",   toDateTime);

            List<OutwardBatch> result = query.getResultList();

            System.out.println("ReportsDaoImpl → getMyBatchesReport → "
                    + "makerId=" + makerId
                    + " from=" + fromDate
                    + " to="   + toDate
                    + " rows=" + result.size());

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    @Override
    public List<OutwardCheque> getChequesByBatch(Long batchDbId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            // outward_cheque.batch_id stores the Long PK of outward_batch
            String sql = "SELECT * FROM outward_cheque "
                       + "WHERE batch_id = :batchDbId "
                       + "ORDER BY seq_no ASC";

            @SuppressWarnings("unchecked")
            org.hibernate.query.NativeQuery<OutwardCheque> query =
                    session.createNativeQuery(sql, OutwardCheque.class);
            query.setParameter("batchDbId", batchDbId);

            List<OutwardCheque> result = query.getResultList();

            System.out.println("ReportsDaoImpl → getChequesByBatch → "
                    + "batchDbId=" + batchDbId
                    + " rows=" + result.size());

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CHECKER REPORTS  (new)
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns all batches verified by a specific checker.
     *
     * Query logic:
     *   - verified_by.id = checkerId  → only batches this checker approved
     *   - status IN ('CHECKER_APPROVED', 'EXPORTED')
     *     → approved but not yet exported + already exported
     *   - ORDER BY verified_at DESC   → most recently verified shown first
     *
     * Uses HQL because OutwardBatch is a mapped entity and
     * verified_by is a @ManyToOne relationship — HQL handles
     * the join cleanly via b.verifiedBy.id.
     */
    @Override
    public List<OutwardBatch> getVerifiedBatches(Long checkerId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            String hql = "FROM OutwardBatch b "
                    + "LEFT JOIN FETCH b.createdBy "
                    + "WHERE b.verifiedBy.id = :checkerId "
                    + "AND b.status IN ('CHECKER_APPROVED', 'EXPORTED') "
                    + "ORDER BY b.verifiedAt DESC";
            Query query = session.createQuery(hql, OutwardBatch.class);
            query.setParameter("checkerId", checkerId);

            List<OutwardBatch> result = query.getResultList();

            System.out.println("ReportsDaoImpl → getVerifiedBatches → "
                    + "checkerId=" + checkerId
                    + " rows=" + result.size());

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    /**
     * Returns all REJECTED and REFERRED actions taken by a specific checker.
     *
     * Query logic:
     *   - checker_id = checkerId      → only this checker's actions
     *   - action IN ('REJECTED', 'REFERRED')
     *     → exclude PASSED actions (not exceptions, not needed in audit log)
     *   - ORDER BY actioned_at DESC   → most recent actions shown first
     *
     * Uses HQL because OutwardCheckerAction is a mapped entity and
     * checker is a @ManyToOne relationship — HQL handles
     * the join cleanly via a.checker.id.
     *
     * The composer will access related outward_batch and outward_cheque
     * data via the lazy-loaded relationships on OutwardCheckerAction.
     * The session is kept open until the list is fully returned so
     * lazy loading works correctly inside the service layer.
     */
    @Override
    public List<OutwardCheckerAction> getCheckerActionLog(Long checkerId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            String hql = "FROM OutwardCheckerAction a "
                       + "WHERE a.checker.id = :checkerId "
                       + "AND a.action IN ('REJECTED', 'REFERRED') "
                       + "ORDER BY a.actionedAt DESC";

            Query query = session.createQuery(hql, OutwardCheckerAction.class);
            query.setParameter("checkerId", checkerId);

            List<OutwardCheckerAction> result = query.getResultList();

            System.out.println("ReportsDaoImpl → getCheckerActionLog → "
                    + "checkerId=" + checkerId
                    + " rows=" + result.size());

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }
}