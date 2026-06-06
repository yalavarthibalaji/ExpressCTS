package com.iispl.daoImpl;

import com.iispl.dao.RejectRepairDao;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RejectRepairDaoImpl — Hibernate (no JDBC, no Spring).
 * Uses HibernateUtil.getSessionFactory() to open sessions.
 */
public class RejectRepairDaoImpl implements RejectRepairDao {

    private static final Logger LOG =
            Logger.getLogger(RejectRepairDaoImpl.class.getName());

    // ════════════════════════════════════════════════
    //  Batch queries
    // ════════════════════════════════════════════════

    @Override
    public List<InwardBatch> findRepairEligibleBatches() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM InwardBatch b " +
                         "WHERE b.status IN ('RECEIVED','PARSED') " +
                         "AND b.micrErrorCount > 0 " +
                         "ORDER BY b.createdAt DESC";
            Query<InwardBatch> q = session.createQuery(hql, InwardBatch.class);
            List<InwardBatch> result = q.getResultList();
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findRepairEligibleBatches failed", e);
            throw new RuntimeException("DB error in findRepairEligibleBatches", e);
        }
    }

    @Override
    public InwardBatch findBatchById(String batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM InwardBatch b WHERE b.batchId = :batchId";
            Query<InwardBatch> q = session.createQuery(hql, InwardBatch.class);
            q.setParameter("batchId", batchId);
            return q.uniqueResult();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findBatchById failed, batchId=" + batchId, e);
            throw new RuntimeException("DB error in findBatchById", e);
        }
    }

    @Override
    public void updateBatchStatus(String batchId, String status, String repairStatus) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String hql = "UPDATE InwardBatch b " +
                         "SET b.status = :status, b.repairStatus = :repairStatus " +
                         "WHERE b.batchId = :batchId";
            session.createQuery(hql)
                   .setParameter("status", status)
                   .setParameter("repairStatus", repairStatus)
                   .setParameter("batchId", batchId)
                   .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "updateBatchStatus failed", e);
            throw new RuntimeException("DB error in updateBatchStatus", e);
        }
    }

    // ════════════════════════════════════════════════
    //  Step 1 — MICR Repair
    // ════════════════════════════════════════════════

    @Override
    public List<InwardCheque> findChequesByBatchId(String batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM InwardCheque c " +
                         "WHERE c.batch.batchId = :batchId " +
                         "ORDER BY c.seqNo ASC";
            Query<InwardCheque> q = session.createQuery(hql, InwardCheque.class);
            q.setParameter("batchId", batchId);
            List<InwardCheque> result = q.getResultList();
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findChequesByBatchId failed, batchId=" + batchId, e);
            throw new RuntimeException("DB error in findChequesByBatchId", e);
        }
    }

    @Override
    public void updateCheque(InwardCheque cheque) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(cheque);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "updateCheque failed, id=" + cheque.getChequeId(), e);
            throw new RuntimeException("DB error in updateCheque", e);
        }
    }

    // ════════════════════════════════════════════════
    //  Step 2 — Date & Amount Repair
    //  Fetch ALL cheques in batch (checker needs to
    //  compare proc vs received date/amount for each)
    // ════════════════════════════════════════════════

    @Override
    public List<InwardCheque> findStep2ChequesByBatchId(String batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Step 2 shows all cheques — user reviews date/amount comparison
            String hql = "FROM InwardCheque c " +
                         "WHERE c.batch.batchId = :batchId " +
                         "ORDER BY c.seqNo ASC";
            Query<InwardCheque> q = session.createQuery(hql, InwardCheque.class);
            q.setParameter("batchId", batchId);
            List<InwardCheque> result = q.getResultList();
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findStep2ChequesByBatchId failed", e);
            throw new RuntimeException("DB error in findStep2ChequesByBatchId", e);
        }
    }

    // ════════════════════════════════════════════════
    //  Step 3 — Payee Name & Account Entry
    // ════════════════════════════════════════════════

    @Override
    public List<InwardCheque> findStep3ChequesByBatchId(String batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Step 3 shows all cheques that need payee + account entry
            String hql = "FROM InwardCheque c " +
                         "WHERE c.batch.batchId = :batchId " +
                         "ORDER BY c.seqNo ASC";
            Query<InwardCheque> q = session.createQuery(hql, InwardCheque.class);
            q.setParameter("batchId", batchId);
            List<InwardCheque> result = q.getResultList();
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findStep3ChequesByBatchId failed", e);
            throw new RuntimeException("DB error in findStep3ChequesByBatchId", e);
        }
    }
}