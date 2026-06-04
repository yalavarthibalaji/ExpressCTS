package com.iispl.daoImpl;

import com.iispl.dao.InwardBatchDao;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

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
}