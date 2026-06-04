//package com.iispl.daoImpl;
//
//import com.iispl.dao.OutwardBatchDao;
//import com.iispl.entity.outward.OutwardBatch;
//import com.iispl.util.HibernateUtil;
//import org.hibernate.Session;
//import org.hibernate.Transaction;
//import org.hibernate.query.NativeQuery;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * File    : com/iispl/daoImpl/OutwardBatchDaoImpl.java
// * Purpose : Implementation of OutwardBatchDao using native SQL + Hibernate.
// */
//public class OutwardBatchDaoImpl implements OutwardBatchDao {
//
//    @Override
//    public OutwardBatch save(OutwardBatch batch) {
//        Session session = HibernateUtil.getSessionFactory().openSession();
//        Transaction tx = null;
//        try {
//            tx = session.beginTransaction();
//            session.save(batch);
//            tx.commit();
//            System.out.println("OutwardBatchDao → Saved batch: " + batch.getBatchId());
//            return batch;
//        } catch (Exception e) {
//            if (tx != null) tx.rollback();
//            System.err.println("OutwardBatchDao → Save failed: " + e.getMessage());
//            e.printStackTrace();
//            return null;
//        } finally {
//            session.close();
//        }
//    }
//
//    @Override
//    public OutwardBatch findByBatchId(String batchId) {
//        Session session = HibernateUtil.getSessionFactory().openSession();
//        try {
//            String sql = "SELECT * FROM outward_batch WHERE batch_id = :batchId";
//            NativeQuery<OutwardBatch> query = session.createNativeQuery(sql, OutwardBatch.class);
//            query.setParameter("batchId", batchId);
//            return query.uniqueResult();
//        } catch (Exception e) {
//            System.err.println("OutwardBatchDao → findByBatchId failed: " + e.getMessage());
//            return null;
//        } finally {
//            session.close();
//        }
//    }
//
//    @Override
//    public List<OutwardBatch> findByCreatedBy(Long userId) {
//        Session session = HibernateUtil.getSessionFactory().openSession();
//        try {
//            String sql = "SELECT * FROM outward_batch "
//                       + "WHERE created_by = :userId "
//                       + "ORDER BY created_at DESC";
//            NativeQuery<OutwardBatch> query = session.createNativeQuery(sql, OutwardBatch.class);
//            query.setParameter("userId", userId);
//            return query.list();
//        } catch (Exception e) {
//            System.err.println("OutwardBatchDao → findByCreatedBy failed: " + e.getMessage());
//            return new ArrayList<>();
//        } finally {
//            session.close();
//        }
//    }
//
//    @Override
//    public boolean updateStatus(Long batchId, String newStatus) {
//        Session session = HibernateUtil.getSessionFactory().openSession();
//        Transaction tx = null;
//        try {
//            tx = session.beginTransaction();
//            String sql = "UPDATE outward_batch "
//                       + "SET status = :status, updated_at = NOW() "
//                       + "WHERE id = :id";
//            NativeQuery<?> query = session.createNativeQuery(sql);
//            query.setParameter("status", newStatus);
//            query.setParameter("id", batchId);
//            int rows = query.executeUpdate();
//            tx.commit();
//            return rows > 0;
//        } catch (Exception e) {
//            if (tx != null) tx.rollback();
//            System.err.println("OutwardBatchDao → updateStatus failed: " + e.getMessage());
//            return false;
//        } finally {
//            session.close();
//        }
//    }
//
//    @Override
//    public boolean existsByBatchId(String batchId) {
//        Session session = HibernateUtil.getSessionFactory().openSession();
//        try {
//            String sql = "SELECT COUNT(*) FROM outward_batch WHERE batch_id = :batchId";
//            NativeQuery<?> query = session.createNativeQuery(sql);
//            query.setParameter("batchId", batchId);
//            Number count = (Number) query.uniqueResult();
//            return count != null && count.intValue() > 0;
//        } catch (Exception e) {
//            System.err.println("OutwardBatchDao → existsByBatchId failed: " + e.getMessage());
//            return false;
//        } finally {
//            session.close();
//        }
//    }
//
//    @Override
//    public int countBatchesToday(String datePrefix) {
//        Session session = HibernateUtil.getSessionFactory().openSession();
//        try {
//            // datePrefix format: B-2026-0603
//            // batch_id format  : B-2026-0603-001
//            // LIKE 'B-2026-0603-%' counts all batches created today
//            String sql = "SELECT COUNT(*) FROM outward_batch "
//                       + "WHERE batch_id LIKE :prefix";
//            NativeQuery<?> query = session.createNativeQuery(sql);
//            query.setParameter("prefix", datePrefix + "-%");
//            Number count = (Number) query.uniqueResult();
//            return count != null ? count.intValue() : 0;
//        } catch (Exception e) {
//            System.err.println("OutwardBatchDao → countBatchesToday failed: " + e.getMessage());
//            return 0;
//        } finally {
//            session.close();
//        }
//    }
//}
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

    @Override
    public OutwardBatch save(OutwardBatch batch) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(batch);
            tx.commit();
            System.out.println("OutwardBatchDao → Saved batch: " + batch.getBatchId());
            return batch;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("OutwardBatchDao → Save failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            session.close();
        }
    }

    @Override
    public OutwardBatch findByBatchId(String batchId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_batch WHERE batch_id = :batchId";
            NativeQuery<OutwardBatch> query = session.createNativeQuery(sql, OutwardBatch.class);
            query.setParameter("batchId", batchId);
            return query.uniqueResult();
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → findByBatchId failed: " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    @Override
    public List<OutwardBatch> findByCreatedBy(Long userId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_batch "
                       + "WHERE created_by = :userId "
                       + "ORDER BY created_at DESC";
            NativeQuery<OutwardBatch> query = session.createNativeQuery(sql, OutwardBatch.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → findByCreatedBy failed: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    @Override
    public boolean updateStatus(Long batchId, String newStatus) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            String sql = "UPDATE outward_batch "
                       + "SET status = :status, updated_at = NOW() "
                       + "WHERE id = :id";
            NativeQuery<?> query = session.createNativeQuery(sql);
            query.setParameter("status", newStatus);
            query.setParameter("id", batchId);
            int rows = query.executeUpdate();
            tx.commit();
            return rows > 0;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("OutwardBatchDao → updateStatus failed: " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean existsByBatchId(String batchId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT COUNT(*) FROM outward_batch WHERE batch_id = :batchId";
            NativeQuery<?> query = session.createNativeQuery(sql);
            query.setParameter("batchId", batchId);
            Number count = (Number) query.uniqueResult();
            return count != null && count.intValue() > 0;
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → existsByBatchId failed: " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean existsByFileNameAndMaker(String fileName, Long makerId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            // Check if this maker already uploaded a batch with the same file name
            // that wasn't rejected — ignore REJECTED batches so reupload is allowed
            String sql = "SELECT COUNT(*) FROM outward_batch "
                       + "WHERE file_path = :fileName "
                       + "AND created_by = :makerId "
                       + "AND status != 'REJECTED'";
            NativeQuery<?> query = session.createNativeQuery(sql);
            query.setParameter("fileName", fileName);
            query.setParameter("makerId", makerId);
            Number count = (Number) query.uniqueResult();
            return count != null && count.intValue() > 0;
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → existsByFileNameAndMaker failed: " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    @Override
    public int countBatchesToday(String datePrefix) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            // datePrefix format: B-2026-0603
            // batch_id format  : B-2026-0603-001
            // LIKE 'B-2026-0603-%' counts all batches created today
            String sql = "SELECT COUNT(*) FROM outward_batch "
                       + "WHERE batch_id LIKE :prefix";
            NativeQuery<?> query = session.createNativeQuery(sql);
            query.setParameter("prefix", datePrefix + "-%");
            Number count = (Number) query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            System.err.println("OutwardBatchDao → countBatchesToday failed: " + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }
}