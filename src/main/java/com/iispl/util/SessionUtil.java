package com.iispl.util;

import com.iispl.dto.LoginDTO;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;

/**
 * Helper for session checks and logout. Use from any composer.
 */
public class SessionUtil {

    public static final String SESSION_KEY = "loggedInUser";

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

    /** Invalidates session and redirects to login. */
    public static void logout() {
        Sessions.getCurrent().invalidate();
        Executions.sendRedirect("/login/login.zul");
    }
}