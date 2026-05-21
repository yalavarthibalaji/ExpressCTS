package com.iispl.daoImpl;

import com.iispl.dao.OutwardChequeStagingDao;
import com.iispl.db.HibernateUtil;
import com.iispl.entity.OutwardChequeStaging;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.util.List;

public class OutwardChequeStagingDaoImpl implements OutwardChequeStagingDao {

    @Override
    public void saveStagingCheque(OutwardChequeStaging stagingCheque) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(stagingCheque);
            transaction.commit();
            System.out.println("Staging cheque saved: " + stagingCheque.getChequeNumber());
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            throw new RuntimeException("Failed to save staging cheque: " + e.getMessage());
        }
    }

    @Override
    public List<OutwardChequeStaging> findAllByBatchId(String batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<OutwardChequeStaging> query = session.createQuery(
                "FROM OutwardChequeStaging WHERE batchId = :batchId",
                OutwardChequeStaging.class);
            query.setParameter("batchId", batchId);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch staging cheques: " + e.getMessage());
        }
    }

    @Override
    public List<OutwardChequeStaging> findPendingByBatchId(String batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<OutwardChequeStaging> query = session.createQuery(
                "FROM OutwardChequeStaging WHERE batchId = :batchId " +
                "AND stagingStatus = 'PENDING'",
                OutwardChequeStaging.class);
            query.setParameter("batchId", batchId);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch pending staging cheques: " + e.getMessage());
        }
    }

    @Override
    public void updateStagingStatus(Long id, String status, String reviewedBy) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            Query query = session.createQuery(
                "UPDATE OutwardChequeStaging " +
                "SET stagingStatus = :status, " +
                "reviewedBy = :reviewedBy, " +
                "reviewedAt = :reviewedAt " +
                "WHERE id = :id");
            query.setParameter("status", status);
            query.setParameter("reviewedBy", reviewedBy);
            query.setParameter("reviewedAt", LocalDateTime.now());
            query.setParameter("id", id);
            query.executeUpdate();

            transaction.commit();
            System.out.println("Staging status updated for id: " + id + " → " + status);

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            throw new RuntimeException("Failed to update staging status: " + e.getMessage());
        }
    }
}