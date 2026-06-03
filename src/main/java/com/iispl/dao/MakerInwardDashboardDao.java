package com.iispl.dao;


import java.util.List;

import com.iispl.entity.inward.InwardBatch;

/**
 * MakerInwardDashboardDao
 *
 * DAO interface for Maker Inward Dashboard data access.
 */
public interface MakerInwardDashboardDao {

    /**
     * Fetch all inward batches from the database.
     *
     * @return list of InwardBatch; may be null (ServiceImpl handles null-safety)
     */
    List<InwardBatch> findAllInwardBatches();

    /**
     * Fetch inward batches filtered by a given status.
     *
     * @param status  upper-cased status string e.g. "PENDING"
     * @return filtered list; may be null
     */
    List<InwardBatch> findInwardBatchesByStatus(String status);
}