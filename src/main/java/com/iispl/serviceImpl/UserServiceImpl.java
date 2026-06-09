package com.iispl.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.iispl.dao.RoleDao;
import com.iispl.dao.UserDao;
import com.iispl.daoImpl.RoleDaoImpl;
import com.iispl.daoImpl.UserDaoImpl;
import com.iispl.dto.LoginDTO;
import com.iispl.entity.Role;
import com.iispl.entity.User;
import com.iispl.service.AuditService;
import com.iispl.service.SessionService;
import com.iispl.service.UserService;
import com.iispl.util.PasswordUtil;
import com.iispl.util.SessionUtil;

/**
 * File    : com/iispl/serviceImpl/UserServiceImpl.java
 * Purpose : Implementation of UserService.
 *
 * Phase F1 + F3-A enhancements to validateLogin():
 *   - B3: last_login_at is updated on success
 *   - B4: failed_attempts increment + auto-lock at 5
 *   - F3: audit log row written for every login event
 *   - F3: DB session row created on success; force-logout on auto-lock
 */
public class UserServiceImpl implements UserService {

    private final UserDao        userDao        = new UserDaoImpl();
    private final RoleDao        roleDao        = new RoleDaoImpl();
    private final AuditService   auditService   = new AuditServiceImpl();
    private final SessionService sessionService = new SessionServiceImpl();

    private static final int MAX_FAILED_ATTEMPTS = 5;

    // ════════════════════════════════════════════════════════════════
    //  validateLogin — F1 + F3 enhanced
    // ════════════════════════════════════════════════════════════════

    @Override
    public LoginDTO validateLogin(String userLoginId, String password) {

        // ── Input validation ──
        if (userLoginId == null || userLoginId.trim().isEmpty()) return null;
        if (password    == null || password.isEmpty())           return null;

        String loginId = userLoginId.trim();

        // Login ID can be any text (the inward team shares this users table
        // and uses text-based IDs like "admin", "maker.out", "checker.out").

        // ── Lookup user ──
        User user = userDao.findByLoginId(loginId);
        if (user == null) {
            System.err.println("UserService → login failed: no user with loginId=" + loginId);
            auditService.log(null, AuditService.M_AUTH,
                    AuditService.A_LOGIN_FAILED,
                    AuditService.E_USER, null,
                    null, "No user with loginId=" + loginId);
            return null;
        }

        // ── Account state checks ──
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            System.err.println("UserService → login blocked: user " + loginId
                    + " status=" + user.getStatus());
            auditService.log(user.getId(), AuditService.M_AUTH,
                    AuditService.A_LOGIN_FAILED,
                    AuditService.E_USER, user.getId(),
                    null, "Account status=" + user.getStatus());
            return null;
        }

        if (user.isLocked()) {
            System.err.println("UserService → login blocked: user " + loginId
                    + " is LOCKED (contact admin)");
            auditService.log(user.getId(), AuditService.M_AUTH,
                    AuditService.A_LOGIN_LOCKED,
                    AuditService.E_USER, user.getId(),
                    null, "Login attempted while LOCKED");
            return null;
        }

        // ── Password check ──
        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            // FAILED password — increment counter and maybe lock
            int newCount = userDao.incrementFailedAttempts(user.getId());
            System.err.println("UserService → password mismatch for " + loginId
                    + " (attempt " + newCount + "/" + MAX_FAILED_ATTEMPTS + ")");

            auditService.log(user.getId(), AuditService.M_AUTH,
                    AuditService.A_LOGIN_FAILED,
                    AuditService.E_USER, user.getId(),
                    "attempts=" + (newCount - 1),
                    "attempts=" + newCount);

