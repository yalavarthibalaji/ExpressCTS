package com.iispl.service;


import java.util.List;

import com.iispl.entity.inward.InwardBatch;

/**
 * MakerInwardDashboardService
 *
 * Service interface for the Maker Inward Dashboard.
 * Defines the contract for fetching inward batch data.
 */
public interface MakerInwardDashboardService {

    /**
     * Returns all inward batches visible to the Maker role.
     *
     * @return list of InwardBatch; never null (empty list if none exist)
     */
    List<InwardBatch> getInwardBatches();

    /**
     * Returns inward batches filtered by status.
     *
     * @param status  e.g. "PENDING", "PROCESSED", "REJECTED"
     * @return filtered list; never null
     */
    List<InwardBatch> getInwardBatchesByStatus(String status);
}