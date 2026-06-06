package com.iispl.daoImpl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import com.iispl.dao.DateAmountDao;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.util.HibernateUtil;

/**
 * DateAmountDaoImpl
 * Hibernate-based DAO for Step 2 (Date & Amount Repair) and
 * Step 3 (Payee Name & Account Entry).
 * Uses InwardCheque entity fields as defined.
 */
public class DateAmountDaoImpl implements DateAmountDao {

    private static final Logger LOG = Logger.getLogger(DateAmountDaoImpl.class.getName());

    // ──────────────────────────────────────────────────────────────────────────
    //  Step 2 + Step 3 : Find cheques by batch
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public List<InwardCheque> findChequesByBatchId(String batchId) {
        Session session = null;
        Transaction tx = null;
        List<InwardCheque> list = new ArrayList<>();
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // batch is a ManyToOne → join fetch to avoid N+1
            String hql = "FROM InwardCheque ic "
                       + "LEFT JOIN FETCH ic.batch b "
                       + "WHERE b.batchId = :batchId "
                       + "ORDER BY ic.chequeNo ASC";

            Query<InwardCheque> query = session.createQuery(hql, InwardCheque.class);
            query.setParameter("batchId", batchId);
            list = query.getResultList();

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "findChequesByBatchId failed, batchId=" + batchId, e);
            throw new RuntimeException("Hibernate error in findChequesByBatchId", e);
        } finally {
            if (session != null) session.close();
        }
        return list;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Find single cheque by PK  (Chequeid field → getChequeId())
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public InwardCheque findChequeById(Long chequeId) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            InwardCheque cheque = session.get(InwardCheque.class, chequeId);

            tx.commit();
            return cheque;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "findChequeById failed, chequeId=" + chequeId, e);
            throw new RuntimeException("Hibernate error in findChequeById", e);
        } finally {
            if (session != null) session.close();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Step 2 : Update Date & Amount
    //  Entity fields: chequeDateOcr, amountOcr, repairStatus
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public int updateDateAndAmount(InwardCheque cheque) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // cheque.getChequeId() → PK (field name: Chequeid in entity)
            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.chequeDateOcr = :rcvdDate, "
                       + "    ic.amountOcr     = :rcvdAmount, "
                       + "    ic.repairStatus  = 'DATE_AMT_REPAIRED', "
                       + "    ic.updatedAt     = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            Query<?> query = session.createQuery(hql);
            query.setParameter("rcvdDate",
                    cheque.getChequeDateOcr() != null ? cheque.getChequeDateOcr() : null);
            query.setParameter("rcvdAmount",
                    cheque.getAmountOcr() != null ? cheque.getAmountOcr() : BigDecimal.ZERO);
            query.setParameter("chequeId", cheque.getChequeId());

            int rows = query.executeUpdate();
            tx.commit();
            return rows;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "updateDateAndAmount failed, chequeId=" + cheque.getChequeId(), e);
            throw new RuntimeException("Hibernate error in updateDateAndAmount", e);
        } finally {
            if (session != null) session.close();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Step 3 : Update Payee Name & Account Number
    //  Entity fields: payeeName, draweeAccountNumber, repairStatus
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public int updatePayeeAndAccount(InwardCheque cheque) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.payeeName           = :payeeName, "
                       + "    ic.draweeAccountNumber = :accountNo, "
                       + "    ic.repairStatus        = 'ENTRY_DONE', "
                       + "    ic.updatedAt           = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            Query<?> query = session.createQuery(hql);
            query.setParameter("payeeName",  cheque.getPayeeName());
            query.setParameter("accountNo",  cheque.getDraweeAccountNumber());   // ← correct field from entity
            query.setParameter("chequeId",   cheque.getChequeId());

            int rows = query.executeUpdate();
            tx.commit();
            return rows;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "updatePayeeAndAccount failed, chequeId=" + cheque.getChequeId(), e);
            throw new RuntimeException("Hibernate error in updatePayeeAndAccount", e);
        } finally {
            if (session != null) session.close();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Submit entire batch to Checker
    //  Sets repairStatus = 'SUBMITTED_TO_CHECKER' on all cheques in batch
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public int submitBatchToChecker(String batchId) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.repairStatus = 'SUBMITTED_TO_CHECKER', "
                       + "    ic.status       = 'SUBMITTED', "
                       + "    ic.updatedAt    = CURRENT_TIMESTAMP "
                       + "WHERE ic.batch.batchId = :batchId";

            Query<?> query = session.createQuery(hql);
            query.setParameter("batchId", batchId);

            int rows = query.executeUpdate();
            tx.commit();
            return rows;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "submitBatchToChecker failed, batchId=" + batchId, e);
            throw new RuntimeException("Hibernate error in submitBatchToChecker", e);
        } finally {
            if (session != null) session.close();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Refer cheque back to a specific module (Step 2 / Step 3 refer action)
    //  Sets repairStatus = 'REFERRED_BACK', remarks updated
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public int referChequeBack(Long chequeId, String referReason, String remarks) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.repairStatus = 'REFERRED_BACK', "
                       + "    ic.remarks      = :remarks, "
                       + "    ic.updatedAt    = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            Query<?> query = session.createQuery(hql);
            query.setParameter("chequeId", chequeId);
            query.setParameter("remarks",  referReason + (remarks != null ? " | " + remarks : ""));

            int rows = query.executeUpdate();
            tx.commit();
            return rows;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "referChequeBack failed, chequeId=" + chequeId, e);
            throw new RuntimeException("Hibernate error in referChequeBack", e);
        } finally {
            if (session != null) session.close();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Reject cheque (Step 2 reject)
    //  Sets repairStatus = 'REJECTED', status = 'RETURNED'
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public int rejectCheque(Long chequeId, String rejectReason, String remarks) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.repairStatus = 'REJECTED', "
                       + "    ic.status       = 'RETURNED', "
                       + "    ic.remarks      = :remarks, "
                       + "    ic.updatedAt    = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            Query<?> query = session.createQuery(hql);
            query.setParameter("chequeId", chequeId);
            query.setParameter("remarks",  rejectReason + (remarks != null ? " | " + remarks : ""));

            int rows = query.executeUpdate();
            tx.commit();
            return rows;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "rejectCheque failed, chequeId=" + chequeId, e);
            throw new RuntimeException("Hibernate error in rejectCheque", e);
        } finally {
            if (session != null) session.close();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Find cheques by batch + repairStatus filter  (for filtered list views)
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public List<InwardCheque> findChequesByBatchAndStatus(String batchId, String repairStatus) {
        Session session = null;
        Transaction tx = null;
        List<InwardCheque> list = new ArrayList<>();
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            StringBuilder hql = new StringBuilder(
                "FROM InwardCheque ic "
              + "LEFT JOIN FETCH ic.batch b "
              + "WHERE b.batchId = :batchId "
            );

            if (repairStatus != null && !repairStatus.isEmpty()) {
                hql.append("AND ic.repairStatus = :repairStatus ");
            }
            hql.append("ORDER BY ic.chequeNo ASC");

            Query<InwardCheque> query = session.createQuery(hql.toString(), InwardCheque.class);
            query.setParameter("batchId", batchId);
            if (repairStatus != null && !repairStatus.isEmpty()) {
                query.setParameter("repairStatus", repairStatus);
            }

            list = query.getResultList();
            tx.commit();

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "findChequesByBatchAndStatus failed", e);
            throw new RuntimeException("Hibernate error in findChequesByBatchAndStatus", e);
        } finally {
            if (session != null) session.close();
        }
        return list;
    }
}