package com.iispl.daoImpl;

import com.iispl.dao.InwardBatchDao;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InwardBatchDaoImpl implements InwardBatchDao {

    private static final Logger LOG =
            Logger.getLogger(InwardBatchDaoImpl.class.getName());

    @Override
    public void save(InwardBatch batch) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();
            session.persist(batch);
            session.flush();
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
            return session.createQuery(
                    "FROM InwardBatch ORDER BY createdAt DESC",
                    InwardBatch.class).list();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch InwardBatches: " + e.getMessage(), e);
        }
    }

    @Override
    public List<InwardBatch> findPendingCheckerBatches() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.createNativeQuery(
                "SELECT * FROM inward_batch " +
                "WHERE status IN ('MakerVerified', 'CheckerReferred') " +
                "ORDER BY created_at ASC",
                InwardBatch.class
            ).list();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findPendingCheckerBatches failed", e);
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    @Override
    public List<InwardBatch> findRepairEligibleBatches() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM InwardBatch b " +
                    "WHERE b.status IN ('RECEIVED', 'PARSED') " +
                    "AND b.micrErrorCount > 0 " +
                    "ORDER BY b.createdAt DESC",
                    InwardBatch.class).list();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findRepairEligibleBatches failed", e);
            return new ArrayList<>();
        }
    }

    @Override
    public InwardBatch findByBatchId(String batchId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            NativeQuery<InwardBatch> q = session.createNativeQuery(
                    "SELECT * FROM inward_batch WHERE batch_id = :batchId",
                    InwardBatch.class);
            q.setParameter("batchId", batchId);
            return q.uniqueResult();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findByBatchId failed, batchId=" + batchId, e);
            return null;
        } finally {
            session.close();
        }
    }

    @Override
    public void updateBatchStatus(String batchId, String status, String repairStatus) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "UPDATE InwardBatch b " +
                    "SET b.status = :status, b.repairStatus = :repairStatus " +
                    "WHERE b.batchId = :batchId")
                   .setParameter("status",       status)
                   .setParameter("repairStatus", repairStatus)
                   .setParameter("batchId",      batchId)
                   .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "updateBatchStatus failed, batchId=" + batchId, e);
            throw new RuntimeException("updateBatchStatus failed", e);
        }
    }

    @Override
    public void updateBatchMicrErrorCount(String batchId, int count) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "UPDATE InwardBatch b SET b.micrErrorCount = :cnt " +
                    "WHERE b.batchId = :batchId")
                   .setParameter("cnt",     count)
                   .setParameter("batchId", batchId)
                   .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE,
                    "updateBatchMicrErrorCount failed, batchId=" + batchId, e);
            throw new RuntimeException("updateBatchMicrErrorCount failed", e);
        }
    }

    @Override
    public int countAllBatches() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Number result = (Number) session.createNativeQuery(
                    "SELECT COUNT(*) FROM inward_batch").uniqueResult();
            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "countAllBatches failed", e);
            return 0;
        } finally {
            session.close();
        }
    }

    @Override
    public int countClearedBatches() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Number result = (Number) session.createNativeQuery(
                    "SELECT COUNT(*) FROM inward_batch " +
                    "WHERE status IN ('Verified','CBS_Processed')").uniqueResult();
            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "countClearedBatches failed", e);
            return 0;
        } finally {
            session.close();
        }
    }

    @Override
    public List<InwardBatch> findInwardBatchesByStatus(String status) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.createQuery(
                "FROM InwardBatch b WHERE b.status = :status ORDER BY b.createdAt DESC",
                InwardBatch.class
            ).setParameter("status", status).list();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findInwardBatchesByStatus failed, status=" + status, e);
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }
	
	public List<InwardBatch> findBatchesByStatuses(List<String> statuses) {
	    Session session = HibernateUtil.getSessionFactory().openSession();
	    try {
	        return session.createQuery(
	            "FROM InwardBatch b WHERE b.status IN :statuses ORDER BY b.createdAt DESC",
	            InwardBatch.class
	        ).setParameter("statuses", statuses).list();
	    } catch (Exception e) {
	        LOG.log(Level.SEVERE, "findBatchesByStatuses failed", e);
	        return new ArrayList<>();
	    } finally {
	        session.close();
	    }
	}
}