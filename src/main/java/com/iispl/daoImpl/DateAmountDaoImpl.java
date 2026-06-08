package com.iispl.daoImpl;

import java.math.BigDecimal;
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
 *
 * Hibernate-based DAO for:
 *   - Step 2 : Date & Amount Repair (updateDateAndAmount, rejectCheque, referChequeBack)
 *   - Step 3 : Payee Name & Account Entry (updatePayeeAndAccount)
 *   - Batch submission (submitBatchToChecker)
 *
 * All sessions are opened and closed per method (no long-lived sessions).
 * Each write operation uses an explicit Transaction with rollback on failure.
 */
public class DateAmountDaoImpl implements DateAmountDao {

    private static final Logger LOG =
            Logger.getLogger(DateAmountDaoImpl.class.getName());

    // ─────────────────────────────────────────────────────────────────────
    //  READ — findChequesByBatchId
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public List<InwardCheque> findChequesByBatchId(String batchId) {
        Session session = null;
        Transaction tx  = null;
        List<InwardCheque> list = new ArrayList<>();
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // LEFT JOIN FETCH avoids N+1 on batch association
            String hql = "FROM InwardCheque ic "
                       + "LEFT JOIN FETCH ic.batch b "
                       + "WHERE b.batchId = :batchId "
                       + "ORDER BY ic.chequeNo ASC";

            Query<InwardCheque> q = session.createQuery(hql, InwardCheque.class);
            q.setParameter("batchId", batchId);
            list = q.getResultList();
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

    // ─────────────────────────────────────────────────────────────────────
    //  READ — findChequeById
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public InwardCheque findChequeById(Long chequeId) {
        Session session = null;
        Transaction tx  = null;
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

    // ─────────────────────────────────────────────────────────────────────
    //  READ — findChequesByBatchAndStatus
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public List<InwardCheque> findChequesByBatchAndStatus(String batchId, String repairStatus) {
        Session session = null;
        Transaction tx  = null;
        List<InwardCheque> list = new ArrayList<>();
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            StringBuilder hql = new StringBuilder(
                "FROM InwardCheque ic "
              + "LEFT JOIN FETCH ic.batch b "
              + "WHERE b.batchId = :batchId "
            );
            if (repairStatus != null && !repairStatus.isEmpty())
                hql.append("AND ic.repairStatus = :repairStatus ");
            hql.append("ORDER BY ic.chequeNo ASC");

            Query<InwardCheque> q = session.createQuery(hql.toString(), InwardCheque.class);
            q.setParameter("batchId", batchId);
            if (repairStatus != null && !repairStatus.isEmpty())
                q.setParameter("repairStatus", repairStatus);

            list = q.getResultList();
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

    // ─────────────────────────────────────────────────────────────────────
    //  WRITE — Step 2: updateDateAndAmount
    //  Fields: chequeDateOcr, amountOcr, repairStatus → 'DATE_AMT_REPAIRED'
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public int updateDateAndAmount(InwardCheque cheque) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.chequeDateOcr = :rcvdDate, "
                       + "    ic.amountOcr     = :rcvdAmount, "
                       + "    ic.repairStatus  = :repairStatus, "
                       + "    ic.remarks       = :remarks, "
                       + "    ic.updatedAt     = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            Query<?> q = session.createQuery(hql);
            q.setParameter("rcvdDate",
                    cheque.getChequeDateOcr() != null ? cheque.getChequeDateOcr() : null);
            q.setParameter("rcvdAmount",
                    cheque.getAmountOcr() != null ? cheque.getAmountOcr() : BigDecimal.ZERO);
            q.setParameter("repairStatus",
                    cheque.getRepairStatus() != null ? cheque.getRepairStatus() : "REPAIRED");
            q.setParameter("remarks",
                    cheque.getRemarks() != null ? cheque.getRemarks() : "");
            q.setParameter("chequeId", cheque.getId());

            int rows = q.executeUpdate();
            tx.commit();
            return rows;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE,
                    "updateDateAndAmount failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Hibernate error in updateDateAndAmount", e);
        } finally {
            if (session != null) session.close();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  WRITE — Step 2: rejectCheque
    //  Sets repairStatus = 'REJECTED', status = 'RETURNED', remarks
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public int rejectCheque(Long chequeId, String rejectReason, String remarks) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            String combinedRemarks = rejectReason
                    + (remarks != null && !remarks.isBlank() ? " | " + remarks : "");

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.repairStatus = 'REJECTED', "
                       + "    ic.status       = 'RETURNED', "
                       + "    ic.remarks      = :remarks, "
                       + "    ic.updatedAt    = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            Query<?> q = session.createQuery(hql);
            q.setParameter("chequeId", chequeId);
            q.setParameter("remarks",  combinedRemarks);

            int rows = q.executeUpdate();
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

    // ─────────────────────────────────────────────────────────────────────
    //  WRITE — Step 2: referChequeBack
    //  Sets repairStatus = 'REFERRED_BACK', remarks
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public int referChequeBack(Long chequeId, String referReason, String remarks) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            String combinedRemarks = referReason
                    + (remarks != null && !remarks.isBlank() ? " | " + remarks : "");

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.repairStatus = 'REFERRED_BACK', "
                       + "    ic.remarks      = :remarks, "
                       + "    ic.updatedAt    = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            Query<?> q = session.createQuery(hql);
            q.setParameter("chequeId", chequeId);
            q.setParameter("remarks",  combinedRemarks);

            int rows = q.executeUpdate();
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

    // ─────────────────────────────────────────────────────────────────────
    //  WRITE — Step 3: updatePayeeAndAccount
    //  Fields: payeeName, draweeAccountNumber, repairStatus → 'ENTRY_DONE'
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public int updatePayeeAndAccount(InwardCheque cheque) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.payeeName           = :payeeName, "
                       + "    ic.draweeAccountNumber = :accountNo, "
                       + "    ic.repairStatus        = 'ENTRY_DONE', "
                       + "    ic.updatedAt           = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            Query<?> q = session.createQuery(hql);
            q.setParameter("payeeName",  cheque.getPayeeName());
            q.setParameter("accountNo",  cheque.getDraweeAccountNumber());
            q.setParameter("chequeId",   cheque.getId());

            int rows = q.executeUpdate();
            tx.commit();
            return rows;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE,
                    "updatePayeeAndAccount failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Hibernate error in updatePayeeAndAccount", e);
        } finally {
            if (session != null) session.close();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  WRITE — submitBatchToChecker
    //  Sets repairStatus = 'SUBMITTED_TO_CHECKER', status = 'SUBMITTED'
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public int submitBatchToChecker(String batchId) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.repairStatus = 'SUBMITTED_TO_CHECKER', "
                       + "    ic.status       = 'SUBMITTED', "
                       + "    ic.updatedAt    = CURRENT_TIMESTAMP "
                       + "WHERE ic.batch.batchId = :batchId";

            Query<?> q = session.createQuery(hql);
            q.setParameter("batchId", batchId);

            int rows = q.executeUpdate();
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
}