package com.iispl.serviceImpl;

import com.iispl.dao.AuditLogDao;
import com.iispl.daoImpl.AuditLogDaoImpl;
import com.iispl.entity.AuditLog;
import com.iispl.entity.User;
import com.iispl.service.AuditService;
import com.iispl.util.SessionUtil;

import java.time.LocalDateTime;

/**
 * File    : com/iispl/serviceImpl/AuditServiceImpl.java
 * Purpose : Implementation of AuditService.
 *
 * IMPORTANT DESIGN RULE - audit writes never throw.
 * If the audit DAO fails for any reason (DB down, table missing, etc.)
 * we log the failure to System.err and return silently. The business
 * operation that called us must NEVER be blocked by an audit problem.
 */
public class AuditServiceImpl implements AuditService {

    private final AuditLogDao auditDao = new AuditLogDaoImpl();

    @Override
    public void log(Long   userId,
                     String module,
                     String action,
                     String entityType,
                     Long   entityId,
                     String oldData,
                     String newData) {

        try {
            AuditLog row = new AuditLog();

            // userId - system events may be null (e.g. failed login w/ unknown user)
            if (userId != null) {
                User userRef = new User();
                userRef.setId(userId);
                row.setUser(userRef);
            }
            // user_id is NOT NULL on the table, so use 0 as "system" placeholder
            // if userId is null. If your schema actually allows NULL, remove this.
            if (row.getUser() == null) {
                User systemRef = new User();
                systemRef.setId(0L);
                row.setUser(systemRef);
            }

            row.setModule(module     != null ? module     : "UNKNOWN");
            row.setAction(action     != null ? action     : "UNKNOWN");
            row.setEntityType(entityType);
            row.setEntityId(entityId);
            row.setOldData(truncate(oldData, 4000));
            row.setNewData(truncate(newData, 4000));
            row.setIpAddress(SessionUtil.getCurrentIp());
            row.setCreatedAt(LocalDateTime.now());

            auditDao.save(row);

            System.out.println("AuditService -> " + module + " / " + action
                    + " by userId=" + userId
                    + (entityType != null ? " on " + entityType + "#" + entityId : ""));
        } catch (Exception e) {
            // Defensive - must not propagate
            System.err.println("AuditService -> log failed (non-critical): "
                    + e.getMessage());
        }
    }

    @Override
    public void log(Long userId, String module, String action) {
        log(userId, module, action, null, null, null, null);
    }

    /** Safely truncates strings that would exceed the column width. */
    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}