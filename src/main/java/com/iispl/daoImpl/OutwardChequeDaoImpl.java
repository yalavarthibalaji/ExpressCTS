package com.iispl.daoImpl;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import com.iispl.dao.OutwardChequeDao;
import com.iispl.db.HibernateUtil;
import com.iispl.entity.OutwardCheque;

public class OutwardChequeDaoImpl implements OutwardChequeDao {

	@Override
	public void saveCheque(OutwardCheque cheque) {
		Transaction transaction = null;
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			transaction = session.beginTransaction();
			session.persist(cheque);
			transaction.commit();
			System.out.println("Cheque saved to master: " + cheque.getChequeNumber());
		} catch (Exception e) {
			if (transaction != null) {
				transaction.rollback();
			}
			e.printStackTrace();
			throw new RuntimeException("Failed to save cheque: " + e.getMessage());
		}
	}

	@Override
	public List<OutwardCheque> findAllByBatchId(Long batchId) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			Query<OutwardCheque> query = session.createQuery("FROM OutwardCheque WHERE batch.id = :batchId",
					OutwardCheque.class);
			query.setParameter("batchId", batchId);
			return query.getResultList();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to fetch cheques: " + e.getMessage());
		}
	}

	@Override
	public OutwardCheque findByChequeId(String chequeId) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			Query<OutwardCheque> query = session.createQuery("FROM OutwardCheque WHERE chequeId = :chequeId",
					OutwardCheque.class);
			query.setParameter("chequeId", chequeId);
			return query.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to fetch cheque: " + e.getMessage());
		}
	}
}