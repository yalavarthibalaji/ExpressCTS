package com.iispl.serviceImpl;

import java.util.List;

import com.iispl.dao.OutwardBatchDao;
import com.iispl.dao.OutwardChequeDao;
import com.iispl.daoImpl.OutwardBatchDaoImpl;
import com.iispl.daoImpl.OutwardChequeDaoImpl;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.service.AuditService;
import com.iispl.service.MakerOutwardService;
import com.iispl.service.NotificationService;

/**
 * File    : com/iispl/serviceImpl/MakerOutwardServiceImpl.java
 */
public class MakerOutwardServiceImpl implements MakerOutwardService {

    private final OutwardChequeDao    chequeDao    = new OutwardChequeDaoImpl();
    private final OutwardBatchDao     batchDao     = new OutwardBatchDaoImpl();
    private final AuditService        auditService = new AuditServiceImpl();
    private final NotificationService notifService = new NotificationServiceImpl();

    @Override
    public int[] getReferralCounts(Long batchDbId) {
        if (batchDbId == null) return new int[]{0, 0};
        int micr = chequeDao.countReferredByModule(batchDbId, "MICR_REPAIR");
        int data = chequeDao.countReferredByModule(batchDbId, "DATA_ENTRY");
        System.out.println("MakerOutwardService → getReferralCounts: batchId="
                + batchDbId + " micr=" + micr + " data=" + data);
        return new int[]{micr, data};
    }

    @Override
    public int countActiveReferrals(Long batchDbId) {
        if (batchDbId == null) return 0;
        int total = chequeDao.countActiveReferrals(batchDbId);
        System.out.println("MakerOutwardService → countActiveReferrals: batchId="
                + batchDbId + " total=" + total);
        return total;
    }

    @Override
    public boolean resubmitBatch(Long batchDbId, Long makerId, String makerName) {
        if (batchDbId == null) {
            System.err.println("MakerOutwardService → resubmitBatch: batchDbId is null");
            return false;
        }
        if (makerId == null) {
            System.err.println("MakerOutwardService → resubmitBatch: makerId is null");
            return false;
        }

        // Verify batch belongs to this maker and is REFER_BACK
        List<OutwardBatch> all = batchDao.findByCreatedBy(makerId);
        OutwardBatch batch = null;
        for (OutwardBatch b : all) {
            if (b.getId().equals(batchDbId)) { batch = b; break; }
        }
        if (batch == null) {
            System.err.println("MakerOutwardService → resubmitBatch: "
                    + "batch not found OR does not belong to maker "
                    + makerId + ", batchDbId=" + batchDbId);
            return false;
        }
        if (!"REFER_BACK".equals(batch.getStatus())) {
            System.err.println("MakerOutwardService → resubmitBatch: "
                    + "batch is not in REFER_BACK status. current=" + batch.getStatus());
            return false;
        }

        int remaining = chequeDao.countActiveReferrals(batchDbId);
        if (remaining > 0) {
            System.err.println("MakerOutwardService → resubmitBatch: "
                    + remaining + " cheque(s) still have unresolved referrals");
            return false;
        }

        boolean ok = batchDao.markSubmitted(batchDbId, makerId);

        if (ok) {
            System.out.println("MakerOutwardService → resubmitBatch: batchId="
                    + batchDbId + " (" + batch.getBatchId() + ") "
                    + "→ SUBMITTED by maker " + makerId);

            auditService.log(
                makerId,
                AuditService.M_ACCOUNT_ENTRY,
                AuditService.A_BATCH_RESUBMITTED,
                AuditService.E_OUTWARD_BATCH,
                batchDbId,
                "status=REFER_BACK",
                "status=SUBMITTED, batchId=" + batch.getBatchId());

            // ── Notify all CHECKER_OUTWARD users that this batch is back ──
            notifService.notifyResubmitted(batch.getBatchId(), makerName);

        } else {
            System.err.println("MakerOutwardService → resubmitBatch: "
                    + "status update failed for batchId=" + batchDbId);
        }
        return ok;
    }
}