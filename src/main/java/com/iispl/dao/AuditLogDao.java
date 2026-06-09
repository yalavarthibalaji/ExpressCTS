package com.iispl.dao;

import com.iispl.entity.AuditLog;

/**
 * File    : com/iispl/dao/AuditLogDao.java
 * Purpose : Insert-only DAO for the audit_log table.
 *           Audit rows are append-only — never updated, never deleted.
 *           Used by AuditService for application-level audit trail
 *           (login/logout, batch lifecycle events, exports, etc.)
 */
public interface AuditLogDao {

    /**
     * Inserts an audit row.
     * Errors are logged to System.err but never thrown —
     * audit failures must NEVER block the business operation.
     *
     * @return true if the row was saved, false if the insert failed
     */
    boolean save(AuditLog log);
}