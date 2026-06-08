package com.iispl.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iispl.dao.DateAmountDao;
import com.iispl.daoImpl.DateAmountDaoImpl;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.DateAmountService;

/**
 * DateAmountServiceImpl
 *
 * Business layer for:
 *   - Step 2 : Date & Amount Repair (accept / reject / refer)
 *   - Step 3 : Payee Name & Account Entry
 *   - Batch submission to Checker
 *
 * Guarantees:
 *   - Non-null list returns (never returns null — always empty list on error)
 *   - Boolean returns indicate single-row success (rows == 1)
 *   - All exceptions are caught, logged, and translated to safe return values
 *     so the composer never sees an uncaught RuntimeException from the service.
 */
public class DateAmountServiceImpl implements DateAmountService {

    private static final Logger LOG =
            Logger.getLogger(DateAmountServiceImpl.class.getName());

    private final DateAmountDao dao;

    // ── Constructors ──────────────────────────────────────────────────────

    /** Default constructor — creates its own DAO (non-Spring). */
    public DateAmountServiceImpl() {
        this.dao = new DateAmountDaoImpl();
    }

    /** Injectable constructor for Spring / testing. */
    public DateAmountServiceImpl(DateAmountDao dao) {
        this.dao = dao;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  STEP 2 & 3 — QUERIES
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public List<InwardCheque> getChequesByBatchId(String batchId) {
        if (isBlank(batchId)) {
            LOG.warning("getChequesByBatchId: blank batchId");
            return new ArrayList<>();
        }
        try {
            List<InwardCheque> list = dao.findChequesByBatchId(batchId.trim());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getChequesByBatchId failed for batch: " + batchId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public InwardCheque getChequeById(String chequeId) {
        if (isBlank(chequeId)) {
            LOG.warning("getChequeById: blank chequeId");
            return null;
        }
        try {
            long id = Long.parseLong(chequeId.trim());
            return dao.findChequeById(id);
        } catch (NumberFormatException e) {
            LOG.log(Level.WARNING, "getChequeById: non-numeric id=" + chequeId, e);
            return null;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getChequeById failed for id=" + chequeId, e);
            return null;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  STEP 2 — DATE & AMOUNT REPAIR
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Accept repair — persists corrected OCR date and amount.
     * The caller (composer) sets repairStatus on the entity before calling.
     */
    @Override
    public boolean saveRepairDateAndAmount(InwardCheque cheque) {
        if (cheque == null || cheque.getId() == null) {
            LOG.warning("saveRepairDateAndAmount: null cheque or id");
            return false;
        }
        try {
            int rows = dao.updateDateAndAmount(cheque);
            if (rows != 1)
                LOG.warning("saveRepairDateAndAmount: expected 1 row, got " + rows
                        + " for chequeId=" + cheque.getId());
            return rows == 1;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "saveRepairDateAndAmount failed for chequeId=" + cheque.getId(), e);
            return false;
        }
    }

    /**
     * Reject — delegates to DAO with reason + remarks.
     * DAO sets repairStatus = 'REJECTED', status = 'RETURNED'.
     */
    @Override
    public boolean rejectCheque(Long chequeId, String rejectReason, String remarks) {
        if (chequeId == null) {
            LOG.warning("rejectCheque: null chequeId");
            return false;
        }
        if (isBlank(rejectReason)) {
            LOG.warning("rejectCheque: blank rejectReason for chequeId=" + chequeId);
            return false;
        }
        try {
            int rows = dao.rejectCheque(chequeId, rejectReason.trim(),
                    remarks != null ? remarks.trim() : "");
            return rows == 1;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "rejectCheque failed for chequeId=" + chequeId, e);
            return false;
        }
    }

    /**
     * Refer back — delegates to DAO with reason + remarks.
     * DAO sets repairStatus = 'REFERRED_BACK'.
     */
    @Override
    public boolean referChequeBack(Long chequeId, String referReason, String remarks) {
        if (chequeId == null) {
            LOG.warning("referChequeBack: null chequeId");
            return false;
        }
        try {
            int rows = dao.referChequeBack(chequeId,
                    referReason != null ? referReason.trim() : "REFER",
                    remarks    != null ? remarks.trim()    : "");
            return rows == 1;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "referChequeBack failed for chequeId=" + chequeId, e);
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  STEP 3 — PAYEE NAME & ACCOUNT ENTRY
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public boolean savePayeeAndAccount(InwardCheque cheque) {
        if (cheque == null || cheque.getId() == null) {
            LOG.warning("savePayeeAndAccount: null cheque or id");
            return false;
        }
        try {
            int rows = dao.updatePayeeAndAccount(cheque);
            if (rows != 1)
                LOG.warning("savePayeeAndAccount: expected 1 row, got " + rows
                        + " for chequeId=" + cheque.getId());
            return rows == 1;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "savePayeeAndAccount failed for chequeId=" + cheque.getId(), e);
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  BATCH SUBMISSION
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public int submitBatchToChecker(String batchId) {
        if (isBlank(batchId)) {
            LOG.warning("submitBatchToChecker: blank batchId");
            return 0;
        }
        try {
            int rows = dao.submitBatchToChecker(batchId.trim());
            LOG.info("submitBatchToChecker: " + rows + " cheques submitted for batch=" + batchId);
            return rows;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "submitBatchToChecker failed for batchId=" + batchId, e);
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}