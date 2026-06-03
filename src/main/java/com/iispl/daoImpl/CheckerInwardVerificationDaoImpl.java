package com.iispl.daoImpl;

import com.iispl.dao.CheckerInwardVerificationDao;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.util.*;

import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CheckerInwardVerificationDaoImpl implements CheckerInwardVerificationDao {

	@Override
	public List<InwardBatch> findPendingBatches() {

		Session session = null;
		List<InwardBatch> result = new ArrayList<>();

		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// JOIN FETCH cheques — needed by composer to build presenting banks string
			// JOIN FETCH checkerActions — needed by composer for countActions /
			// getLastCheckerName
			String hql = "SELECT DISTINCT b FROM InwardBatch b " + "LEFT JOIN FETCH b.cheques "
					+ "LEFT JOIN FETCH b.checkerActions ca " + "LEFT JOIN FETCH ca.checker "
					+ "WHERE b.status = :status " + "ORDER BY b.createdAt DESC";

			Query<InwardBatch> query = session.createQuery(hql, InwardBatch.class);
			query.setParameter("status", "RECEIVED");

			result = query.getResultList();

		} catch (Exception e) {
			System.err.println("Error fetching pending inward batches: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
		}

		return result;
	}

	@Override
	public List<InwardBatch> findClearedBatches(Date fromDate, Date toDate) {

		Session session = null;
		List<InwardBatch> result = new ArrayList<>();

		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// Convert java.util.Date → LocalDate (entity uses LocalDate for batchDate)
			LocalDate from = toLocalDate(fromDate);
			LocalDate to = toLocalDate(toDate);

			// JOIN FETCH checkerActions — needed for acceptedCount, returnedCount,
			// clearedBy
			StringBuilder hql = new StringBuilder(
					"SELECT DISTINCT b FROM InwardBatch b " + "LEFT JOIN FETCH b.checkerActions ca "
							+ "LEFT JOIN FETCH ca.checker " + "WHERE b.status = :status");

			if (from != null) {
				hql.append(" AND b.batchDate >= :fromDate");
			}
			if (to != null) {
				hql.append(" AND b.batchDate <= :toDate");
			}

			// Note: ORDER BY after JOIN FETCH DISTINCT must use b field, not ca
			hql.append(" ORDER BY b.batchDate DESC");

			Query<InwardBatch> query = session.createQuery(hql.toString(), InwardBatch.class);
			query.setParameter("status", "CLEARED");

			if (from != null) {
				query.setParameter("fromDate", from);
			}
			if (to != null) {
				query.setParameter("toDate", to);
			}

			result = query.getResultList();

		} catch (Exception e) {
			System.err.println("Error fetching cleared inward batches: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
		}

		return result;
	}

	@Override
	public List<InwardCheque> findReturnedCheques(Date fromDate, Date toDate, String batchId) {

		Session session = null;
		List<InwardCheque> result = new ArrayList<>();

		try {
			session = HibernateUtil.getSessionFactory().openSession();

			LocalDate from = toLocalDate(fromDate);
			LocalDate to = toLocalDate(toDate);

			// JOIN FETCH batch — needed for cheque.getBatch().getBatchId()
			// JOIN FETCH checkerActions — needed for returnReason, returnedBy, returnTime
			StringBuilder hql = new StringBuilder("SELECT DISTINCT c FROM InwardCheque c " + "LEFT JOIN FETCH c.batch "
					+ "LEFT JOIN FETCH c.checkerActions ca " + "LEFT JOIN FETCH ca.checker "
					+ "WHERE c.status = :status");

			if (from != null) {
				hql.append(" AND c.chequeDate >= :fromDate");
			}
			if (to != null) {
				hql.append(" AND c.chequeDate <= :toDate");
			}
			if (batchId != null && !batchId.isEmpty()) {
				// c.batch is the InwardBatch object — navigate to batchId field
				hql.append(" AND c.batch.batchId = :batchId");
			}

			hql.append(" ORDER BY c.updatedAt DESC");

			Query<InwardCheque> query = session.createQuery(hql.toString(), InwardCheque.class);
			query.setParameter("status", "RETURNED");

			if (from != null) {
				query.setParameter("fromDate", from);
			}
			if (to != null) {
				query.setParameter("toDate", to);
			}
			if (batchId != null && !batchId.isEmpty()) {
				query.setParameter("batchId", batchId);
			}

			result = query.getResultList();

		} catch (Exception e) {
			System.err.println("Error fetching returned inward cheques: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
		}

		return result;
	}

	private LocalDate toLocalDate(Date date) {
		if (date == null)
			return null;
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}
}