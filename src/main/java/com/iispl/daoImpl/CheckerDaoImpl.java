/*checkerDaoImpl is done by Ramana and dummy data is present here to verification*/
package com.iispl.daoImpl;

import com.iispl.dao.CheckerDao;
import com.iispl.db.HibernateUtil;
import com.iispl.dto.CheckerBatch;
import com.iispl.dto.CheckerCheque;
import com.iispl.entity.OutwardBatch;
import com.iispl.entity.OutwardCheque;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CheckerDaoImpl.java
 * Package  : com.iispl.daoImpl
 * Pattern  : DAO Implementation — reads and writes Supabase via Hibernate
 *
 * NO dummy data. Every method hits the real outward_batches
 * and outward_cheques tables in Supabase.
 *
 * Tables used:
 *   outward_batches  — read batch list, update checker_done / status
 *   outward_cheques  — read cheque list, update checker_status / remarks
 *   audit_logs       — insert one row per checker action
 */
public class CheckerDaoImpl implements CheckerDao {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    // ── 1. Get all batches in checker queue ──────────────────────────
    // Rule: maker_done = true AND checker_done = false
    @Override
    public List<CheckerBatch> getAllCheckerBatches() {
        List<CheckerBatch> result = new ArrayList<>();
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            List<OutwardBatch> rows = session.createQuery(
                "FROM OutwardBatch ob " +
                "WHERE ob.makerDone = true AND ob.checkerDone = false " +
                "ORDER BY ob.clearingDate DESC",
                OutwardBatch.class
            ).list();

            for (OutwardBatch ob : rows) {
                CheckerBatch cb = toCheckerBatch(ob);
                fillBatchCounts(session, cb, ob.getId());
                result.add(cb);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(session);
        }
        return result;
    }

    // ── 2. Get all cheques for a batch ───────────────────────────────
    @Override
    public List<CheckerCheque> getChequesByBatchId(String batchId) {
        List<CheckerCheque> result = new ArrayList<>();
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            List<OutwardCheque> rows = session.createQuery(
                "FROM OutwardCheque oc " +
                "WHERE oc.batch.batchId = :batchId " +
                "ORDER BY oc.chequeNumber ASC",
                OutwardCheque.class
            ).setParameter("batchId", batchId).list();

            for (OutwardCheque oc : rows) {
                result.add(toCheckerCheque(oc));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(session);
        }
        return result;
    }

