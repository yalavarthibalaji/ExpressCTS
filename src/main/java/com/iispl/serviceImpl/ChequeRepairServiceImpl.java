package com.iispl.serviceImpl;

import com.iispl.dao.ChequeRepairDao;
import com.iispl.daoImpl.ChequeRepairDaoImpl;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.ChequeRepairService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChequeRepairServiceImpl
 *
 * Business layer for Step 2 (Date & Amount) and Step 3 (Payee & Account).
 * Delegates to ChequeRepairDao; guarantees non-null list returns; wraps exceptions.
 */
public class ChequeRepairServiceImpl implements ChequeRepairService {

    private static final Logger LOG =
            Logger.getLogger(ChequeRepairServiceImpl.class.getName());

    private final ChequeRepairDao chequeRepairDao;

    // ── Constructors ──────────────────────────────────────────────────────

    /** Default — creates its own DAO (non-Spring). */
    public ChequeRepairServiceImpl() {
        this.chequeRepairDao = new ChequeRepairDaoImpl();
    }

    /** Spring / test constructor. */
    public ChequeRepairServiceImpl(ChequeRepairDao chequeRepairDao) {
        this.chequeRepairDao = chequeRepairDao;
    }

    // ── Interface implementation ──────────────────────────────────────────

    @Override
    public List<InwardCheque> getChequesByBatchId(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            LOG.warning("getChequesByBatchId called with blank batchId");
            return new ArrayList<>();
        }
        try {
            List<InwardCheque> list = chequeRepairDao.findChequesByBatchId(batchId.trim());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching cheques for batchId=" + batchId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public InwardCheque getChequeById(Long chequeId) {
        if (chequeId == null) {
            LOG.warning("getChequeById called with null chequeId");
            return null;
        }
        try {
            return chequeRepairDao.findChequeById(chequeId);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error fetching cheque id=" + chequeId, e);
            return null;
        }
    }

    @Override
    public boolean saveDateAmountRepair(InwardCheque cheque) {
        if (cheque == null || cheque.getChequeId() == null) {
            LOG.warning("saveDateAmountRepair called with null cheque or id");
            return false;
        }
        try {
            // Mark as repaired if not already set
            if (cheque.getRepairStatus() == null) {
                cheque.setRepairStatus("REPAIRED");
            }
            return chequeRepairDao.updateDateAmount(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error saving date/amount repair for id=" + cheque.getChequeId(), e);
            return false;
        }
    }

    @Override
    public boolean savePayeeAccountRepair(InwardCheque cheque) {
        if (cheque == null || cheque.getChequeId() == null) {
            LOG.warning("savePayeeAccountRepair called with null cheque or id");
            return false;
        }
        try {
            if (cheque.getRepairStatus() == null) {
                cheque.setRepairStatus("REPAIRED");
            }
            return chequeRepairDao.updatePayeeAccount(cheque);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error saving payee/account for id=" + cheque.getChequeId(), e);
            return false;
        }
    }

    @Override
    public int getPendingRepairCount(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            return 0;
        }
        try {
            return chequeRepairDao.countPendingRepairs(batchId.trim());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error counting pending repairs for batchId=" + batchId, e);
            return 0;
        }
    }
}