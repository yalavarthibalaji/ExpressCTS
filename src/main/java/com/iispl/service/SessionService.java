package com.iispl.service;

import com.iispl.entity.UserSession;
import java.util.List;

/**
 * File    : com/iispl/service/SessionService.java
 * Purpose : Higher-level operations over user_sessions table.
 *           Called from login/logout flow + admin "active sessions" page.
 */
public interface SessionService {

    /**
     * Records a new active session for a logged-in user.
     *
     * @param userId    the user logging in
     * @param ipAddress client IP
     * @return the newly-generated session token (UUID), or null on failure
     */
    String startSession(Long userId, String ipAddress);

    /**
     * Closes a session by its token (sets logout_at, is_expired=true).
     */
    boolean endSession(String sessionToken);

    /**
     * Force-logs-out every active session for the user.
     * Used when the account is locked due to failed attempts.
     */
    int endAllSessionsForUser(Long userId);

    /** Returns active sessions for a user (for admin "who's online" view). */
    List<UserSession> getActiveSessions(Long userId);
}