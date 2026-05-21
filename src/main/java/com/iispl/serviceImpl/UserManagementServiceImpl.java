package com.iispl.serviceImpl;

import at.favre.lib.crypto.bcrypt.BCrypt;

import com.iispl.dao.UserManagementDao;
import com.iispl.daoImpl.UserManagementDaoImpl;
import com.iispl.entity.SystemRole;
import com.iispl.entity.SystemUser;
import com.iispl.entity.UserRole;
import com.iispl.service.UserManagementService;

import java.time.LocalDateTime;
import java.util.List;

public class UserManagementServiceImpl implements UserManagementService {

    private UserManagementDao userMgmtDao = new UserManagementDaoImpl();

    // default password for all new users
    private static final String DEFAULT_PASSWORD = "Test@123";

    @Override
    public void addUser(String userId, String fullName, String branchCode,
                        String initial, String roleCode, String createdBy) {

        // check if userId already exists
        if (userMgmtDao.userIdExists(userId)) {
            throw new RuntimeException("User ID already exists: " + userId);
        }

        // find the role
        SystemRole role = userMgmtDao.findRoleByCode(roleCode);
        if (role == null) {
            throw new RuntimeException("Role not found: " + roleCode);
        }

        // hash the default password
        String hashedPassword = BCrypt.withDefaults()
                .hashToString(12, DEFAULT_PASSWORD.toCharArray());

        // build SystemUser entity
        SystemUser user = new SystemUser();
        user.setUserId(userId);
        user.setPasswordHash(hashedPassword);
        user.setFullName(fullName);
        user.setBranchCode(branchCode);
        user.setInitial(initial);
        user.setActive(true);
        user.setCreatedBy(createdBy);
        user.setCreatedAt(LocalDateTime.now());

        // save user
        userMgmtDao.saveUser(user);

        // fetch saved user to get DB generated id
        SystemUser savedUser = userMgmtDao.findByUserId(userId);

        // assign role to user
        UserRole userRole = new UserRole();
        userRole.setSystemUser(savedUser);
        userRole.setSystemRole(role);
        userRole.setAssignedBy(createdBy);
        userRole.setAssignedAt(LocalDateTime.now());
        userMgmtDao.saveUserRole(userRole);

        System.out.println("New user created: " + userId
            + " with role: " + roleCode
            + " default password: " + DEFAULT_PASSWORD);
    }

    @Override
    public void enableUser(String userId, String updatedBy) {
        SystemUser user = getExistingUser(userId);
        user.setActive(true);
        userMgmtDao.updateUser(user);
        System.out.println("User enabled: " + userId + " by: " + updatedBy);
    }

    @Override
    public void disableUser(String userId, String updatedBy) {
        SystemUser user = getExistingUser(userId);
        user.setActive(false);
        userMgmtDao.updateUser(user);
        System.out.println("User disabled: " + userId + " by: " + updatedBy);
    }

    @Override
    public void updateUser(String userId, String fullName,
                           String branchCode, String initial, String updatedBy) {
        SystemUser user = getExistingUser(userId);
        user.setFullName(fullName);
        user.setBranchCode(branchCode);
        user.setInitial(initial);
        userMgmtDao.updateUser(user);
        System.out.println("User updated: " + userId + " by: " + updatedBy);
    }

    @Override
    public void changeUserRole(String userId, String newRoleCode, String updatedBy) {
        SystemUser user = getExistingUser(userId);

        SystemRole newRole = userMgmtDao.findRoleByCode(newRoleCode);
        if (newRole == null) {
            throw new RuntimeException("Role not found: " + newRoleCode);
        }

        // check if user already has a role
        UserRole existingUserRole = userMgmtDao.findUserRole(user.getId());

        if (existingUserRole != null) {
            // update existing role
            userMgmtDao.updateUserRole(user.getId(), newRole.getId());
        } else {
            // assign new role
            UserRole userRole = new UserRole();
            userRole.setSystemUser(user);
            userRole.setSystemRole(newRole);
            userRole.setAssignedBy(updatedBy);
            userRole.setAssignedAt(LocalDateTime.now());
            userMgmtDao.saveUserRole(userRole);
        }

        System.out.println("Role changed for: " + userId
            + " to: " + newRoleCode + " by: " + updatedBy);
    }

    @Override
    public void resetPassword(String userId, String newPassword, String updatedBy) {
        SystemUser user = getExistingUser(userId);

        String hashedPassword = BCrypt.withDefaults()
                .hashToString(12, newPassword.toCharArray());

        user.setPasswordHash(hashedPassword);
        userMgmtDao.updateUser(user);
        System.out.println("Password reset for: " + userId + " by: " + updatedBy);
    }

    @Override
    public List<SystemUser> getAllUsers() {
        return userMgmtDao.getAllUsers();
    }

    @Override
    public List<SystemUser> getUsersByBranch(String branchCode) {
        return userMgmtDao.getUsersByBranch(branchCode);
    }

    @Override
    public List<SystemRole> getAllRoles() {
        return userMgmtDao.getAllRoles();
    }

    @Override
    public SystemUser findByUserId(String userId) {
        return userMgmtDao.findByUserId(userId);
    }

    @Override
    public String getRoleCodeForUser(String userId) {
        SystemUser user = userMgmtDao.findByUserId(userId);
        if (user == null) return null;

        UserRole userRole = userMgmtDao.findUserRole(user.getId());
        if (userRole == null) return null;

        return userRole.getSystemRole().getRoleCode();
    }

    // ── Helper ────────────────────────────────────────────────────────

    private SystemUser getExistingUser(String userId) {
        SystemUser user = userMgmtDao.findByUserId(userId);
        if (user == null) {
            throw new RuntimeException("User not found: " + userId);
        }
        return user;
    }
}