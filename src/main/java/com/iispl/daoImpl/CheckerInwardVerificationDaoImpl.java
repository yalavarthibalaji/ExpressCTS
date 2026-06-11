package com.iispl.daoImpl;

import com.iispl.dao.CheckerInwardVerificationDao;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CheckerInwardVerificationDaoImpl implements CheckerInwardVerificationDao {

    @Override
    public List<InwardBatch> findPendingBatches() {

        Session session = null;
        List<InwardBatch> result = new ArrayList<>();

        try {
            session = HibernateUtil.getSessionFactory().openSession();

            String hql = "SELECT DISTINCT b FROM InwardBatch b "
                    + "LEFT JOIN FETCH b.cheques "
                    + "WHERE b.status IN :statuses";

            Query<InwardBatch> query = session.createQuery(hql, InwardBatch.class);
            query.setParameter("statuses", List.of("MakerVerified", "CheckerReferred"));

            result = query.getResultList();

            result.sort((a, b) -> {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });

        } catch (Exception e) {
            System.err.println("[CheckerInwardVerificationDaoImpl] "
                + "findPendingBatches error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }

        return result;
    }

    @Override
    public List<InwardBatch> findClearedBatches(Date fromDate, Date toDate) {

        Session session = null;
        List<InwardBatch> result = new ArrayList<>();

        try {
            session = HibernateUtil.getSessionFactory().openSession();

            LocalDate from = toLocalDate(fromDate);
            LocalDate to   = toLocalDate(toDate);

            StringBuilder hql = new StringBuilder(
                "SELECT DISTINCT b FROM InwardBatch b "
              + "LEFT JOIN FETCH b.checkerActions ca "
              + "LEFT JOIN FETCH ca.checker "
              + "WHERE b.status IN ('Verified', 'CBS_Processed')"); // ← FIXED (was "CLEARED")

            if (from != null) hql.append(" AND b.batchDate >= :fromDate");
            if (to   != null) hql.append(" AND b.batchDate <= :toDate");

            Query<InwardBatch> query = session.createQuery(hql.toString(), InwardBatch.class);

            if (from != null) query.setParameter("fromDate", from);
            if (to   != null) query.setParameter("toDate",   to);

            result = query.getResultList();

            result.sort((a, b) -> {
                if (a.getBatchDate() == null && b.getBatchDate() == null) return 0;
                if (a.getBatchDate() == null) return 1;
                if (b.getBatchDate() == null) return -1;
                return b.getBatchDate().compareTo(a.getBatchDate());
            });

        } catch (Exception e) {
            System.err.println("[CheckerInwardVerificationDaoImpl] "
                + "findClearedBatches error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }

        return result;
    }

    @Override
    public List<InwardCheque> findReturnedCheques(Date fromDate, Date toDate, String batchId) {

        Session session = null;
        List<InwardCheque> result = new ArrayList<>();

        try {
            session = HibernateUtil.getSessionFactory().openSession();

            LocalDate from = toLocalDate(fromDate);
            LocalDate to   = toLocalDate(toDate);

            StringBuilder hql = new StringBuilder(
                "SELECT DISTINCT c FROM InwardCheque c "
              + "LEFT JOIN FETCH c.batch "
              + "LEFT JOIN FETCH c.checkerActions ca "
              + "LEFT JOIN FETCH ca.checker "
              + "WHERE c.status = :status");

            if (from != null)                          hql.append(" AND c.chequeDate >= :fromDate");
            if (to   != null)                          hql.append(" AND c.chequeDate <= :toDate");
            if (batchId != null && !batchId.isEmpty()) hql.append(" AND c.batch.batchId = :batchId");

            Query<InwardCheque> query = session.createQuery(hql.toString(), InwardCheque.class);
            query.setParameter("status", "RETURNED");

            if (from != null)                          query.setParameter("fromDate", from);
            if (to   != null)                          query.setParameter("toDate",   to);
            if (batchId != null && !batchId.isEmpty()) query.setParameter("batchId",  batchId);

            result = query.getResultList();

        } catch (Exception e) {
            System.err.println("[CheckerInwardVerificationDaoImpl] "
                + "findReturnedCheques error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }

        return result;
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}