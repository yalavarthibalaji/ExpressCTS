package com.iispl.service;

import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;
import java.util.List;

/**
 * File    : com/iispl/service/MicrRepairService.java
 * Purpose : Business logic for the MICR Repair screen.
 *
 * Workflow:
 *   1. Maker opens MICR Repair with a batchId
 *   2. Screen loads all MICR-error cheques for that batch
 *   3. For each cheque, maker corrects wrong sub-fields
 *   4. saveRepair() saves corrections + logs to audit table
 *   5. When all cheques repaired/rejected → batch moves to ENTRY_DONE
 */
public interface MicrRepairService {

    /** Load batch by batch_id string (e.g. B-2026-0603-001). */
    OutwardBatch getBatch(String batchId);

    /**
     * Returns all MICR-error cheques for a batch that still need repair.
     * Excludes already REPAIRED and REJECTED cheques.
     */
    List<OutwardCheque> getMicrErrorCheques(Long batchDbId);

    /**
     * Saves corrected MICR sub-fields for one cheque.
     *
     * What it does:
     *   1. Compares new values with original to detect changed fields
     *   2. Logs each changed field to outward_micr_repair audit table
     *   3. Updates outward_cheque with corrected fields + new micrCodeCorrected
     *   4. Sets repair_status = REPAIRED
     *   5. If all cheques in batch repaired/rejected → marks batch ENTRY_DONE
     *
     * @param chequeId       DB id of the cheque being repaired
     * @param cityCode       corrected city code (3 digits)
     * @param bankCode       corrected bank code (3 digits)
     * @param branchCode     corrected branch code (3 digits)
     * @param baseNumber     corrected base number (6 digits)
     * @param transactionCode corrected transaction code (2 digits)
     * @param remarks        optional remarks from maker
     * @param makerId        logged-in maker's user ID
     */
    boolean saveRepair(Long   chequeId,
            String chequeNo,       // ← added
            String cityCode,
            String bankCode,
            String branchCode,
            String baseNumber,
            String transactionCode,
            String remarks,
            Long   makerId);

    /**
     * Rejects a cheque during MICR Repair with a reason code.
     * Marks cheque REJECTED, decrements batch totals.
     * Checks if all remaining cheques are done → update batch if so.
     */
    boolean rejectCheque(Long   chequeId,
                          String reasonCode,
                          String remarks,
                          Long   makerId);

    /**
     * Returns true if all MICR-error cheques in the batch
     * have been repaired or rejected.
     */
    boolean isAllRepaired(Long batchDbId);

    /**
     * Marks the batch status = ENTRY_DONE once all MICR repairs are complete.
     * Called automatically by saveRepair/rejectCheque when done.
     */
    boolean markBatchEntryDone(Long batchDbId);
    
    /**
     * Returns all batches with status = NEEDS_REPAIR for this maker.
     * Used when MICR Repair is accessed from the sidebar (no batchId in URL).
     */
    List<OutwardBatch> getBatchesNeedingRepair(Long makerId);
}