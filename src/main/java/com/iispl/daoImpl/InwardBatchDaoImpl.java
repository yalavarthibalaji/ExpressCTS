package com.iispl.daoImpl;

import com.iispl.dao.InwardBatchDao;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import java.util.ArrayList;
import java.util.List;

public class InwardBatchDaoImpl implements InwardBatchDao {

    @Override
    public List<InwardBatch> findPendingCheckerBatches() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            NativeQuery<InwardBatch> q = session.createNativeQuery(
                "SELECT * FROM inward_batch " +
                "WHERE status IN ('RECEIVED', 'PENDING_CHECKER') " +
                "ORDER BY created_at ASC",
                InwardBatch.class
            );
            return q.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    @Override
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
}