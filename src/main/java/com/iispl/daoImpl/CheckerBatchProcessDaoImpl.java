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

	        // Load batch
	        InwardBatch batch = session.createQuery(
	            "SELECT b FROM InwardBatch b WHERE b.batchId = :batchId",
	            InwardBatch.class
	        ).setParameter("batchId", batchId).uniqueResult();

	        if (batch == null) return null;

	        // Load only cheques that are NOT referred back
	        // SEND_BACK cheques are already saved individually and returned to Maker
	        List<InwardCheque> cheques = session.createQuery(
	            "SELECT c FROM InwardCheque c " +
	            "WHERE c.batch.batchId = :batchId " +
	            "AND c.status NOT IN ('SEND_BACK', 'ACCEPTED', 'RETURNED') " +
	            "ORDER BY c.seqNo ASC",
	            InwardCheque.class
	        ).setParameter("batchId", batchId).getResultList();

	        batch.setCheques(cheques);
	        return batch;

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

    @Override
    public void saveReturnAction(InwardCheckerAction action) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            session.persist(action);

            InwardCheque cheque = action.getInwardCheque();
            if (cheque != null) {
                cheque.setStatus("RETURNED");
                session.merge(cheque);
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            System.err.println("saveReturnAction error: " + e.getMessage());
            throw new RuntimeException("Failed to save return action.", e);
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    @Override
    public void updateBatchStatusTo(InwardBatch batch, String statusValue) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            batch.setStatus(statusValue);
            session.merge(batch);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            System.err.println("updateBatchStatusTo error: " + e.getMessage());
            throw new RuntimeException("Failed to update batch status to " + statusValue + ".", e);
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    @Override
    public void saveReferBackAction(InwardCheckerAction action) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // 1. Save the action record
            session.persist(action);

            // 2. Update referred cheque — status + repair_status + refer_back_module
            InwardCheque cheque = action.getInwardCheque();
            if (cheque != null) {
                cheque.setStatus("SEND_BACK");
                cheque.setReferBackModule(action.getReferBackModule());
                cheque.setRepairStatus(mapModuleToRepairStatus(action.getReferBackModule()));
                session.merge(cheque);
            }

            // 3. Update batch status to CheckerReferred in same transaction
            //    This blocks Maker from proceeding until Maker fixes and re-sends
            InwardBatch batch = action.getInwardBatch();
            if (batch != null) {
                InwardBatch managed = session.get(InwardBatch.class, batch.getId());
                if (managed != null) {
                    managed.setStatus("CheckerReferred");
                    session.merge(managed);
                    // Keep in-memory object in sync so UI updates immediately
                    batch.setStatus("CheckerReferred");
                }
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            System.err.println("saveReferBackAction error: " + e.getMessage());
            throw new RuntimeException("Failed to save refer-back action.", e);
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    // Maps refer-back module code → inward_cheque.repair_status value
    private String mapModuleToRepairStatus(String module) {
        if (module == null) return "NEEDS_REPAIR";
        switch (module.toUpperCase()) {
            case "MICR_REPAIR":   return "REFERRED_MICR";
            case "DATE_AMOUNT":   return "REFERRED_DATEAMOUNT";
            case "PAYEE_ACCOUNT": return "REFERRED_PAYEEACCOUNT";
            default:              return "NEEDS_REPAIR";
        }
    }


    // Maps checker action string → inward_cheque.status column value
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