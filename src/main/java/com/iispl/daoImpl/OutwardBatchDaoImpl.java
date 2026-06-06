package com.iispl.daoImpl;

import com.iispl.dao.OutwardBatchDao;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * File    : com/iispl/daoImpl/OutwardBatchDaoImpl.java
 * Purpose : Implementation of OutwardBatchDao using native SQL + Hibernate.
 */
public class OutwardBatchDaoImpl implements OutwardBatchDao {

    // ════════════════════════════════════════════════════
    //  Save
    // ════════════════════════════════════════════════════

    @Override
    public OutwardBatch save(OutwardBatch batch) {
        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();
            session.persist(batch);
            tx.commit();
            System.out.println("OutwardBatchDao → Saved batch: "
                    + batch.getBatchId());
            return batch;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("OutwardBatchDao → save failed: "
                    + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Find by batch_id string
    // ════════════════════════════════════════════════════

    @Override
    public OutwardBatch findByBatchId(String batchId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_batch "
                       + "WHERE batch_id = :batchId";
            NativeQuery<OutwardBatch> q =
                    session.createNativeQuery(sql, OutwardBatch.class);
            q.setParameter("batchId", batchId);
            return q.uniqueResult();
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → findByBatchId failed: "
                    + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Find by maker (all batches for one user)
    // ════════════════════════════════════════════════════

    @Override
    public List<OutwardBatch> findByCreatedBy(Long makerId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_batch "
                       + "WHERE created_by = :makerId "
                       + "ORDER BY created_at DESC";
            NativeQuery<OutwardBatch> q =
                    session.createNativeQuery(sql, OutwardBatch.class);
            q.setParameter("makerId", makerId);
            return q.list();
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → findByCreatedBy failed: "
                    + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Update status
    // ════════════════════════════════════════════════════

    @Override
    public boolean updateStatus(Long batchDbId, String newStatus) {
        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();
            String sql = "UPDATE outward_batch "
                       + "SET status = :status, updated_at = NOW() "
                       + "WHERE id = :id";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("status", newStatus);
            q.setParameter("id",     batchDbId);
            int rows = q.executeUpdate();
            tx.commit();
            System.out.println("OutwardBatchDao → Status updated to "
                    + newStatus + " for batch id=" + batchDbId);
            return rows > 0;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("OutwardBatchDao → updateStatus failed: "
                    + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Existence checks
    // ════════════════════════════════════════════════════

    @Override
    public boolean existsByBatchId(String batchId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT COUNT(*) FROM outward_batch "
                       + "WHERE batch_id = :batchId";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("batchId", batchId);
            Number count = (Number) q.uniqueResult();
            return count != null && count.intValue() > 0;
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → existsByBatchId failed: "
                    + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean existsByFilePathAndMaker(String filePath, Long makerId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT COUNT(*) FROM outward_batch "
                       + "WHERE file_path = :filePath "
                       + "AND created_by = :makerId "
                       + "AND status != 'REJECTED'";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("filePath", filePath);
            q.setParameter("makerId",  makerId);
            Number count = (Number) q.uniqueResult();
            return count != null && count.intValue() > 0;
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → existsByFilePathAndMaker failed: "
                    + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Count batches today (for batch ID generation)
    // ════════════════════════════════════════════════════

    @Override
    public int countBatchesToday(String datePrefix) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT COUNT(*) FROM outward_batch "
                       + "WHERE batch_id LIKE :prefix";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("prefix", datePrefix + "-%");
            Number count = (Number) q.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → countBatchesToday failed: "
                    + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Find batches needing MICR repair (sidebar access)
    // ════════════════════════════════════════════════════

    @Override
    public List<OutwardBatch> findNeedsRepairByMaker(Long makerId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_batch "
                       + "WHERE created_by = :makerId "
                       + "AND status = 'NEEDS_REPAIR' "
                       + "ORDER BY created_at DESC";
            NativeQuery<OutwardBatch> q =
                    session.createNativeQuery(sql, OutwardBatch.class);
            q.setParameter("makerId", makerId);
            return q.list();
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → findNeedsRepairByMaker failed: "
                    + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Find batches ready for account entry (sidebar access)
    // ════════════════════════════════════════════════════

    @Override
    public List<OutwardBatch> findEntryReadyByMaker(Long makerId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_batch "
                       + "WHERE created_by = :makerId "
                       + "AND status = 'ENTRY_DONE' "
                       + "ORDER BY created_at DESC";
            NativeQuery<OutwardBatch> q =
                    session.createNativeQuery(sql, OutwardBatch.class);
            q.setParameter("makerId", makerId);
            return q.list();
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → findEntryReadyByMaker failed: "
                    + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Find ALL batches (admin view)
    // ════════════════════════════════════════════════════

    @Override
    public List<OutwardBatch> findAll() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_batch "
                       + "ORDER BY created_at DESC";
            NativeQuery<OutwardBatch> q =
                    session.createNativeQuery(sql, OutwardBatch.class);
            return q.list();
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → findAll failed: "
                    + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }
 // ════════════════════════════════════════════════════
    //  Checker Outward — Queue
    // ════════════════════════════════════════════════════

    @Override
    public List<OutwardBatch> findCheckerQueueBatches() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_batch "
                       + "WHERE status IN ('SUBMITTED', "
                       +                  "'CHECKER_IN_PROGRESS', "
                       +                  "'CHECKER_HOLD') "
                       + "ORDER BY created_at ASC";
            NativeQuery<OutwardBatch> q =
                    session.createNativeQuery(sql, OutwardBatch.class);
            return q.list();
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → findCheckerQueueBatches failed: "
                    + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Checker Outward — Approved (ready for DEM Export)
    // ════════════════════════════════════════════════════

    @Override
    public List<OutwardBatch> findCheckerApprovedBatches() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_batch "
                       + "WHERE status = 'CHECKER_APPROVED' "
                       + "ORDER BY updated_at DESC";
            NativeQuery<OutwardBatch> q =
                    session.createNativeQuery(sql, OutwardBatch.class);
            return q.list();
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → findCheckerApprovedBatches failed: "
                    + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Checker Outward — Count by status (dashboard summary)
    // ════════════════════════════════════════════════════

    @Override
    public int countByStatus(String status) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT COUNT(*) FROM outward_batch "
                       + "WHERE status = :status";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("status", status);
            Number count = (Number) q.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → countByStatus failed: "
                    + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }
}