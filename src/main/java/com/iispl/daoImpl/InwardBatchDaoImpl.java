package com.iispl.daoImpl;

import com.iispl.dao.InwardBatchDao;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.util.HibernateUtil;
import org.hibernate.Session;

import org.hibernate.Transaction;


import org.hibernate.query.NativeQuery;

import java.util.ArrayList;

import java.util.List;

public class InwardBatchDaoImpl implements InwardBatchDao {

	@Override
	public void save(InwardBatch batch) {
	    Transaction tx = null;
	    Session session = HibernateUtil.getSessionFactory().openSession();
	    try {
	        tx = session.beginTransaction();
	        session.persist(batch);
	        session.flush();   // ✅ forces INSERT on batch first → ID assigned → cheques can reference it
	        tx.commit();
	    } catch (Exception e) {
	        if (tx != null) tx.rollback();
	        throw new RuntimeException("Failed to save InwardBatch: " + e.getMessage(), e);
	    } finally {
	        session.close();
	    }
	}

    @Override
    public List<InwardBatch> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM InwardBatch ORDER BY createdAt DESC", InwardBatch.class)
                          .list();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch InwardBatches: " + e.getMessage(), e);
        }
    }

    @Override
    public List<InwardBatch> findPendingCheckerBatches() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            // FIX: MakerVerified is the correct status when maker sends to checker
            return session.createNativeQuery(
                "SELECT * FROM inward_batch WHERE status = 'MakerVerified' ORDER BY created_at ASC",
                InwardBatch.class
            ).list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    @Override
    public List<InwardBatch> findInwardBatchesByStatus(String status) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM InwardBatch WHERE status = :status ORDER BY createdAt DESC",
                    InwardBatch.class)
                .setParameter("status", status)
                .list();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch InwardBatches by status: " + e.getMessage(), e);
        }
    }
    
    public InwardBatch findByBatchId(String batchId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            NativeQuery<InwardBatch> q = session.createNativeQuery(
                "SELECT * FROM inward_batch WHERE batch_id = :batchId",
                InwardBatch.class
            );
            q.setParameter("batchId", batchId);
            return q.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            session.close();
        }
    }

    @Override
    public int countAllBatches() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Number result = (Number) session.createNativeQuery(
                "SELECT COUNT(*) FROM inward_batch"
            ).uniqueResult();
            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            session.close();
        }
    }

    @Override
    public int countClearedBatches() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            // FIX: Verified = checker submitted, CBS_Processed = debit generated
            Number result = (Number) session.createNativeQuery(
                "SELECT COUNT(*) FROM inward_batch WHERE status IN ('Verified','CBS_Processed')"
            ).uniqueResult();
            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            session.close();
        }
    }
}