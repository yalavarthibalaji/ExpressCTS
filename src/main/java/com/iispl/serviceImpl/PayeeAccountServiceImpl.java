package com.iispl.serviceImpl;

import com.iispl.dao.InwardBatchDao;
import com.iispl.dao.InwardChequeDao;
import com.iispl.daoImpl.InwardBatchDaoImpl;
import com.iispl.daoImpl.InwardChequeDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.PayeeAccountService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PayeeAccountServiceImpl implements PayeeAccountService {

    private static final Logger LOG =
            Logger.getLogger(PayeeAccountServiceImpl.class.getName());

    private static final String STATUS_ENTRY_DONE    = "ENTRY_DONE";
    private static final String STATUS_REFERRED_BACK = "REFERRED_PAYEEACCOUNT";
    private static final String BATCH_STATUS_CHECKER = "MakerVerified";
    private static final String BATCH_REPAIR_STEP3_DONE = "STEP3_COMPLETE";

    private final InwardChequeDao chequeDao;
    private final InwardBatchDao  batchDao;

    public PayeeAccountServiceImpl() {
        this.chequeDao = new InwardChequeDaoImpl();
        this.batchDao  = new InwardBatchDaoImpl();
    }

    public PayeeAccountServiceImpl(InwardChequeDao chequeDao, InwardBatchDao batchDao) {
        this.chequeDao = chequeDao;
        this.batchDao  = batchDao;
    }

    @Override
    public InwardBatch getBatchById(String batchId) {
        if (batchId == null || batchId.isBlank()) return null;
        try {
            return batchDao.findByBatchId(batchId.trim());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getBatchById failed: " + batchId, e);
            return null;
        }
    }

    @Override
    public List<InwardCheque> getChequesByBatchId(String batchId) {
        if (batchId == null || batchId.isBlank()) return new ArrayList<>();
        try {
            InwardBatch batch = batchDao.findByBatchId(batchId.trim());
            if (batch == null) {
                LOG.warning("getChequesByBatchId: batch not found, batchId=" + batchId);
                return new ArrayList<>();
            }
            List<InwardCheque> list = chequeDao.findByBatchId(batch.getId());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "getChequesByBatchId (step3) failed: " + batchId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveEntry(InwardCheque cheque) {
        if (cheque == null) return;
        try {
            cheque.setRepairStatus(STATUS_ENTRY_DONE);
            chequeDao.updateCheque(cheque);
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
            if (remarks != null && !remarks.isBlank())
                cheque.setRemarks(remarks.trim());
            chequeDao.updateCheque(cheque);
            LOG.info("Step3 referred back — chequeId=" + cheque.getId()
                    + " chequeNo=" + cheque.getChequeNo());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "referBack failed, chequeId=" + cheque.getId(), e);
            throw new RuntimeException("Step 3 refer-back failed", e);
        }
    }
    
    @Override
    public boolean savePayeeAndAccount(InwardCheque cheque) {
        if (cheque == null || cheque.getId() == null) {
            LOG.warning("savePayeeAndAccount: null cheque or id");
            return false;
        }
        try {
            int rows = chequeDao.updatePayeeAndAccount(cheque);
            if (rows != 1)
                LOG.warning("savePayeeAndAccount: expected 1 row, got " + rows
                        + " for chequeId=" + cheque.getId());
            return rows == 1;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "savePayeeAndAccount failed, chequeId=" + cheque.getId(), e);
            return false;
        }
    }


    @Override
    public boolean proceedToInwardChecker(String batchId) {
        if (batchId == null || batchId.isBlank()) return false;
        try {
            // Reset any SEND_BACK cheques to RECEIVED so checker can see them again.
            // Maker has repaired them — repairStatus already reflects the work done.
            // Only the status field needs clearing from SEND_BACK.
            chequeDao.resetSendBackCheques(batchId);

            batchDao.updateBatchStatus(batchId, BATCH_STATUS_CHECKER, BATCH_REPAIR_STEP3_DONE);
            LOG.info("Batch " + batchId + " advanced to " + BATCH_STATUS_CHECKER);
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "proceedToInwardChecker failed, batchId=" + batchId, e);
            return false;
        }
    }

    @Override
    public long countPending(List<InwardCheque> cheques) {
        if (cheques == null) return 0;
        return cheques.stream()
                .filter(c -> !"ENTRY_DONE".equalsIgnoreCase(c.getRepairStatus()))
                .count();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}