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
 * DateAmountRepairServiceImpl
 *
 * Business layer for Step 2 (Date & Amount Repair) and
 * Step 3 (Payee Name & Account Entry).
 *
 * Delegates to DAO, guarantees non-null returns, handles exceptions.
 */
public class DateAmountServiceImpl implements DateAmountService {

    private static final Logger LOG =
            Logger.getLogger(DateAmountServiceImpl.class.getName());

    private final DateAmountDao dao;

    // ── Constructors ──────────────────────────────────────────────────────

    /** Default — creates its own DAO (non-Spring). */
    public DateAmountServiceImpl() {
        this.dao = new DateAmountDaoImpl();
    }

    /** Spring / test constructor. */
    public DateAmountServiceImpl(DateAmountDao dao) {
        this.dao = dao;
    }

    // ── Interface implementation ──────────────────────────────────────────

    @Override
    public List<InwardCheque> getChequesByBatchId(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            LOG.warning("getChequesByBatchId called with blank batchId");
            return new ArrayList<>();
        }
        try {
            List<InwardCheque> list = dao.findChequesByBatchId(batchId.trim());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching cheques for batch: " + batchId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public InwardCheque getChequeById(String chequeId) {
        if (chequeId == null || chequeId.trim().isEmpty()) {
            LOG.warning("getChequeById called with blank chequeId");
            return null;
        }
        try {
            Long id = Long.parseLong(chequeId.trim());
            return dao.findChequeById(id);
        } catch (NumberFormatException e) {
            LOG.log(Level.WARNING, "getChequeById: invalid numeric id: " + chequeId, e);
            return null;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching cheque by id: " + chequeId, e);
            return null;
        }
    }

    @Override
    public boolean saveRepairDateAndAmount(InwardCheque cheque) {
        if (cheque == null || cheque.getId() == null) {
            LOG.warning("saveRepairDateAndAmount: null cheque or chequeId");
            return false;
        }
        try {
            int rows = dao.updateDateAndAmount(cheque);
            return rows == 1;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error saving date/amount for chequeId: " + cheque.getId(), e);
            return false;
        }
    }

    @Override
    public boolean savePayeeAndAccount(InwardCheque cheque) {
        if (cheque == null || cheque.getId() == null) {
            LOG.warning("savePayeeAndAccount: null cheque or chequeId");
            return false;
        }
        try {
            int rows = dao.updatePayeeAndAccount(cheque);
            return rows == 1;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error saving payee/account for chequeId: " + cheque.getId(), e);
            return false;
        }
    }

    @Override
    public int submitBatchToChecker(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            LOG.warning("submitBatchToChecker called with blank batchId");
            return 0;
        }
        try {
            return dao.submitBatchToChecker(batchId.trim());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error submitting batch to checker: " + batchId, e);
            return 0;
        }
    }
}