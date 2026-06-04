package com.iispl.dao;

import com.iispl.entity.outward.OutwardMicrRepair;

/**
 * File    : com/iispl/dao/OutwardMicrRepairDao.java
 * Purpose : Saves MICR repair audit logs to outward_micr_repair table.
 *           Every field changed during MICR Repair is recorded here.
 */
public interface OutwardMicrRepairDao {

    /**
     * Save one repair log record (one changed field = one record).
     */
    boolean save(OutwardMicrRepair repair);
}