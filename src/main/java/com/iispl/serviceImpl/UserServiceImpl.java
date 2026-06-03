package com.iispl.serviceImpl;

import com.iispl.dao.RoleDao;
import com.iispl.dao.UserDao;
import com.iispl.daoImpl.RoleDaoImpl;
import com.iispl.daoImpl.UserDaoImpl;
import com.iispl.dto.LoginDTO;
import com.iispl.entity.Role;
import com.iispl.entity.User;
import com.iispl.service.UserService;
import com.iispl.util.PasswordUtil;

import java.time.LocalDateTime;
import java.util.List;

public class UserServiceImpl implements UserService {

    private final UserDao userDao = new UserDaoImpl();
    private final RoleDao roleDao = new RoleDaoImpl();

    @Override
    public LoginDTO validateLogin(String userLoginId, String password) {
        if (userLoginId == null || userLoginId.trim().isEmpty())   return null;
        if (password    == null || password.isEmpty())             return null;

        User user = userDao.findByLoginId(userLoginId.trim());
        if (user == null)                                  return null;
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()))  return null;
        if (user.isLocked())                               return null;

        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            return null;
        }

        LoginDTO dto = new LoginDTO();
        dto.setUserId(user.getUserLoginId());
        dto.setUserLoginId(user.getUserLoginId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        if (user.getRole() != null) dto.setRoleCode(user.getRole().getRoleCode());
        return dto;
    }

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
        return saved ? null : "Failed to save user. Please try again.";
    }

    @Override
    public boolean toggleUserStatus(Long userId) {
        List<User> all = userDao.findAll();
        for (User u : all) {
            if (u.getId().equals(userId)) {
                String newStatus = "ACTIVE".equalsIgnoreCase(u.getStatus()) ? "INACTIVE" : "ACTIVE";
                return userDao.updateStatus(userId, newStatus);
            }
        }
        return false;
    }
}