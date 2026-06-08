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

/**
 * File    : com/iispl/serviceImpl/AccountEntryServiceImpl.java
 * Purpose : Implements Account and Amount Entry business logic.
 */
public class AccountEntryServiceImpl implements AccountEntryService {

    private final OutwardBatchDao  batchDao  = new OutwardBatchDaoImpl();
    private final OutwardChequeDao chequeDao = new OutwardChequeDaoImpl();

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

        boolean ok = chequeDao.saveAccountEntry(
            chequeId,
            accountNo     != null ? accountNo.trim()     : "",
            accountHolder != null ? accountHolder.trim() : "",
            amount,
            amountInWords != null ? amountInWords.trim() : "",
            chequeDate    != null ? chequeDate.trim()    : "",
            payeeName     != null ? payeeName.trim()     : "");

        if (ok) {
            // Clear referral if this cheque was sent by Checker (REFER_BACK).
            // Safe to call unconditionally — DAO is a no-op when
            // referred_to_module is already NULL (normal flow).
            chequeDao.clearReferral(chequeId, "ENTRY_DONE");

            System.out.println("AccountEntryService → Entry saved. "
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
            // Clear referral if this rejection happened on a CHECKER_REFERRED
            // cheque (REFER_BACK batch flow). No-op otherwise.
            chequeDao.clearReferral(chequeId, "REJECTED");
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
    public boolean submitBatch(Long batchDbId) {
        if (batchDbId == null) return false;
        boolean ok = batchDao.updateStatus(batchDbId, "SUBMITTED");
        if (ok) {
            System.out.println("AccountEntryService → Batch id=" + batchDbId
                    + " SUBMITTED to Checker queue.");
        }
        return ok;
    }
}