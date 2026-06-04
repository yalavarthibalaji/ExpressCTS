package com.iispl.daoImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;

import com.iispl.dao.OutwardChequeDao;
import com.iispl.entity.User;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.util.HibernateUtil;

/**
 * File    : com/iispl/daoImpl/OutwardChequeDaoImpl.java
 * Purpose : Implementation of OutwardChequeDao using native SQL + Hibernate.
 *           All cheques for a batch are saved in ONE transaction.
 *           If any cheque fails to save, the entire batch cheques roll back.
 */
public class OutwardChequeDaoImpl implements OutwardChequeDao {

    @Override
    public boolean saveAll(List<OutwardCheque> cheques) {
        if (cheques == null || cheques.isEmpty()) {
            System.out.println("OutwardChequeDao → No cheques to save.");
            return false;
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            for (OutwardCheque cheque : cheques) {
                session.save(cheque);
            }

            tx.commit();
            System.out.println("OutwardChequeDao → Saved "
                    + cheques.size() + " cheques successfully.");
            return true;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("OutwardChequeDao → saveAll failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            session.close();
        }
    }

    @Override
    public List<OutwardCheque> findByBatchId(Long batchId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_cheque "
                       + "WHERE batch_id = :batchId "
                       + "ORDER BY seq_no ASC";
            NativeQuery<OutwardCheque> query = session.createNativeQuery(sql, OutwardCheque.class);
            query.setParameter("batchId", batchId);
            return query.list();
        } catch (Exception e) {
            System.err.println("OutwardChequeDao → findByBatchId failed: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    @Override
    public int countMicrErrors(Long batchId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT COUNT(*) FROM outward_cheque "
                       + "WHERE batch_id = :batchId "
                       + "AND is_micr_error = TRUE";
            NativeQuery<?> query = session.createNativeQuery(sql);
            query.setParameter("batchId", batchId);
            Number count = (Number) query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            System.err.println("OutwardChequeDao → countMicrErrors failed: " + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }
    @Override
    public boolean rejectCheque(Long chequeId, Long userId) {
        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();

            // ── 1. Load the cheque (with its batch) ──
            OutwardCheque cheque = session.get(OutwardCheque.class, chequeId);
            if (cheque == null) {
                System.err.println("OutwardChequeDao → rejectCheque: cheque not found id=" + chequeId);
                tx.rollback();
                return false;
            }

            // ── 2. Skip if already rejected ──
            if ("REJECTED".equals(cheque.getStatus())) {
                System.out.println("OutwardChequeDao → cheque already rejected id=" + chequeId);
                tx.rollback();
                return true;
            }

            // ── 3. Update cheque fields ──
            cheque.setStatus("REJECTED");
            cheque.setRejectedAt(LocalDateTime.now());
            cheque.setRejectedReasonCode("MAN");   // MAN = manual reject by maker

            User rejector = new User();
            rejector.setId(userId);
            cheque.setRejectedBy(rejector);

            session.update(cheque);

            // ── 4. Update batch totals ──
            OutwardBatch batch = cheque.getBatch();
            if (batch != null) {
                int        newCount  = Math.max(0, batch.getChequeCount() - 1);
                BigDecimal curAmount = batch.getActualAmount() != null
                                        ? batch.getActualAmount() : BigDecimal.ZERO;
                BigDecimal chqAmount = cheque.getAmount() != null
                                        ? cheque.getAmount() : BigDecimal.ZERO;
                BigDecimal newAmount = curAmount.subtract(chqAmount);

                batch.setChequeCount(newCount);
                batch.setActualAmount(newAmount);
                session.update(batch);

                System.out.println("OutwardChequeDao → Updated batch " + batch.getBatchId()
                        + ": count=" + newCount + ", amount=" + newAmount);
            }

            tx.commit();
            System.out.println("OutwardChequeDao → Cheque " + cheque.getChequeNo() + " rejected.");
            return true;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("OutwardChequeDao → rejectCheque failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            session.close();
        }
    }
}