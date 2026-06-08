package com.iispl.daoImpl;

import com.iispl.dao.OutwardExportDao;
import com.iispl.entity.outward.OutwardExport;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * File    : com/iispl/daoImpl/OutwardExportDaoImpl.java
 * Purpose : Implementation of OutwardExportDao using native SQL + Hibernate.
 */
public class OutwardExportDaoImpl implements OutwardExportDao {

    // ════════════════════════════════════════════════════
    //  Save
    // ════════════════════════════════════════════════════

    @Override
    public OutwardExport save(OutwardExport export) {
        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();
            session.persist(export);
            tx.commit();
            System.out.println("OutwardExportDao → Saved export: "
                    + export.getFileType() + " / " + export.getFileName());
            return export;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("OutwardExportDao → save failed: " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Find by batch
    // ════════════════════════════════════════════════════

    @Override
    public List<OutwardExport> findByBatchId(Long batchDbId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_exports "
                       + "WHERE batch_id = :batchId "
                       + "ORDER BY generated_at DESC";
            NativeQuery<OutwardExport> q =
                    session.createNativeQuery(sql, OutwardExport.class);
            q.setParameter("batchId", batchDbId);
            return q.list();
        } catch (Exception e) {
            System.err.println("OutwardExportDao → findByBatchId failed: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Find most recent export by batch + fileType
    // ════════════════════════════════════════════════════

    @Override
    public OutwardExport findLatestByBatchAndType(Long batchDbId, String fileType) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_exports "
                       + "WHERE batch_id  = :batchId "
                       + "AND   file_type = :fileType "
                       + "ORDER BY generated_at DESC "
                       + "LIMIT 1";
            NativeQuery<OutwardExport> q =
                    session.createNativeQuery(sql, OutwardExport.class);
            q.setParameter("batchId",  batchDbId);
            q.setParameter("fileType", fileType);
            return q.uniqueResult();
        } catch (Exception e) {
            System.err.println("OutwardExportDao → findLatestByBatchAndType failed: "
                    + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Mark as transmitted
    // ════════════════════════════════════════════════════

    @Override
    public boolean markTransmitted(Long exportId) {
        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();
            String sql = "UPDATE outward_exports "
                       + "SET status = 'TRANSMITTED', "
                       + "    transmitted_at = NOW() "
                       + "WHERE id = :id";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("id", exportId);
            int rows = q.executeUpdate();
            tx.commit();
            System.out.println("OutwardExportDao → Marked TRANSMITTED for export id="
                    + exportId);
            return rows > 0;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("OutwardExportDao → markTransmitted failed: "
                    + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Count by fileType
    // ════════════════════════════════════════════════════

    @Override
    public int countByFileType(String fileType) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT COUNT(*) FROM outward_exports "
                       + "WHERE file_type = :fileType";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("fileType", fileType);
            Number count = (Number) q.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            System.err.println("OutwardExportDao → countByFileType failed: "
                    + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }
}