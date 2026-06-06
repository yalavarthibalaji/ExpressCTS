package com.iispl.daoImpl;

import com.iispl.dao.InwardChequeDao;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Hibernate-based implementation of InwardChequeDao.
 *
 * No JDBC, no Spring — plain Hibernate sessions from HibernateUtil.
 */
public class InwardChequeDaoImpl implements InwardChequeDao {

    // ── Write ─────────────────────────────────────────────────────────────

    @Override
    public void save(InwardCheque cheque) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(cheque);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException(
                    "Failed to save InwardCheque: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveAll(List<InwardCheque> cheques) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            int i = 0;
            for (InwardCheque cheque : cheques) {
                session.persist(cheque);
                // Flush and clear every 50 rows to avoid OutOfMemoryError
                // on large batches (Hibernate first-level cache).
                if (++i % 50 == 0) {
                    session.flush();
                    session.clear();
                }
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException(
                    "Failed to save InwardCheques: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(InwardCheque cheque) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(cheque);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException(
                    "Failed to update InwardCheque: " + e.getMessage(), e);
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────

    @Override
    public InwardCheque findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(InwardCheque.class, id);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fetch InwardCheque by id: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch all cheques for a batch ordered by seqNo ASC.
     *
     * HQL uses batch.id (the FK column mapped on InwardCheque),
     * not the string batchId field on InwardBatch.
     */
    @Override
    public List<InwardCheque> findByBatchId(Long batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM InwardCheque c " +
                            "WHERE c.batch.id = :batchId " +
                            "ORDER BY c.seqNo ASC",
                            InwardCheque.class)
                    .setParameter("batchId", batchId)
                    .list();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fetch InwardCheques by batchId: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch cheques for a batch filtered by a specific repairStatus,
     * ordered by seqNo ASC.
     *
     * Used by the service layer when the UI filter combo is set to
     * NEEDS_REPAIR / REPAIRED / REFERRED_BACK.
     */
    @Override
    public List<InwardCheque> findByBatchIdAndRepairStatus(
            Long batchId, String repairStatus) {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM InwardCheque c " +
                            "WHERE c.batch.id = :batchId " +
                            "  AND UPPER(c.repairStatus) = UPPER(:repairStatus) " +
                            "ORDER BY c.seqNo ASC",
                            InwardCheque.class)
                    .setParameter("batchId", batchId)
                    .setParameter("repairStatus", repairStatus)
                    .list();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fetch InwardCheques by batchId + repairStatus: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Count cheques in a batch that still need repair.
     *
     * A cheque "needs repair" when:
     *   repairStatus IS NULL  (never touched)
     *   OR repairStatus = 'NEEDS_REPAIR'
     *
     * The composer uses this via the service to block the
     * "Proceed to Step 2" button until the count reaches 0.
     */
    @Override
    public long countPendingRepairsByBatchId(Long batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(c) FROM InwardCheque c " +
                            "WHERE c.batch.id = :batchId " +
                            "  AND (c.repairStatus IS NULL " +
                            "       OR UPPER(c.repairStatus) = 'NEEDS_REPAIR')",
                            Long.class)
                    .setParameter("batchId", batchId)
                    .uniqueResult();
            return count != null ? count : 0L;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to count pending repairs for batchId: " + e.getMessage(), e);
        }
    }
}