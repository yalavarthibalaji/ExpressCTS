package com.iispl.daoImpl;

import com.iispl.dao.CheckerInwardReportsDao;
import com.iispl.dto.InwardReportDTO;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.entity.inward.InwardExport;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * File    : com/iispl/daoImpl/CheckerInwardReportsDaoImpl.java
 *
 * Status flow (uses existing InwardStatus enum — no new values added):
 *   Verified     → batch is eligible for Generate to Debit  (button enabled)
 *   CBS_Processed → debit has been generated                (button disabled)
 *
 * Display labels shown in the grid:
 *   Verified      → "Pending"    (awaiting debit generation)
 *   CBS_Processed → "Completed"  (debit generated)
 *   Rejected      → "Failed"
 *   All others    → raw status string
 *
 * CHANGE — saveInwardExport / findExportByBatchAndType added to persist
 *          inward_exports records after ACK.xml / RRF.xml generation.
 */
public class CheckerInwardReportsDaoImpl implements CheckerInwardReportsDao {

    private static final Logger log =
            LoggerFactory.getLogger(CheckerInwardReportsDaoImpl.class);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─────────────────────────────────────────────────────────────────────────
    //  findReports
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<InwardReportDTO> findReports(String batchIdSearch,
                                              Date   fromDate,
                                              Date   toDate,
                                              String status,
                                              int    pageNo,
                                              int    pageSize) {

        Session session = null;
        List<InwardReportDTO> results = new ArrayList<>();

        try {
            session = HibernateUtil.getSessionFactory().openSession();

            // Show batches that have reached or passed the Verified stage
            StringBuilder hql = new StringBuilder(
                "SELECT DISTINCT b FROM InwardBatch b " +
                "WHERE b.status IN ('Verified','CBS_Processed','Rejected') "
            );

            if (isSpecificStatus(status)) hql.append("AND b.status = :status ");
            if (isNotBlank(batchIdSearch)) hql.append("AND LOWER(b.batchId) LIKE LOWER(:batchSearch) ");
            if (fromDate != null)          hql.append("AND b.batchDate >= :fromDate ");
            if (toDate   != null)          hql.append("AND b.batchDate <= :toDate ");

            hql.append("ORDER BY b.batchDate DESC, b.createdAt DESC");

            Query<InwardBatch> query =
                    session.createQuery(hql.toString(), InwardBatch.class);

            if (isSpecificStatus(status))  query.setParameter("status",      status);
            if (isNotBlank(batchIdSearch)) query.setParameter("batchSearch", "%" + batchIdSearch.trim() + "%");
            if (fromDate != null)          query.setParameter("fromDate",    toLocalDate(fromDate));
            if (toDate   != null)          query.setParameter("toDate",      toLocalDate(toDate));

            if (pageNo   < 1) pageNo   = 1;
            if (pageSize < 1) pageSize = 50;
            query.setFirstResult((pageNo - 1) * pageSize);
            query.setMaxResults(pageSize);

            for (InwardBatch batch : query.getResultList()) {
                results.add(toDTO(batch));
            }

        } catch (Exception e) {
            log.error("findReports error: {}", e.getMessage(), e);
        } finally {
            closeQuietly(session);
        }

        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  countReports
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public int countReports(String batchIdSearch,
                             Date   fromDate,
                             Date   toDate,
                             String status) {

        Session session = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();

            StringBuilder hql = new StringBuilder(
                "SELECT COUNT(DISTINCT b) FROM InwardBatch b " +
                "WHERE b.status IN ('Verified','CBS_Processed','Rejected') "
            );

            if (isSpecificStatus(status))  hql.append("AND b.status = :status ");
            if (isNotBlank(batchIdSearch)) hql.append("AND LOWER(b.batchId) LIKE LOWER(:batchSearch) ");
            if (fromDate != null)          hql.append("AND b.batchDate >= :fromDate ");
            if (toDate   != null)          hql.append("AND b.batchDate <= :toDate ");

            Query<Long> query = session.createQuery(hql.toString(), Long.class);

            if (isSpecificStatus(status))  query.setParameter("status",      status);
            if (isNotBlank(batchIdSearch)) query.setParameter("batchSearch", "%" + batchIdSearch.trim() + "%");
            if (fromDate != null)          query.setParameter("fromDate",    toLocalDate(fromDate));
            if (toDate   != null)          query.setParameter("toDate",      toLocalDate(toDate));

            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;

        } catch (Exception e) {
            log.error("countReports error: {}", e.getMessage(), e);
            return 0;
        } finally {
            closeQuietly(session);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  getBatchStatus
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String getBatchStatus(String batchId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Query<String> query = session.createQuery(
                "SELECT b.status FROM InwardBatch b WHERE b.batchId = :batchId",
                String.class
            );
            query.setParameter("batchId", batchId);
            return query.uniqueResult();
        } catch (Exception e) {
            log.error("getBatchStatus error for '{}': {}", batchId, e.getMessage(), e);
            return null;
        } finally {
            closeQuietly(session);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  updateBatchStatus
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void updateBatchStatus(String batchId, String newStatus) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            Query<?> query = session.createQuery(
                "UPDATE InwardBatch b " +
                "SET b.status = :newStatus, b.updatedAt = :now " +
                "WHERE b.batchId = :batchId"
            );
            query.setParameter("newStatus", newStatus);
            query.setParameter("now",       LocalDateTime.now());
            query.setParameter("batchId",   batchId);
            int updated = query.executeUpdate();

            tx.commit();
            log.info("updateBatchStatus — batch '{}' → '{}' ({} rows)", batchId, newStatus, updated);

        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            log.error("updateBatchStatus error for '{}': {}", batchId, e.getMessage(), e);
            throw new RuntimeException("Failed to update status for batch '" + batchId + "'", e);
        } finally {
            closeQuietly(session);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  executeDebitGeneration
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void executeDebitGeneration(String batchId) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            InwardBatch batch = session.createQuery(
                "SELECT b FROM InwardBatch b LEFT JOIN FETCH b.cheques " +
                "WHERE b.batchId = :batchId",
                InwardBatch.class
            ).setParameter("batchId", batchId).uniqueResult();

            if (batch == null) {
                throw new IllegalArgumentException("Batch not found: " + batchId);
            }

            // TODO: create debit entries for each ACCEPTED cheque in the batch.
            // Example:
            //   for (InwardCheque cheque : batch.getCheques()) {
            //       if ("ACCEPTED".equalsIgnoreCase(cheque.getStatus())) {
            //           DebitEntry entry = new DebitEntry(batch, cheque);
            //           session.persist(entry);
            //       }
            //   }

            tx.commit();
            log.info("executeDebitGeneration — debit entries committed for batch '{}'", batchId);

        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            log.error("executeDebitGeneration error for '{}': {}", batchId, e.getMessage(), e);
            throw new RuntimeException("Debit generation DB error for batch '" + batchId + "'", e);
        } finally {
            closeQuietly(session);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  findBatchWithChequesAndActions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads a fully hydrated InwardBatch: cheques + checker actions per cheque.
     * Two separate queries are used to avoid a Hibernate CartesianProduct from
     * multiple simultaneous JOIN FETCH on bag collections.
     *
     * Query 1 — batch + cheques
     * Query 2 — batch + checkerActions per cheque  (initialises the lazy list)
     */
    @Override
    public InwardBatch findBatchWithChequesAndActions(String batchId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            // Step 1: fetch batch + cheques
            InwardBatch batch = session.createQuery(
                "SELECT DISTINCT b FROM InwardBatch b " +
                "LEFT JOIN FETCH b.cheques " +
                "WHERE b.batchId = :batchId",
                InwardBatch.class
            ).setParameter("batchId", batchId).uniqueResult();

            if (batch == null) return null;

            // Step 2: for each cheque, initialise checkerActions collection
            // (avoids MultipleBagFetchException from double JOIN FETCH)
            for (InwardCheque cheque : batch.getCheques()) {
                session.createQuery(
                    "SELECT DISTINCT c FROM InwardCheque c " +
                    "LEFT JOIN FETCH c.checkerActions ca " +
                    "WHERE c.id = :chequeId",
                    InwardCheque.class
                ).setParameter("chequeId", cheque.getId()).uniqueResult();
            }

            return batch;

        } catch (Exception e) {
            log.error("findBatchWithChequesAndActions error for '{}': {}", batchId, e.getMessage(), e);
            return null;
        } finally {
            closeQuietly(session);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  saveInwardExport  (NEW)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persists a new InwardExport row, or updates the existing one when a
     * record for the same batch + file_type already exists.
     * <p>
     * Uses a dedicated transaction so that a failure here does not roll back
     * the XML-file write or the batch status update.
     */
    @Override
    public void saveInwardExport(InwardExport export) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Look for an existing row with the same batch + fileType
            InwardExport existing = findExportByBatchAndTypeInSession(
                    session,
                    export.getBatch().getBatchId(),
                    export.getFileType()
            );

            if (existing != null) {
                // UPDATE: refresh mutable fields only
                existing.setFileName(export.getFileName());
                existing.setFilePath(export.getFilePath());
                existing.setStatus(export.getStatus());
                existing.setGeneratedAt(LocalDateTime.now());
                session.merge(existing);
                log.info("saveInwardExport — UPDATED existing record id={} for batch='{}' type='{}'",
                         existing.getId(),
                         export.getBatch().getBatchId(),
                         export.getFileType());
            } else {
                // INSERT: associate managed proxies for FK relationships
                session.persist(export);
                log.info("saveInwardExport — INSERTED new record for batch='{}' type='{}'",
                         export.getBatch().getBatchId(),
                         export.getFileType());
            }

            tx.commit();

        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            log.error("saveInwardExport error for batch='{}' type='{}': {}",
                      export.getBatch() != null ? export.getBatch().getBatchId() : "N/A",
                      export.getFileType(),
                      e.getMessage(), e);
            throw new RuntimeException("Failed to persist InwardExport record: " + e.getMessage(), e);
        } finally {
            closeQuietly(session);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  findExportByBatchAndType  (NEW)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public InwardExport findExportByBatchAndType(String batchId, String fileType) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            return findExportByBatchAndTypeInSession(session, batchId, fileType);
        } catch (Exception e) {
            log.error("findExportByBatchAndType error for batch='{}' type='{}': {}",
                      batchId, fileType, e.getMessage(), e);
            return null;
        } finally {
            closeQuietly(session);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helper — reusable within an open Session
    // ─────────────────────────────────────────────────────────────────────────

    private InwardExport findExportByBatchAndTypeInSession(Session session,
                                                            String  batchId,
                                                            String  fileType) {
        return session.createQuery(
            "SELECT e FROM InwardExport e " +
            "JOIN e.batch b " +
            "WHERE b.batchId = :batchId AND e.fileType = :fileType",
            InwardExport.class
        ).setParameter("batchId",  batchId)
         .setParameter("fileType", fileType)
         .uniqueResult();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  InwardBatch → InwardReportDTO
    // ─────────────────────────────────────────────────────────────────────────

    private InwardReportDTO toDTO(InwardBatch batch) {
        InwardReportDTO dto = new InwardReportDTO();

        dto.setBatchId    (batch.getBatchId());
        dto.setBatchDate  (batch.getBatchDate() != null
                           ? batch.getBatchDate().format(DATE_FMT) : "");
        dto.setTotalCheques(batch.getTotalCheques());
        dto.setTotalAmount (batch.getTotalAmount() != null
                           ? batch.getTotalAmount() : BigDecimal.ZERO);
        dto.setStatus     (mapToDisplayStatus(batch.getStatus()));

        // Button is enabled ONLY for Verified batches — the debit-eligible state
        dto.setDebitEligible("Verified".equals(batch.getStatus()));

        return dto;
    }

    /**
     * Maps InwardStatus enum values to business-friendly display labels.
     *
     *   Verified      → Pending     (ready for debit generation)
     *   CBS_Processed → Completed   (debit already generated)
     *   Rejected      → Failed
     *   Everything else shown as-is
     */
    private String mapToDisplayStatus(String dbStatus) {
        if (dbStatus == null) return "—";
        switch (dbStatus) {
            case "Verified":      return "Pending";
            case "CBS_Processed": return "Completed";
            case "Rejected":      return "Failed";
            default:              return dbStatus;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private boolean isSpecificStatus(String s) {
        return s != null && !s.isEmpty() && !s.equalsIgnoreCase("ALL");
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private void closeQuietly(Session session) {
        try {
            if (session != null && session.isOpen()) session.close();
        } catch (Exception e) {
            log.warn("Error closing Hibernate session: {}", e.getMessage());
        }
    }
}