package com.iispl.daoImpl;

import com.iispl.dao.CheckerInwardReportsDao;
import com.iispl.dto.InwardReportDTO;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * File    : com/iispl/daoImpl/CheckerInwardReportsDaoImpl.java
 *
 * Uses HQL + HibernateUtil.getSessionFactory() — identical pattern to
 * CheckerInwardVerificationDaoImpl.
 *
 * Aggregates (acceptedCount, returnedCount, presentingBanks) are computed
 * in Java by iterating batch.getCheques(), exactly the same way
 * CheckerInwardVerificationComposer already does it.
 * This avoids native SQL / STRING_AGG entirely and stays in pure HQL.
 */
public class CheckerInwardReportsDaoImpl implements CheckerInwardReportsDao {

    // ─────────────────────────────────────────────────────────────────────────
    //  findReports — HQL with optional filters, returns mapped DTOs
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

            // JOIN FETCH cheques — needed to compute accepted/returned/presentingBanks
            // in Java (same pattern as CheckerInwardVerificationDaoImpl)
            StringBuilder hql = new StringBuilder(
                "SELECT DISTINCT b FROM InwardBatch b " +
                "LEFT JOIN FETCH b.cheques " +
                "WHERE b.status IN ('PENDING_CHECKER','ACCEPTED','RETURNED','REJECTED') "
            );

            // ── Optional filters ─────────────────────────────────────────────
            if (isSpecificStatus(status)) {
                hql.append("AND b.status = :status ");
            }
            if (isNotBlank(batchIdSearch)) {
                hql.append("AND LOWER(b.batchId) LIKE LOWER(:batchSearch) ");
            }
            if (fromDate != null) {
                hql.append("AND b.batchDate >= :fromDate ");
            }
            if (toDate != null) {
                hql.append("AND b.batchDate <= :toDate ");
            }

            hql.append("ORDER BY b.batchDate DESC, b.createdAt DESC");

            Query<InwardBatch> query = session.createQuery(hql.toString(), InwardBatch.class);

            if (isSpecificStatus(status))  query.setParameter("status",      status);
            if (isNotBlank(batchIdSearch)) query.setParameter("batchSearch", "%" + batchIdSearch.trim() + "%");
            if (fromDate != null)          query.setParameter("fromDate",    toLocalDate(fromDate));
            if (toDate   != null)          query.setParameter("toDate",      toLocalDate(toDate));

            // Pagination
            if (pageNo   < 1) pageNo   = 1;
            if (pageSize < 1) pageSize = 50;
            query.setFirstResult((pageNo - 1) * pageSize);
            query.setMaxResults(pageSize);

            List<InwardBatch> batches = query.getResultList();
            for (InwardBatch batch : batches) {
                results.add(toDTO(batch));
            }

        } catch (Exception e) {
            System.err.println("findReports error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }

        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  countReports — same filters, COUNT only (no FETCH needed)
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
                "WHERE b.status IN ('PENDING_CHECKER','ACCEPTED','RETURNED','REJECTED') "
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
            System.err.println("countReports error: " + e.getMessage());
            e.printStackTrace();
            return 0;
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  InwardBatch → InwardReportDTO
    //  Aggregates computed in Java from batch.getCheques() —
    //  same approach as CheckerInwardVerificationComposer
    // ─────────────────────────────────────────────────────────────────────────

    private InwardReportDTO toDTO(InwardBatch batch) {
        InwardReportDTO dto = new InwardReportDTO();

        dto.setBatchId     (batch.getBatchId());
        dto.setBatchDate   (batch.getBatchDate() != null ? batch.getBatchDate().toString() : "");
        dto.setTotalCheques(batch.getTotalCheques());
        dto.setMicrErrors  (batch.getMicrErrorCount());
        dto.setStatus      (batch.getStatus());

        List<InwardCheque> cheques = batch.getCheques();
        if (cheques == null || cheques.isEmpty()) {
            dto.setAcceptedCount (0);
            dto.setReturnedCount (0);
            dto.setRejectedCount (0);
            dto.setReferredCount (0);
            dto.setIqaFails      (0);
            dto.setPresentingBanks("—");
        } else {
            int accepted  = 0, returned = 0, rejected = 0, referred = 0, iqaFail = 0;
            Set<String> banks = new LinkedHashSet<>(); // preserves insertion order, no duplicates

            for (InwardCheque c : cheques) {
                String cs = c.getStatus();
                if ("ACCEPTED".equalsIgnoreCase(cs)) accepted++;
                else if ("RETURNED".equalsIgnoreCase(cs)) returned++;
                else if ("REJECTED".equalsIgnoreCase(cs)) rejected++;
                else if ("REFERRED".equalsIgnoreCase(cs)) referred++;

                if ("FAIL".equalsIgnoreCase(c.getIqaStatus())) iqaFail++;

                if (c.getPresentingBankName() != null && !c.getPresentingBankName().isBlank()) {
                    banks.add(c.getPresentingBankName().trim());
                }
            }

            dto.setAcceptedCount (accepted);
            dto.setReturnedCount (returned);
            dto.setRejectedCount (rejected);
            dto.setReferredCount (referred);
            dto.setIqaFails      (iqaFail);
            dto.setPassedCount   (accepted); // legacy alias
            dto.setPresentingBanks(banks.isEmpty() ? "—" : String.join(", ", banks));
        }

        return dto;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers — identical to CheckerInwardVerificationDaoImpl
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
}