package com.iispl.daoImpl;

import com.iispl.dao.InwardBatchDao;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Collections;
import java.util.List;

public class InwardBatchDaoImpl implements InwardBatchDao {

    @Override
    public List<InwardBatch> findAll() {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            List<InwardBatch> batches = session
                    .createQuery("FROM InwardBatch ORDER BY createdAt DESC", InwardBatch.class)
                    .getResultList();

            tx.commit();
            return batches;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}