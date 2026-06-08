package com.iispl.serviceImpl;

import com.iispl.dao.RejectRepairDao;
import com.iispl.daoImpl.RejectRepairDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.PayeeAccountService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PayeeAccountServiceImpl — Step 3 Payee Name & Account Entry.
 *
 * Reuses RejectRepairDao for DB access (same tables, same Hibernate session
 * factory) but keeps all Step-3-specific status constants and business logic
 * completely isolated from RejectRepairServiceImpl.
 */
public class PayeeAccountServiceImpl implements PayeeAccountService {

    private static final Logger LOG =
            Logger.getLogger(PayeeAccountServiceImpl.class.getName());

    // ── Step-3 status constants ────────────────────────────────────────────
    private static final String STATUS_ENTRY_DONE        = "ENTRY_DONE";
    private static final String STATUS_REFERRED_BACK     = "REFERRED_BACK";

    /** Batch status written when all cheques are processed and handed to checker. */
    private static final String BATCH_STATUS_CHECKER     = "SUBMITTED_TO_CHECKER";
    private static final String BATCH_REPAIR_STEP3_DONE  = "STEP3_COMPLETE";

    // ── DAO ────────────────────────────────────────────────────────────────
    private final RejectRepairDao dao;

    /** Default constructor — used by ZK composers (no DI container). */
    public PayeeAccountServiceImpl() {
        this.dao = new RejectRepairDaoImpl();
    }

    /** Injection constructor — for unit tests. */
    public PayeeAccountServiceImpl(RejectRepairDao dao) {
        this.dao = dao;
    }

    // ══════════════════════════════════════════════════════════════════════
    // BATCH
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public InwardBatch getBatchById(String batchId) {
        if (batchId == null || batchId.isBlank()) return null;
        try {
            return dao.findBatchById(batchId.trim());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getBatchById failed: " + batchId, e);
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHEQUES
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<InwardCheque> getChequesByBatchId(String batchId) {
        if (batchId == null || batchId.isBlank()) return new ArrayList<>();
        try {
            List<InwardCheque> list = dao.findStep3ChequesByBatchId(batchId.trim());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getChequesByBatchId (step3) failed: " + batchId, e);
            return new ArrayList<>();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SAVE / REFER
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void saveEntry(InwardCheque cheque) {
        if (cheque == null) return;
        try {
            // Guard: payeeName and accountNumber must be set by the caller
            // (composer validates before calling saveEntry).
            cheque.setRepairStatus(STATUS_ENTRY_DONE);
            dao.updateCheque(cheque);
            LOG.info("Step3 entry saved — chequeId=" + cheque.getId()
                    + " chequeNo=" + cheque.getChequeNo());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "saveEntry failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Step 3 save failed", e);
        }
    }

    @Override
    public void referBack(InwardCheque cheque, String remarks) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_REFERRED_BACK);
            if (remarks != null && !remarks.isBlank()) {
                cheque.setRemarks(remarks.trim());
            }
            dao.updateCheque(cheque);
            LOG.info("Step3 referred back — chequeId=" + cheque.getId()
                    + " chequeNo=" + cheque.getChequeNo());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "referBack failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Step 3 refer-back failed", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // WORKFLOW
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public boolean proceedToInwardChecker(String batchId) {
        if (batchId == null || batchId.isBlank()) return false;
        try {
            List<InwardCheque> cheques = getChequesByBatchId(batchId);

            // Ensure no cheque is still unprocessed (safety check).
            // Caller (composer) shows a confirmation dialog before reaching here,
            // so we only log and do NOT block — the checker can handle residuals.
            long stillPending = countPending(cheques);
            if (stillPending > 0) {
                LOG.warning("proceedToInwardChecker: " + stillPending
                        + " cheque(s) still pending in batch " + batchId
                        + " — proceeding anyway (checker will handle residuals).");
            }

            dao.updateBatchStatus(batchId, BATCH_STATUS_CHECKER, BATCH_REPAIR_STEP3_DONE);
            LOG.info("Batch " + batchId + " advanced to SUBMITTED_TO_CHECKER.");
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "proceedToInwardChecker failed, batchId=" + batchId, e);
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public long countPending(List<InwardCheque> cheques) {
        if (cheques == null) return 0;
        return cheques.stream()
                .filter(c -> !STATUS_REFERRED_BACK.equalsIgnoreCase(c.getRepairStatus()))
                .filter(c -> isBlank(c.getPayeeName())
                          || isBlank(c.getDraweeAccountNumber()))
                .count();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}