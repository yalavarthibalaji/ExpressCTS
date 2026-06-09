package com.iispl.dao;

import com.iispl.entity.UserSession;

import java.util.List;

/**
 * File    : com/iispl/dao/UserSessionDao.java
 * Purpose : CRUD for user_sessions table.
 *           Tracks DB-level session records (separate from HTTP session)
 *           so admin can see concurrent logins, last activity, etc.
 */
public interface UserSessionDao {

    /** Inserts a new active session row. Returns the entity with id populated. */
    UserSession save(UserSession session);

    /**
     * Marks a session as expired (logout_at=NOW(), is_expired=true).
     * @return true if the session was found and updated
     */
    boolean expireByToken(String sessionToken);

    /**
     * Expires all active sessions for a user.
     * Used when admin force-logs-out a user, or when failing-attempts lock.
     */
    int expireAllForUser(Long userId);

    /** Returns currently-active sessions for a user (is_expired=false). */
    List<UserSession> findActiveByUser(Long userId);
}