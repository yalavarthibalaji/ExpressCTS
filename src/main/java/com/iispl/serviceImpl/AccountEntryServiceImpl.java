package com.iispl.serviceImpl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.iispl.dao.OutwardBatchDao;
import com.iispl.dao.OutwardChequeDao;
import com.iispl.daoImpl.OutwardBatchDaoImpl;
import com.iispl.daoImpl.OutwardChequeDaoImpl;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.service.AccountEntryService;
import com.iispl.service.AuditService;

/**
 * File    : com/iispl/serviceImpl/AccountEntryServiceImpl.java
 * Purpose : Implements Account and Amount Entry business logic.
 */
public class AccountEntryServiceImpl implements AccountEntryService {

    private final OutwardBatchDao  batchDao  = new OutwardBatchDaoImpl();
    private final OutwardChequeDao chequeDao = new OutwardChequeDaoImpl();
    private final AuditService auditService = new AuditServiceImpl();

    @Override
    public OutwardBatch getBatch(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) return null;
        return batchDao.findByBatchId(batchId.trim());
    }

    @Override
    public List<OutwardBatch> getEntryBatches(Long makerId) {
        if (makerId == null) return Collections.emptyList();
        List<OutwardBatch> list = batchDao.findEntryReadyByMaker(makerId);
        System.out.println("AccountEntryService → Found " + list.size()
                + " batch(es) ready for entry. makerId=" + makerId);
        return list;
    }

    @Override
    public List<OutwardCheque> getPendingCheques(Long batchDbId) {
        if (batchDbId == null) return Collections.emptyList();
        List<OutwardCheque> list = chequeDao.findPendingEntries(batchDbId);
        System.out.println("AccountEntryService → Found " + list.size()
                + " pending cheque(s) for entry. batchDbId=" + batchDbId);
        return list;
    }

    @Override
    public java.util.Map<Long, Integer> getPendingCountsForBatches(java.util.List<Long> batchDbIds) {
        if (batchDbIds == null || batchDbIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        return chequeDao.getPendingCountsForBatches(batchDbIds);
    }

    @Override
    public boolean saveEntry(Long       chequeId,
                              String     accountNo,
                              String     accountHolder,
                              BigDecimal amount,
                              String     amountInWords,
                              String     chequeDate,
                              String     payeeName,
                              Long       makerId) {

        if (chequeId == null || makerId == null) {
            System.err.println("AccountEntryService → saveEntry: "
                    + "chequeId or makerId is null");
            return false;
        }

        // BUG-3 / ENHANCEMENT-1 FIX:
        // Single atomic DAO call replaces the old two-step pattern
        // (saveAccountEntry → clearReferral) which used two transactions
        // and risked a partial write if the second call failed.
        boolean ok = chequeDao.saveAccountEntryAtomic(
            chequeId,
            accountNo     != null ? accountNo.trim()     : "",
            accountHolder != null ? accountHolder.trim() : "",
            amount,
            amountInWords != null ? amountInWords.trim() : "",
            chequeDate    != null ? chequeDate.trim()    : "",
            payeeName     != null ? payeeName.trim()     : "");

        if (ok) {
            // ENHANCEMENT-2: audit log for every entry save
            auditService.log(
                makerId,
                AuditService.M_ACCOUNT_ENTRY,
                "CHEQUE_ENTRY_SAVED",
                AuditService.E_OUTWARD_CHEQUE,
                chequeId,
                null,
                "account=" + accountNo + ", amount=" + amount);

            System.out.println("AccountEntryService → Entry saved (atomic). "
                    + "chequeId=" + chequeId);
        }
        return ok;
    }

    @Override
    public boolean rejectCheque(Long   chequeId,
                                  String reasonCode,
                                  String remarks,
                                  Long   makerId) {
        if (chequeId == null || makerId == null) return false;
        boolean ok = chequeDao.rejectWithReason(
            chequeId,
            reasonCode != null ? reasonCode.trim() : "",
            remarks    != null ? remarks.trim()    : "",
            makerId);

        if (ok) {
            chequeDao.clearReferral(chequeId, "REJECTED");

            auditService.log(
                makerId,
                AuditService.M_ACCOUNT_ENTRY,
                AuditService.A_CHEQUE_REJECTED,
                AuditService.E_OUTWARD_CHEQUE,
                chequeId,
                null,
                "reason=" + reasonCode + ", remarks=" + remarks);
        }
        return ok;
    }

    @Override
    public boolean isAllEntriesDone(Long batchDbId) {
        if (batchDbId == null) return false;
        int pending = chequeDao.countPendingEntries(batchDbId);
        System.out.println("AccountEntryService → Pending entries: "
                + pending + " for batchDbId=" + batchDbId);
        return pending == 0;
    }

    @Override
    public boolean submitBatch(Long batchDbId, Long makerId) {
        if (batchDbId == null || makerId == null) return false;

        boolean ok = batchDao.markSubmitted(batchDbId, makerId);
        if (ok) {
            System.out.println("AccountEntryService → Batch id=" + batchDbId
                    + " SUBMITTED to Checker queue by makerId=" + makerId);

            auditService.log(
                makerId,
                AuditService.M_ACCOUNT_ENTRY,
                AuditService.A_BATCH_SUBMITTED,
                AuditService.E_OUTWARD_BATCH,
                batchDbId,
                "status=ENTRY_PENDING",
                "status=SUBMITTED");
        }
        return ok;
    }
}