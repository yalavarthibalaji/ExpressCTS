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
	public List<OutwardCheque> findMicrErrorCheques(Long batchDbId) {
	    Session session = HibernateUtil.getSessionFactory().openSession();
	    try {
	        String sql = "SELECT * FROM outward_cheque "
	                   + "WHERE batch_id = :batchId "
	                   + "AND is_micr_error = TRUE "
	                   + "AND repair_status != 'REPAIRED' "
	                   + "AND status != 'REJECTED' "
	                   + "ORDER BY seq_no ASC";
	        NativeQuery<OutwardCheque> q =
	                session.createNativeQuery(sql, OutwardCheque.class);
	        q.setParameter("batchId", batchDbId);
	        return q.list();
	    } catch (Exception e) {
	        System.err.println("OutwardChequeDao → findMicrErrorCheques failed: "
	                + e.getMessage());
	        return new ArrayList<>();
	    } finally {
	        session.close();
	    }
	}

	@Override
	public boolean updateMicrRepaired(Long chequeId,
	                                   String cityCode,
	                                   String bankCode,
	                                   String branchCode,
	                                   String baseNumber,
	                                   String transactionCode,
	                                   String micrCodeCorrected) {
	    Session     session = HibernateUtil.getSessionFactory().openSession();
	    Transaction tx      = null;
	    try {
	        tx = session.beginTransaction();
	        String sql = "UPDATE outward_cheque "
	                   + "SET city_code          = :cityCode, "
	                   + "    bank_code          = :bankCode, "
	                   + "    branch_code        = :branchCode, "
	                   + "    base_number        = :baseNumber, "
	                   + "    transaction_code   = :transCode, "
	                   + "    micr_code_corrected = :micrCorrected, "
	                   + "    repair_status      = 'REPAIRED', "
	                   + "    updated_at         = NOW() "
	                   + "WHERE id = :chequeId";
	        NativeQuery<?> q = session.createNativeQuery(sql);
	        q.setParameter("cityCode",      cityCode);
	        q.setParameter("bankCode",      bankCode);
	        q.setParameter("branchCode",    branchCode);
	        q.setParameter("baseNumber",    baseNumber);
	        q.setParameter("transCode",     transactionCode);
	        q.setParameter("micrCorrected", micrCodeCorrected);
	        q.setParameter("chequeId",      chequeId);
	        int rows = q.executeUpdate();
	        tx.commit();
	        System.out.println("OutwardChequeDao → MICR repaired for cheque id="
	                + chequeId);
	        return rows > 0;
	    } catch (Exception e) {
	        if (tx != null) tx.rollback();
	        System.err.println("OutwardChequeDao → updateMicrRepaired failed: "
	                + e.getMessage());
	        return false;
	    } finally {
	        session.close();
	    }
	}

	@Override
	public int countPendingMicrRepairs(Long batchDbId) {
	    Session session = HibernateUtil.getSessionFactory().openSession();
	    try {
	        String sql = "SELECT COUNT(*) FROM outward_cheque "
	                   + "WHERE batch_id     = :batchId "
	                   + "AND is_micr_error  = TRUE "
	                   + "AND repair_status != 'REPAIRED' "
	                   + "AND status        != 'REJECTED'";
	        NativeQuery<?> q = session.createNativeQuery(sql);
	        q.setParameter("batchId", batchDbId);
	        Number count = (Number) q.uniqueResult();
	        return count != null ? count.intValue() : 0;
	    } catch (Exception e) {
	        System.err.println("OutwardChequeDao → countPendingMicrRepairs failed: "
	                + e.getMessage());
	        return 0;
	    } finally {
	        session.close();
	    }
	}

	@Override
	public boolean rejectWithReason(Long chequeId,
	                                 String reasonCode,
	                                 String remarks,
	                                 Long userId) {
	    Session     session = HibernateUtil.getSessionFactory().openSession();
	    Transaction tx      = null;
	    try {
	        tx = session.beginTransaction();

	        OutwardCheque cheque = session.get(OutwardCheque.class, chequeId);
	        if (cheque == null) { tx.rollback(); return false; }
	        if ("REJECTED".equals(cheque.getStatus())) {
	            tx.rollback();
	            return true;
	        }

	        cheque.setStatus("REJECTED");
	        cheque.setRejectedAt(LocalDateTime.now());
	        cheque.setRejectedReasonCode(reasonCode);
	        cheque.setRemarks(remarks);
	        User u = new User(); u.setId(userId);
	        cheque.setRejectedBy(u);
	        session.merge(cheque);

	        // Decrement batch totals
	        OutwardBatch batch = cheque.getBatch();
	        if (batch != null) {
	            int        newCount  = Math.max(0, batch.getChequeCount() - 1);
	            BigDecimal cur       = batch.getActualAmount() != null
	                                    ? batch.getActualAmount() : BigDecimal.ZERO;
	            BigDecimal chqAmt    = cheque.getAmount() != null
	                                    ? cheque.getAmount() : BigDecimal.ZERO;
	            batch.setChequeCount(newCount);
	            batch.setActualAmount(cur.subtract(chqAmt));
	            session.merge(batch);
	        }

	        tx.commit();
	        System.out.println("OutwardChequeDao → rejectWithReason: cheque "
	                + cheque.getChequeNo() + " rejected. Reason=" + reasonCode);
	        return true;
	    } catch (Exception e) {
	        if (tx != null) tx.rollback();
	        System.err.println("OutwardChequeDao → rejectWithReason failed: "
	                + e.getMessage());
	        return false;
	    } finally {
	        session.close();
	    }
	}

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
    
    
    @Override
    public List<OutwardCheque> findPendingEntries(Long batchDbId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM outward_cheque "
                       + "WHERE batch_id = :batchId "
                       + "AND status = 'PENDING' "
                       + "ORDER BY seq_no ASC";
            NativeQuery<OutwardCheque> q =
                    session.createNativeQuery(sql, OutwardCheque.class);
            q.setParameter("batchId", batchDbId);
            return q.list();
        } catch (Exception e) {
            System.err.println("OutwardChequeDao → findPendingEntries failed: "
                    + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    @Override
    public int countPendingEntries(Long batchDbId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT COUNT(*) FROM outward_cheque "
                       + "WHERE batch_id = :batchId "
                       + "AND status = 'PENDING'";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("batchId", batchDbId);
            Number count = (Number) q.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            System.err.println("OutwardChequeDao → countPendingEntries failed: "
                    + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean saveAccountEntry(Long       chequeId,
                                      String     accountNo,
                                      String     accountHolder,
                                      BigDecimal amount,
                                      String     amountInWords,
                                      String     chequeDate,
                                      String     payeeName) {
        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();

            String sql = "UPDATE outward_cheque "
                       + "SET account_no      = :accountNo, "
                       + "    account_holder  = :accountHolder, "
                       + "    amount          = :amount, "
                       + "    amount_in_words = :amountInWords, "
                       + "    cheque_date     = CAST(:chequeDate AS DATE), "
                       + "    payee_name      = :payeeName, "
                       + "    status          = 'ENTRY_DONE', "
                       + "    updated_at      = NOW() "
                       + "WHERE id = :chequeId";

            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("accountNo",     accountNo);
            q.setParameter("accountHolder", accountHolder);
            q.setParameter("amount",        amount);
            q.setParameter("amountInWords", amountInWords);
            q.setParameter("chequeDate",    chequeDate);
            q.setParameter("payeeName",     payeeName);
            q.setParameter("chequeId",      chequeId);

            int rows = q.executeUpdate();
            tx.commit();

            System.out.println("OutwardChequeDao → Account entry saved. "
                    + "chequeId=" + chequeId + " status=ENTRY_DONE");
            return rows > 0;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("OutwardChequeDao → saveAccountEntry failed: "
                    + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }
}