            if (newCount >= MAX_FAILED_ATTEMPTS) {
                userDao.lockUser(user.getId());
                // Force-close any active sessions for the now-locked user
                sessionService.endAllSessionsForUser(user.getId());

                System.err.println("UserService → user " + loginId
                        + " LOCKED after " + newCount + " failed attempts");

                auditService.log(user.getId(), AuditService.M_AUTH,
                        AuditService.A_USER_LOCKED,
                        AuditService.E_USER, user.getId(),
                        null, "Auto-locked after " + newCount + " failed attempts");
            }
            return null;
        }

        // ══ SUCCESS PATH ══

        // Reset failure counter if it had been incremented
        if (user.getFailedAttempts() > 0) {
            userDao.resetFailedAttempts(user.getId());
        }
        // Update last_login_at
        userDao.updateLastLogin(user.getId());

        // Open a DB session row + remember the token in the HTTP session
        String ip    = SessionUtil.getCurrentIp();
        String token = sessionService.startSession(user.getId(), ip);
        if (token != null) {
            SessionUtil.storeSessionToken(token);
        }

        // Audit success
        auditService.log(user.getId(), AuditService.M_AUTH,
                AuditService.A_LOGIN_SUCCESS,
                AuditService.E_USER, user.getId(),
                null, "token=" + token + ", ip=" + ip);

        System.out.println("UserService → login OK: userId=" + user.getId()
                + " loginId=" + loginId);

        // ── Build the DTO returned to the caller ──
        LoginDTO dto = new LoginDTO();
        dto.setUserId(user.getId());
        dto.setUserLoginId(user.getUserLoginId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        if (user.getRole() != null) {
            dto.setRoleCode(user.getRole().getRoleCode());
        }
        return dto;
    }

    // ════════════════════════════════════════════════════════════════
    //  Unchanged methods below — kept verbatim from your original file
    // ════════════════════════════════════════════════════════════════

    @Override
    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    @Override
    public List<Role> getAllRoles() {
        return roleDao.findAll();
    }

    @Override
    public String createUser(String loginId, String fullName, String email,
                             String mobile, String plainPassword, Integer roleId) {

        // ── Validation ──
        if (loginId == null || loginId.trim().length() < 3)
            return "User ID must be at least 3 characters.";
        if (fullName == null || fullName.trim().length() < 3)
            return "Full Name must be at least 3 characters.";
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            return "Please enter a valid email address.";
        if (plainPassword == null || plainPassword.length() < 4)
            return "Password must be at least 4 characters.";
        if (roleId == null)
            return "Please select a role.";

        loginId = loginId.trim();
        email   = email.trim().toLowerCase();
        // Login ID can be any text (the inward team uses text-based IDs)
        if (userDao.existsByLoginId(loginId))
            return "User ID '" + loginId + "' is already taken.";
        if (userDao.existsByEmail(email))
            return "Email '" + email + "' is already registered.";

        Role role = roleDao.findById(roleId);
        if (role == null)
            return "Selected role does not exist.";

        // ── Build new user ──
        User u = new User();
        u.setUserLoginId(loginId);
        u.setFullName(fullName.trim());
        u.setEmail(email);
        u.setMobile(mobile != null ? mobile.trim() : null);
        u.setPasswordHash(PasswordUtil.hash(plainPassword));
        u.setRole(role);
        u.setStatus("ACTIVE");
        u.setLocked(false);
        u.setFailedAttempts(0);
        u.setCreatedAt(LocalDateTime.now());

        boolean saved = userDao.save(u);

        if (saved) {
            // F3 audit — admin actions are user-mgmt module
            auditService.log(null, AuditService.M_USER_MGMT,
                    AuditService.A_USER_CREATED,
                    AuditService.E_USER, u.getId(),
                    null, "loginId=" + loginId + ", role=" + role.getRoleCode());
        }
        return saved ? null : "Failed to save user. Please try again.";
    }

    @Override
    public boolean toggleUserStatus(Long userId) {
        List<User> all = userDao.findAll();
        for (User u : all) {
            if (u.getId().equals(userId)) {
                String oldStatus = u.getStatus();
                String newStatus = "ACTIVE".equalsIgnoreCase(oldStatus)
                        ? "INACTIVE" : "ACTIVE";
                boolean ok = userDao.updateStatus(userId, newStatus);

                if (ok) {
                    // If we're deactivating, also kick out any active sessions
                    if ("INACTIVE".equals(newStatus)) {
                        sessionService.endAllSessionsForUser(userId);
                    }
                    auditService.log(null, AuditService.M_USER_MGMT,
                            "USER_STATUS_CHANGED",
                            AuditService.E_USER, userId,
                            "status=" + oldStatus,
                            "status=" + newStatus);
                }
                return ok;
            }
        }
        return false;
    }
}