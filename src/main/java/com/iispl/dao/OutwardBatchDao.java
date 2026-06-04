//package com.iispl.dao;
//
//import com.iispl.entity.outward.OutwardBatch;
//import java.util.List;
//
///**
// * File    : com/iispl/dao/OutwardBatchDao.java
// * Purpose : Database operations for outward_batch table.
// */
//public interface OutwardBatchDao {
//
//    /**
//     * Save a new batch to the database.
//     * @return saved batch with generated ID, or null on failure
//     */
//    OutwardBatch save(OutwardBatch batch);
//
//    /**
//     * Find a batch by its batch ID string (e.g. B-2026-0603-001).
//     * @return OutwardBatch or null if not found
//     */
//    OutwardBatch findByBatchId(String batchId);
//
//    /**
//     * Find all batches created by a specific maker.
//     * Used to show the maker's own batch history.
//     */
//    List<OutwardBatch> findByCreatedBy(Long userId);
//
//    /**
//     * Update the status of a batch.
//     * e.g. UPLOADED → NEEDS_REPAIR or UPLOADED → ENTRY_DONE
//     */
//    boolean updateStatus(Long batchId, String newStatus);
//
//    /**
//     * Check if a batch ID already exists in the database.
//     * Used to prevent duplicate uploads.
//     */
//    boolean existsByBatchId(String batchId);
//
//    /**
//     * Count how many batches were created today with the given date prefix.
//     * Used to generate the next sequence number for batch ID.
//     * e.g. prefix = "B-2026-0603" → returns 2 (means next is -003)
//     */
//    int countBatchesToday(String datePrefix);
//}
package com.iispl.dao;

import com.iispl.entity.outward.OutwardBatch;
import java.util.List;

/**
 * File    : com/iispl/dao/OutwardBatchDao.java
 * Purpose : Database operations for outward_batch table.
 */
public interface OutwardBatchDao {

    /**
     * Save a new batch to the database.
     * @return saved batch with generated ID, or null on failure
     */
    OutwardBatch save(OutwardBatch batch);

    /**
     * Find a batch by its batch ID string (e.g. B-2026-0603-001).
     * @return OutwardBatch or null if not found
     */
    OutwardBatch findByBatchId(String batchId);

    /**
     * Find all batches created by a specific maker.
     * Used to show the maker's own batch history.
     */
    List<OutwardBatch> findByCreatedBy(Long userId);

    /**
     * Update the status of a batch.
     * e.g. UPLOADED → NEEDS_REPAIR or UPLOADED → ENTRY_DONE
     */
    boolean updateStatus(Long batchId, String newStatus);

    /**
     * Check if a batch ID already exists in the database.
     * Used to prevent duplicate uploads.
     */
    boolean existsByBatchId(String batchId);

    /**
     * Check if a batch with the same original file name was already uploaded
     * by the same maker and is not REJECTED.
     * Used to block duplicate file uploads.
     */
    boolean existsByFileNameAndMaker(String fileName, Long makerId);

    /**
     * Count how many batches were created today with the given date prefix.
     * Used to generate the next sequence number for batch ID.
     * e.g. prefix = "B-2026-0603" → returns 2 (means next is -003)
     */
    int countBatchesToday(String datePrefix);
}