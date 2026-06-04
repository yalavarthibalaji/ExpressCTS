package com.iispl.daoImpl;

import com.iispl.dao.InwardChequeDao;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class InwardChequeDaoImpl implements InwardChequeDao {

    @Override
    public void save(InwardCheque cheque) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();
            session.persist(cheque);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save InwardCheque: " + e.getMessage(), e);
        } finally {
            session.close();
        }
    }

    @Override
    public void saveAll(List<InwardCheque> cheques) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();
            for (InwardCheque cheque : cheques) {
                session.persist(cheque);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save InwardCheques: " + e.getMessage(), e);
        } finally {
            session.close();
        }
    }

    @Override
    public InwardCheque findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(InwardCheque.class, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch InwardCheque by id: " + e.getMessage(), e);
        }
    }

    @Override
    public List<InwardCheque> findByBatchId(Long batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM InwardCheque WHERE batch.id = :batchId ORDER BY seqNo ASC",
                    InwardCheque.class)
                .setParameter("batchId", batchId)
                .list();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch InwardCheques by batchId: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(InwardCheque cheque) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();
            session.merge(cheque);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to update InwardCheque: " + e.getMessage(), e);
        } finally {
            session.close();
        }
    }
}