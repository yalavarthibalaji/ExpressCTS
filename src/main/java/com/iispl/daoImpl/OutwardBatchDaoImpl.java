package com.iispl.daoImpl;

import com.iispl.dao.OutwardBatchDao;
import com.iispl.db.HibernateUtil;
import com.iispl.entity.OutwardBatch;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class OutwardBatchDaoImpl implements OutwardBatchDao {

    @Override
    public void saveBatch(OutwardBatch batch) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(batch);
            transaction.commit();
            System.out.println("Batch saved: " + batch.getBatchId());
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            throw new RuntimeException("Failed to save batch: " + e.getMessage());
        }
    }

    @Override
    public OutwardBatch findByBatchId(String batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<OutwardBatch> query = session.createQuery(
                "FROM OutwardBatch WHERE batchId = :batchId", OutwardBatch.class);
            query.setParameter("batchId", batchId);
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find batch: " + e.getMessage());
        }
    }

    @Override
    public int countBatchesTodayForBranch(String branchCode, String date) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // count how many batches were created today for this branch
            // this helps us generate the sequence number in batch id
            Query<Long> query = session.createQuery(
                "SELECT COUNT(b) FROM OutwardBatch b " +
                "WHERE b.branchCode = :branchCode " +
                "AND CAST(b.scannedAt AS date) = CAST(:date AS date)",
                Long.class);
            query.setParameter("branchCode", branchCode);
            query.setParameter("date", date);

            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to count batches: " + e.getMessage());
        }
    }
}