    // ── 3. Get one cheque by cheque_id ───────────────────────────────
    @Override
    public CheckerCheque getChequeById(String chequeId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            OutwardCheque oc = session.createQuery(
                "FROM OutwardCheque oc WHERE oc.chequeId = :chequeId",
                OutwardCheque.class
            ).setParameter("chequeId", chequeId).uniqueResult();

            return oc != null ? toCheckerCheque(oc) : null;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(session);
        }
        return null;
    }

    // ── 4. Save checker spot-check decision on one cheque ────────────
    @Override
    public void updateChequeCheckerStatus(String chequeId, String status, String remarks) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            session.createMutationQuery(
                "UPDATE OutwardCheque SET " +
                "checkerStatus = :status, " +
                "checkerRemarks = :remarks, " +
                "reviewed = true, " +
                "checkerDoneAt = :now " +
                "WHERE chequeId = :chequeId"
            )
            .setParameter("status",   status)
            .setParameter("remarks",  remarks != null ? remarks : "")
            .setParameter("now",      LocalDateTime.now())
            .setParameter("chequeId", chequeId)
            .executeUpdate();

            tx.commit();

        } catch (Exception e) {
            rollback(tx);
            e.printStackTrace();
            throw new RuntimeException("Failed to update cheque status: " + e.getMessage());
        } finally {
            close(session);
        }
    }

    // ── 5. Approve entire batch ──────────────────────────────────────
    @Override
    public void approveBatch(String batchId) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            session.createMutationQuery(
                "UPDATE OutwardBatch SET " +
                "checkerDone = true, " +
                "status = 'CHECKER_APPROVED', " +
                "finalizedAt = :now " +
                "WHERE batchId = :batchId"
            )
            .setParameter("now",     LocalDateTime.now())
            .setParameter("batchId", batchId)
            .executeUpdate();

            tx.commit();

        } catch (Exception e) {
            rollback(tx);
            e.printStackTrace();
            throw new RuntimeException("Failed to approve batch: " + e.getMessage());
        } finally {
            close(session);
        }
    }

    // ── 6. Return batch to maker ─────────────────────────────────────
    @Override
    public void returnBatchToMaker(String batchId, String remarks) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Reset batch
            session.createMutationQuery(
                "UPDATE OutwardBatch SET " +
                "makerDone = false, " +
                "checkerDone = false, " +
                "status = 'RETURNED_TO_MAKER' " +
                "WHERE batchId = :batchId"
            ).setParameter("batchId", batchId).executeUpdate();

            // Reset all cheques in batch back to PENDING
            session.createMutationQuery(
                "UPDATE OutwardCheque SET " +
                "checkerStatus = 'PENDING', " +
                "checkerRemarks = :remarks, " +
                "reviewed = false " +
                "WHERE batch.batchId = :batchId"
            )
            .setParameter("remarks", "Returned to Maker: " + remarks)
            .setParameter("batchId", batchId)
            .executeUpdate();

            tx.commit();

        } catch (Exception e) {
            rollback(tx);
            e.printStackTrace();
            throw new RuntimeException("Failed to return batch: " + e.getMessage());
        } finally {
            close(session);
        }
    }

    // ── 7. MICR repair queue — cheques where iqa_status = FAIL ──────
    @Override
    public List<CheckerCheque> getMicrRepairCheques() {
        List<CheckerCheque> result = new ArrayList<>();
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            List<OutwardCheque> rows = session.createQuery(
                "FROM OutwardCheque oc " +
                "WHERE oc.iqaStatus = 'FAIL' " +
                "AND oc.checkerStatus != 'repaired' " +
                "ORDER BY oc.batch.batchId, oc.chequeNumber",
                OutwardCheque.class
            ).list();

            for (OutwardCheque oc : rows) {
                result.add(toCheckerCheque(oc));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(session);
        }
        return result;
    }

    // ── 8. Submit corrected MICR data ────────────────────────────────
    @Override
    public void submitMicrRepair(String chequeId, String micrCode, String chequeNum,
                                  String bankName, String ifscCode, String remarks) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Build dynamic HQL based on which fields are provided
            StringBuilder hql = new StringBuilder(
                "UPDATE OutwardCheque SET checkerStatus = 'repaired', checkerRemarks = :remarks "
            );
            if (notEmpty(micrCode))  hql.append(", micrCode = :micrCode ");
            if (notEmpty(chequeNum)) hql.append(", chequeNumber = :chequeNum ");
            if (notEmpty(bankName))  hql.append(", bankName = :bankName ");
            if (notEmpty(ifscCode))  hql.append(", ifscCode = :ifscCode ");
            hql.append("WHERE chequeId = :chequeId");

            var q = session.createMutationQuery(hql.toString())
                           .setParameter("remarks",  remarks)
                           .setParameter("chequeId", chequeId);

            if (notEmpty(micrCode))  q.setParameter("micrCode",  micrCode);
            if (notEmpty(chequeNum)) q.setParameter("chequeNum", chequeNum);
            if (notEmpty(bankName))  q.setParameter("bankName",  bankName);
            if (notEmpty(ifscCode))  q.setParameter("ifscCode",  ifscCode);

            q.executeUpdate();
            tx.commit();

        } catch (Exception e) {
            rollback(tx);
            e.printStackTrace();
            throw new RuntimeException("MICR repair failed: " + e.getMessage());
        } finally {
            close(session);
        }
    }

    // ── 9. Reset MICR cheque back to pending ─────────────────────────
    @Override
    public void resetMicrChequeStatus(String chequeId, String remarks) {
        updateChequeCheckerStatus(chequeId, "PENDING", remarks);
    }

    // ── 10. Report stats — counts ALL batches including approved ─────
    // getAllCheckerBatches() only returns pending batches.
    // Reports panel needs approved batches too.
    @Override
    public long[] getReportStats() {
        // [0] totalBatches, [1] approvedBatches, [2] rejectedCheques, [3] approvedAmount
        long[] stats = new long[4];
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            Long total = session.createQuery(
                "SELECT COUNT(ob) FROM OutwardBatch ob WHERE ob.makerDone = true",
                Long.class
            ).uniqueResult();
            stats[0] = total != null ? total : 0L;

            Long approved = session.createQuery(
                "SELECT COUNT(ob) FROM OutwardBatch ob WHERE ob.checkerDone = true",
                Long.class
            ).uniqueResult();
            stats[1] = approved != null ? approved : 0L;

            Long rejected = session.createQuery(
                "SELECT COUNT(oc) FROM OutwardCheque oc WHERE oc.checkerStatus = 'rejected'",
                Long.class
            ).uniqueResult();
            stats[2] = rejected != null ? rejected : 0L;

            Long amount = session.createQuery(
                "SELECT SUM(ob.totalAmount) FROM OutwardBatch ob WHERE ob.checkerDone = true",
                Long.class
            ).uniqueResult();
            stats[3] = amount != null ? amount : 0L;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(session);
        }
        return stats;
    }

    // ── 11. Insert audit log row to Supabase ─────────────────────────
    @Override
    public void insertAuditLog(String logType, String actionCode, String message,
                                String userId, String userRole,
                                String batchRef, String chequeRef) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            session.createNativeQuery(
                "INSERT INTO audit_logs " +
                "(log_type, action_code, message, user_id, user_role, " +
                "batch_ref, cheque_ref, logged_at) " +
                "VALUES (:logType, :actionCode, :message, :userId, :userRole, " +
                ":batchRef, :chequeRef, NOW())"
            )
            .setParameter("logType",    safe(logType))
            .setParameter("actionCode", safe(actionCode))
            .setParameter("message",    safe(message))
            .setParameter("userId",     safe(userId))
            .setParameter("userRole",   safe(userRole))
            .setParameter("batchRef",   batchRef)   // nullable
            .setParameter("chequeRef",  chequeRef)  // nullable
            .executeUpdate();

            tx.commit();

        } catch (Exception e) {
            // Audit failure must NOT crash the main operation
            rollback(tx);
            System.err.println("[AUDIT] Insert failed: " + e.getMessage());
        } finally {
            close(session);
        }
    }

    // ── Private: map OutwardBatch → CheckerBatch ─────────────────────
    private CheckerBatch toCheckerBatch(OutwardBatch ob) {
        CheckerBatch cb = new CheckerBatch();
        cb.setBatchId(ob.getBatchId());
        cb.setTotalCheques(ob.getTotalCheques()  != null ? ob.getTotalCheques()  : 0);
        cb.setTotalAmount(ob.getTotalAmount()    != null ? ob.getTotalAmount()   : 0L);
        cb.setStatus(ob.getStatus()             != null ? ob.getStatus()        : "CHECKER_PENDING");
        cb.setCheckerDone(Boolean.TRUE.equals(ob.getCheckerDone()));
        cb.setIqaPass(ob.getIqaPass()           != null ? ob.getIqaPass()       : 0);
        cb.setIqaFail(ob.getIqaFail()           != null ? ob.getIqaFail()       : 0);
        cb.setMicrRepairBatch(Boolean.TRUE.equals(ob.getIsMicrRepairBatch()));
        cb.setRoute(ob.getRoute()               != null ? ob.getRoute()         : "MAKER_CHECKER_DEM");
        cb.setCxfGenerated(Boolean.TRUE.equals(ob.getCxfGenerated()));
        if (ob.getScannedAt() != null) {
            cb.setScannedAt(ob.getScannedAt().format(DATE_FMT));
        }
        return cb;
    }

    // ── Private: load approved/rejected/pending counts from DB ───────
    private void fillBatchCounts(Session session, CheckerBatch cb, Long batchDbId) {
        try {
            Long approved = session.createQuery(
                "SELECT COUNT(oc) FROM OutwardCheque oc " +
                "WHERE oc.batch.id = :id AND oc.checkerStatus = 'approved'",
                Long.class
            ).setParameter("id", batchDbId).uniqueResult();

            Long rejected = session.createQuery(
                "SELECT COUNT(oc) FROM OutwardCheque oc " +
                "WHERE oc.batch.id = :id AND oc.checkerStatus = 'rejected'",
                Long.class
            ).setParameter("id", batchDbId).uniqueResult();

            int app = approved != null ? approved.intValue() : 0;
            int rej = rejected != null ? rejected.intValue() : 0;
            int pen = cb.getTotalCheques() - app - rej;

            cb.setApprovedCount(app);
            cb.setRejectedCount(rej);
            cb.setPendingCount(Math.max(0, pen));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Private: map OutwardCheque → CheckerCheque ───────────────────
    private CheckerCheque toCheckerCheque(OutwardCheque oc) {
        CheckerCheque cc = new CheckerCheque();

        // Group 1: System / XML fields
        cc.setChequeId(oc.getChequeId());
        cc.setTransactionId(oc.getTransactionId());
        cc.setChequeNumber(oc.getChequeNumber());
        cc.setBankName(safe(oc.getBankName()));
        cc.setBranchName(safe(oc.getBranchName()));
        cc.setIfscCode(safe(oc.getIfscCode()));
        cc.setMicrCode(safe(oc.getMicrCode()));
        cc.setDrawerName(safe(oc.getDrawerName()));
        cc.setDrawerAccountNumber(safe(oc.getDrawerAccountNumber()));
        cc.setChequeStatus(safe(oc.getMicrStatus()));
        cc.setIqaStatus(safe(oc.getIqaStatus()));
        cc.setBatchId(oc.getBatch() != null ? oc.getBatch().getBatchId() : "");

        // Group 2: Maker-entered fields
        cc.setAmountInFigures(oc.getAmountInFigures() != null ? oc.getAmountInFigures() : 0L);
        cc.setAmountInWords(safe(oc.getAmountInWords()));
        if (oc.getChequeDate() != null) {
            cc.setChequeDate(oc.getChequeDate().format(DATE_FMT));
        }
        if (oc.getPresentationDate() != null) {
            cc.setPresentationDate(oc.getPresentationDate().format(DATE_FMT));
        }
        cc.setPayeeName(safe(oc.getPayeeName()));
        cc.setDepositorAccount(safe(oc.getDepositorAccountNumber()));
        cc.setMakerFlag(safe(oc.getHvCategory()));
        cc.setMakerRemarks(safe(oc.getMakerRemarks()));
        cc.setMakerStatus(safe(oc.getMakerStatus()));

        // Group 3: Checker decision fields
        String cs = oc.getCheckerStatus();
        cc.setCheckerStatus(cs != null ? cs.toLowerCase() : "pending");
        cc.setCheckerRemarks(safe(oc.getCheckerRemarks()));
        cc.setReviewed(Boolean.TRUE.equals(oc.getReviewed()));

        return cc;
    }

    // ── Utility helpers ───────────────────────────────────────────────
    private String safe(String s) {
        return s != null ? s : "";
    }

    private boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private void rollback(Transaction tx) {
        if (tx != null) {
            try { tx.rollback(); } catch (Exception ignored) {}
        }
    }

    private void close(Session session) {
        if (session != null) {
            try { session.close(); } catch (Exception ignored) {}
        }
    }
}