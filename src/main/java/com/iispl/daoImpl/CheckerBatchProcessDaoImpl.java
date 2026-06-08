package com.iispl.daoImpl;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import com.iispl.dao.CheckerBatchProcessDao;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.util.HibernateUtil;

public class CheckerBatchProcessDaoImpl implements CheckerBatchProcessDao {

    @Override
    public InwardBatch findBatchWithCheques(String batchId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Query<InwardBatch> q = session.createQuery(
                "SELECT DISTINCT b FROM InwardBatch b " +
                "LEFT JOIN FETCH b.cheques " +
                "WHERE b.batchId = :batchId",
                InwardBatch.class
            );
            q.setParameter("batchId", batchId);
            return q.uniqueResult();
        } catch (Exception e) {
            System.err.println("findBatchWithCheques error: " + e.getMessage());
            return null;
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    @Override
    public void saveCheckerActions(List<InwardCheckerAction> actions) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            for (InwardCheckerAction action : actions) {
                session.persist(action);

                InwardCheque cheque = action.getInwardCheque();
                if (cheque != null) {
                    cheque.setStatus(mapActionToStatus(action.getAction()));
                    session.merge(cheque);
                }
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            System.err.println("saveCheckerActions error: " + e.getMessage());
            throw new RuntimeException("Failed to save checker actions.", e);
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    @Override
    public void updateBatchStatus(InwardBatch batch) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // FIX: Use "Verified" (InwardStatus enum value) not "CLEARED"
            // After checker submits → Verified
            // This makes batch appear in Reports page as "Pending (debit eligible)"
            batch.setStatus("Verified");
            session.merge(batch);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            System.err.println("updateBatchStatus error: " + e.getMessage());
            throw new RuntimeException("Failed to update batch status.", e);
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    // Maps checker action → inward_cheque.status
    // These match InwardCheque.status column values
    private String mapActionToStatus(String action) {
        if (action == null) return "RECEIVED";
        switch (action.toUpperCase()) {
            case "ACCEPTED":  return "ACCEPTED";
            case "RETURNED":  return "RETURNED";
            case "SEND_BACK": return "SEND_BACK";
            default:          return "RECEIVED";
        }
    }
}