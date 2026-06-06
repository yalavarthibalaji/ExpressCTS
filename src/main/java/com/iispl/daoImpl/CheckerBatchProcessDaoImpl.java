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
		InwardBatch result = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			String hql = "SELECT DISTINCT b FROM InwardBatch b " + "LEFT JOIN FETCH b.cheques "
					+ "WHERE b.batchId = :batchId";

			Query<InwardBatch> query = session.createQuery(hql, InwardBatch.class);
			query.setParameter("batchId", batchId);

			result = query.uniqueResult();
		} catch (Exception e) {
			System.err.println("Error fetching batch for processing: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
		}
		return result;
	}

	@Override
	public void saveCheckerActions(List<InwardCheckerAction> actions) {

		Session session = null;
		Transaction tx = null;

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
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
			System.err.println("Error saving checker actions: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to save checker actions. Transaction rolled back.", e);
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
		}
	}

	@Override
	public void updateBatchStatus(InwardBatch batch) {

		Session session = null;
		Transaction tx = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();

			batch.setStatus("CLEARED");
			session.merge(batch);
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
			System.err.println("Error updating batch status: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to update branch status. Transaction rolled back.", e);
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
		}
	}

	private String mapActionToStatus(String action) {
		if (action == null)
			return "RECEIVED";

		switch (action.toUpperCase()) {
		case "ACCEPTED":
			return "ACCEPTED";

		case "RETURNED":
			return "RETURNED";

		case "SEND_BACK":
			return "SEND_BACK";

		default:
			return "RECEIVED";
		}
	}

}
