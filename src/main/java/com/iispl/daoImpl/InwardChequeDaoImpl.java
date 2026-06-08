package com.iispl.daoImpl;

import com.iispl.dao.InwardChequeDao;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * InwardChequeDaoImpl
 *
 * Single Hibernate DAO for all InwardCheque reads and writes.
 * Replaces the now-deleted RejectRepairDaoImpl and DateAmountDaoImpl.
 *
 * Session-per-method pattern: every method opens and closes its own
 * session. Write methods use an explicit Transaction with rollback on
 * failure. Read-only methods skip the transaction entirely.
 *
 * Batch-related lookups accept the numeric InwardBatch PK (Long batchId).
 * The service layer is responsible for resolving the string batchId to
 * the numeric PK before calling this DAO.
 */
public class InwardChequeDaoImpl implements InwardChequeDao {

    private static final Logger LOG =
            Logger.getLogger(InwardChequeDaoImpl.class.getName());

    // ═════════════════════════════════════════════════════════════════════
    //  Write: Persist
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void save(InwardCheque cheque) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(cheque);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save InwardCheque", e);
        }
    }

    @Override
    public void saveAll(List<InwardCheque> cheques) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            int i = 0;
            for (InwardCheque cheque : cheques) {
                session.persist(cheque);
                // Flush and clear every 50 rows to keep the first-level
                // cache from growing unbounded on large batches.
                if (++i % 50 == 0) {
                    session.flush();
                    session.clear();
                }
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save InwardCheques", e);
        }
    }

    @Override
    public void update(InwardCheque cheque) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(cheque);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to update InwardCheque", e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Write: Step 1 — MICR Repair
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Merges corrected MICR fields back to the database.
     * The caller is expected to have already set the repairStatus
     * (e.g. "REPAIRED" or "NEEDS_REPAIR") on the entity before calling.
     */
    @Override
    public void updateCheque(InwardCheque cheque) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(cheque);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "updateCheque failed, id=" + cheque.getId(), e);
            throw new RuntimeException("Failed to updateCheque", e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Write: Step 2 — Date & Amount Repair
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public int updateDateAndAmount(InwardCheque cheque) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.chequeDateOcr = :rcvdDate, "
                       + "    ic.amountOcr     = :rcvdAmount, "
                       + "    ic.repairStatus  = :repairStatus, "
                       + "    ic.remarks       = :remarks, "
                       + "    ic.updatedAt     = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            int rows = session.createQuery(hql)
                    .setParameter("rcvdDate",
                            cheque.getChequeDateOcr() != null
                                    ? cheque.getChequeDateOcr() : null)
                    .setParameter("rcvdAmount",
                            cheque.getAmountOcr() != null
                                    ? cheque.getAmountOcr() : BigDecimal.ZERO)
                    .setParameter("repairStatus",
                            cheque.getRepairStatus() != null
                                    ? cheque.getRepairStatus() : "REPAIRED")
                    .setParameter("remarks",
                            cheque.getRemarks() != null ? cheque.getRemarks() : "")
                    .setParameter("chequeId", cheque.getId())
                    .executeUpdate();

            tx.commit();
            return rows;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE,
                    "updateDateAndAmount failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Failed to updateDateAndAmount", e);
        }
    }

    @Override
    public int rejectCheque(Long chequeId, String rejectReason, String remarks) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            String combinedRemarks = rejectReason
                    + (remarks != null && !remarks.isBlank() ? " | " + remarks : "");

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.repairStatus = 'REJECTED', "
                       + "    ic.status       = 'RETURNED', "
                       + "    ic.remarks      = :remarks, "
                       + "    ic.updatedAt    = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            int rows = session.createQuery(hql)
                    .setParameter("chequeId", chequeId)
                    .setParameter("remarks",   combinedRemarks)
                    .executeUpdate();

            tx.commit();
            return rows;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "rejectCheque failed, chequeId=" + chequeId, e);
            throw new RuntimeException("Failed to rejectCheque", e);
        }
    }

    @Override
    public int referChequeBack(Long chequeId, String referReason, String remarks) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            String combinedRemarks = referReason
                    + (remarks != null && !remarks.isBlank() ? " | " + remarks : "");

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.repairStatus = 'REFERRED_BACK', "
                       + "    ic.remarks      = :remarks, "
                       + "    ic.updatedAt    = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            int rows = session.createQuery(hql)
                    .setParameter("chequeId", chequeId)
                    .setParameter("remarks",   combinedRemarks)
                    .executeUpdate();

            tx.commit();
            return rows;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "referChequeBack failed, chequeId=" + chequeId, e);
            throw new RuntimeException("Failed to referChequeBack", e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Write: Step 3 — Payee Name & Account Entry
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public int updatePayeeAndAccount(InwardCheque cheque) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.payeeName           = :payeeName, "
                       + "    ic.draweeAccountNumber = :accountNo, "
                       + "    ic.repairStatus        = 'ENTRY_DONE', "
                       + "    ic.updatedAt           = CURRENT_TIMESTAMP "
                       + "WHERE ic.Chequeid = :chequeId";

            int rows = session.createQuery(hql)
                    .setParameter("payeeName", cheque.getPayeeName())
                    .setParameter("accountNo", cheque.getDraweeAccountNumber())
                    .setParameter("chequeId",  cheque.getId())
                    .executeUpdate();

            tx.commit();
            return rows;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE,
                    "updatePayeeAndAccount failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Failed to updatePayeeAndAccount", e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Write: Batch Submission
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public int submitBatchToChecker(String batchId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            String hql = "UPDATE InwardCheque ic "
                       + "SET ic.repairStatus = 'SUBMITTED_TO_CHECKER', "
                       + "    ic.status       = 'SUBMITTED', "
                       + "    ic.updatedAt    = CURRENT_TIMESTAMP "
                       + "WHERE ic.batch.batchId = :batchId";

            int rows = session.createQuery(hql)
                    .setParameter("batchId", batchId)
                    .executeUpdate();

            tx.commit();
            return rows;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE,
                    "submitBatchToChecker failed, batchId=" + batchId, e);
            throw new RuntimeException("Failed to submitBatchToChecker", e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Read
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public InwardCheque findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(InwardCheque.class, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch InwardCheque by id", e);
        }
    }

    @Override
    public List<InwardCheque> findByBatchId(Long batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM InwardCheque c "
                          + "WHERE c.batch.id = :batchId "
                          + "ORDER BY c.seqNo ASC",
                            InwardCheque.class)
                    .setParameter("batchId", batchId)
                    .list();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch InwardCheques by batchId", e);
        }
    }

    @Override
    public List<InwardCheque> findByBatchIdAndRepairStatus(
            Long batchId, String repairStatus) {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM InwardCheque c "
                          + "WHERE c.batch.id = :batchId "
                          + "  AND UPPER(c.repairStatus) = UPPER(:repairStatus) "
                          + "ORDER BY c.seqNo ASC",
                            InwardCheque.class)
                    .setParameter("batchId",      batchId)
                    .setParameter("repairStatus", repairStatus)
                    .list();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fetch InwardCheques by batchId + repairStatus", e);
        }
    }

    @Override
    public long countPendingRepairsByBatchId(Long batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(c) FROM InwardCheque c "
                          + "WHERE c.batch.id = :batchId "
                          + "  AND (c.repairStatus IS NULL "
                          + "       OR UPPER(c.repairStatus) = 'NEEDS_REPAIR')",
                            Long.class)
                    .setParameter("batchId", batchId)
                    .uniqueResult();
            return count != null ? count : 0L;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to count pending repairs for batchId", e);
        }
    }
}