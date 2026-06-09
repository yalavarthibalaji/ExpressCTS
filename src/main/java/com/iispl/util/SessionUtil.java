package com.iispl.util;

import com.iispl.dto.LoginDTO;
import com.iispl.service.AuditService;
import com.iispl.service.SessionService;
import com.iispl.serviceImpl.AuditServiceImpl;
import com.iispl.serviceImpl.SessionServiceImpl;

import jakarta.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;

/**
 * File    : com/iispl/util/SessionUtil.java
 * Purpose : Helper for session checks, logout, IP capture, and DB-session
 *           token tracking. Use from any composer.
 *
 * Phase F3 additions:
 *   - getCurrentIp()         - pull client IP from current HTTP request
 *   - storeSessionToken()    - remember DB session token in HTTP session
 *   - getStoredSessionToken()- read it back during logout
 *   - logout()               - now closes the DB session + writes audit row
 */
public class SessionUtil {

    public static final String SESSION_KEY              = "loggedInUser";
    public static final String HTTP_ATTR_SESSION_TOKEN  = "cts.session.token";

    // Static service handles (these impls are stateless - safe to share)
    private static final AuditService   AUDIT    = new AuditServiceImpl();
    private static final SessionService SESSIONS = new SessionServiceImpl();

    // ================================================================
    //  Login state (existing behavior, unchanged)
    // ================================================================

    /** Returns logged-in user, or NULL if not logged in. */
    public static LoginDTO getCurrentUser() {
        return (LoginDTO) Sessions.getCurrent().getAttribute(SESSION_KEY);
    }

    /** If not logged in, redirect to login page. Returns the DTO or null. */
    public static LoginDTO requireLogin() {
        LoginDTO dto = getCurrentUser();
        if (dto == null) {
            Executions.sendRedirect("/login/login.zul");
            return null;
        }
        return dto;
    }

    /** Allow only ADMIN role. Redirects others to their dashboard. */
    public static LoginDTO requireAdmin() {
        LoginDTO dto = requireLogin();
        if (dto == null) return null;
        if (!"ADMIN".equals(dto.getRoleCode())) {
            Executions.sendRedirect(getDashboardUrlFor(dto.getRoleCode()));
            return null;
        }
        return dto;
    }

    /** Map role code to its dashboard URL. */
    public static String getDashboardUrlFor(String roleCode) {
        if (roleCode == null) return "/login/login.zul";
        switch (roleCode) {
            case "ADMIN":            return "/admin/adminDashboard.zul";
            case "MAKER_OUTWARD":    return "/dashboard/makerOutward/makerOutwardDashboard.zul";
            case "CHECKER_OUTWARD":  return "/dashboard/checkerOutward/checkerOutwardDashboard.zul";
            case "MAKER_INWARD":     return "/dashboard/makerInward/makerInwardDashboard.zul";
            case "CHECKER_INWARD":   return "/dashboard/checkerInward/checkerInwardDashboard.zul";
            default:                 return "/login/login.zul";
        }
    }

    // ================================================================
    //  Phase F3 - IP + DB session token helpers
    // ================================================================

    /**
     * Returns the caller's IP address from the current HTTP request.
     * Honors X-Forwarded-For (load-balancer / reverse-proxy aware).
     * Falls back to "system" when there's no HTTP context (background job).
     */
    public static String getCurrentIp() {
        try {
            Execution exec = Executions.getCurrent();
            if (exec == null) return "system";

            HttpServletRequest req =
                    (HttpServletRequest) exec.getNativeRequest();
            if (req == null) return "system";

            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.trim().isEmpty()) {
                // X-Forwarded-For may contain comma-separated chain - first is client
                return forwarded.split(",")[0].trim();
            }
            String remote = req.getRemoteAddr();
            return (remote != null && !remote.isEmpty()) ? remote : "system";
        } catch (Exception e) {
            return "system";
        }
    }

    /**
     * Stores the DB session token in HTTP session so logout() can find it
     * later. Called by UserServiceImpl.validateLogin() on successful login.
     */
    public static void storeSessionToken(String token) {
        try {
            if (token == null) return;
            Sessions.getCurrent().setAttribute(HTTP_ATTR_SESSION_TOKEN, token);
        } catch (Exception e) {
            System.err.println("SessionUtil -> storeSessionToken failed: "
                    + e.getMessage());
        }
    }

    /**
     * Reads the DB session token from HTTP session.
     * Returns null if not present (anonymous user or older login).
     */
    public static String getStoredSessionToken() {
        try {
            Object v = Sessions.getCurrent().getAttribute(HTTP_ATTR_SESSION_TOKEN);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ================================================================
    //  Logout - Phase F3 enhanced
    // ================================================================

    /**
     * Logout flow:
     *   1. Close the DB session row (logout_at + is_expired=true)
     *   2. Write a LOGOUT audit row
     *   3. Invalidate the HTTP session
     *   4. Redirect to /login/login.zul
     *
     * Audit + session-close steps are wrapped in try/catch - a failure here
     * must NEVER prevent the user from logging out.
     */
    public static void logout() {

        // Capture the current user BEFORE invalidating session
        LoginDTO dto    = getCurrentUser();
        Long     userId = dto != null ? dto.getUserId() : null;
        String   token  = getStoredSessionToken();

        // -- 1. Close DB session row --
        if (token != null) {
            try {
                SESSIONS.endSession(token);
            } catch (Exception e) {
                System.err.println("SessionUtil -> logout: endSession failed: "
                        + e.getMessage());
            }
        }

        // -- 2. Audit log --
        if (userId != null) {
            try {
                AUDIT.log(userId, AuditService.M_AUTH, AuditService.A_LOGOUT,
                          AuditService.E_USER, userId,
                          null, "token=" + token);
            } catch (Exception e) {
                System.err.println("SessionUtil -> logout: audit log failed: "
                        + e.getMessage());
            }
        }

        // -- 3. Invalidate HTTP session --
        try {
            Sessions.getCurrent().invalidate();
        } catch (Exception ignore) { }

        // -- 4. Redirect to login --
        try {
            Executions.sendRedirect("/login/login.zul");
        } catch (Exception ignore) { }	
    }
}