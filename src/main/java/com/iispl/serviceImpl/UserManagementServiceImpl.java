package com.iispl.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iispl.dao.UserManagementDao;
import com.iispl.daoImpl.UserManagementDaoImpl;
import com.iispl.entity.SystemRole;
import com.iispl.entity.SystemUser;
import com.iispl.entity.UserRequest;
import com.iispl.entity.UserRole;
import com.iispl.service.UserManagementService;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class UserManagementServiceImpl implements UserManagementService {

    private UserManagementDao userMgmtDao = new UserManagementDaoImpl();

    private static final String DEFAULT_PASSWORD = "Test@123";

    // ─────────────────────────────────────────────────────────────────
    // IMPORTANT:
    // Methods 1-7 below do NOT execute DB changes directly anymore.
    // They only save a PENDING UserRequest.
    // The actual DB change happens only inside acceptRequest()
    // when the admin clicks Accept in the User Request panel.
    // ─────────────────────────────────────────────────────────────────

    // ── 1. Add User → saves PENDING request ──────────────────────────

    @Override
    public void addUser(String userId, String fullName, String branchCode,
                        String initial, String roleCode, String createdBy) {

        // check if userId already exists before raising request
        if (userMgmtDao.userIdExists(userId)) {
            throw new RuntimeException("User ID already exists: " + userId);
        }

        // check if role is valid before raising request
        SystemRole role = userMgmtDao.findRoleByCode(roleCode);
        if (role == null) {
            throw new RuntimeException("Role not found: " + roleCode);
        }

        // details format: "fullName|branchCode|initial|roleCode"
        String details = fullName + "|" + branchCode + "|" + initial + "|" + roleCode;

        saveRequest("ADD_USER", userId, createdBy, details);

        System.out.println("ADD_USER request raised for: " + userId + " by: " + createdBy);
    }

    // ── 2. Delete User → saves PENDING request ────────────────────────

    @Override
    public void deleteUser(String userId, String adminId) {

        // check user exists before raising request
        SystemUser user = userMgmtDao.findByUserId(userId);
        if (user == null) {
            throw new RuntimeException("User not found: " + userId);
        }

        saveRequest("DELETE_USER", userId, adminId, "Delete user account");

        System.out.println("DELETE_USER request raised for: " + userId + " by: " + adminId);
    }

    // ── 3. Enable User → saves PENDING request ───────────────────────

    @Override
    public void enableUser(String userId, String updatedBy) {
        getExistingUser(userId); // validate user exists
        saveRequest("ENABLE_USER", userId, updatedBy, "Enable user account");
        System.out.println("ENABLE_USER request raised for: " + userId + " by: " + updatedBy);
    }

    // ── 4. Disable User → saves PENDING request ──────────────────────

    @Override
    public void disableUser(String userId, String updatedBy) {
        getExistingUser(userId); // validate user exists
        saveRequest("DISABLE_USER", userId, updatedBy, "Disable user account");
        System.out.println("DISABLE_USER request raised for: " + userId + " by: " + updatedBy);
    }

    // ── 5. Update User → saves PENDING request ───────────────────────

    @Override
    public void updateUser(String userId, String fullName,
                           String branchCode, String initial, String updatedBy) {
        getExistingUser(userId); // validate user exists

        // details format: "fullName|branchCode|initial"
        String details = fullName + "|" + branchCode + "|" + initial;

        saveRequest("UPDATE_USER", userId, updatedBy, details);

        System.out.println("UPDATE_USER request raised for: " + userId + " by: " + updatedBy);
    }

    // ── 6. Change Role → saves PENDING request ───────────────────────

    @Override
    public void changeUserRole(String userId, String newRoleCode, String updatedBy) {
        getExistingUser(userId); // validate user exists

        SystemRole role = userMgmtDao.findRoleByCode(newRoleCode);
        if (role == null) {
            throw new RuntimeException("Role not found: " + newRoleCode);
        }

        // details = just the new role code
        saveRequest("CHANGE_ROLE", userId, updatedBy, newRoleCode);

        System.out.println("CHANGE_ROLE request raised for: " + userId
            + " new role: " + newRoleCode + " by: " + updatedBy);
    }

    // ── 7. Reset Password → saves PENDING request ────────────────────

    @Override
    public void resetPassword(String userId, String newPassword, String updatedBy) {
        getExistingUser(userId); // validate user exists

        // hash the password now and store in details
        // so when accepted, we just save the hash directly without needing the plain password
        String hashedPassword = BCrypt.withDefaults()
                .hashToString(12, newPassword.toCharArray());

        saveRequest("RESET_PASSWORD", userId, updatedBy, hashedPassword);

        System.out.println("RESET_PASSWORD request raised for: " + userId + " by: " + updatedBy);
    }

    // ── 8. Read-only methods (no request needed) ──────────────────────

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

    // ── 9. User Request methods ───────────────────────────────────────

    @Override
    public List<UserRequest> getPendingRequests() {
        return userMgmtDao.getPendingRequests();
    }

    @Override
    public void acceptRequest(String requestId, String actionedBy) {
        UserRequest request = getExistingRequest(requestId);

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Request is already " + request.getStatus());
        }

        String type   = request.getRequestType();
        String userId = request.getTargetUserId();

        // Execute the actual DB operation based on request type
        switch (type) {

            case "ADD_USER":
                // details format: "fullName|branchCode|initial|roleCode"
                String[] addParts = request.getDetails().split("\\|");
                if (addParts.length < 4) {
                    throw new RuntimeException("Invalid ADD_USER request details.");
                }
                executeAddUser(userId, addParts[0], addParts[1], addParts[2], addParts[3], actionedBy);
                break;

            case "DELETE_USER":
                executeDeleteUser(userId);
                break;

            case "ENABLE_USER":
                executeEnableUser(userId);
                break;

            case "DISABLE_USER":
                executeDisableUser(userId);
                break;

            case "UPDATE_USER":
                // details format: "fullName|branchCode|initial"
                String[] updateParts = request.getDetails().split("\\|");
                if (updateParts.length < 3) {
                    throw new RuntimeException("Invalid UPDATE_USER request details.");
                }
                executeUpdateUser(userId, updateParts[0], updateParts[1], updateParts[2]);
                break;

            case "CHANGE_ROLE":
                // details = newRoleCode
                executeChangeRole(userId, request.getDetails(), actionedBy);
                break;

            case "RESET_PASSWORD":
                // details = already hashed password
                executeResetPassword(userId, request.getDetails());
                break;

            default:
                throw new RuntimeException("Unknown request type: " + type);
        }

        // Mark request as ACCEPTED
        request.setStatus("ACCEPTED");
        request.setActionedBy(actionedBy);
        userMgmtDao.updateRequest(request);

        System.out.println("Request " + requestId + " ACCEPTED by: " + actionedBy);
    }

    @Override
    public void declineRequest(String requestId, String actionedBy) {
        UserRequest request = getExistingRequest(requestId);

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Request is already " + request.getStatus());
        }

        // Just mark as DECLINED — no DB operation performed
        request.setStatus("DECLINED");
        request.setActionedBy(actionedBy);
        userMgmtDao.updateRequest(request);

        System.out.println("Request " + requestId + " DECLINED by: " + actionedBy);
    }

    // ── Private execute methods (called only on Accept) ───────────────
    // These do the actual DB work

    private void executeAddUser(String userId, String fullName, String branchCode,
                                String initial, String roleCode, String createdBy) {
        SystemRole role = userMgmtDao.findRoleByCode(roleCode);
        if (role == null) throw new RuntimeException("Role not found: " + roleCode);

        String hashedPassword = BCrypt.withDefaults()
                .hashToString(12, DEFAULT_PASSWORD.toCharArray());

        SystemUser user = new SystemUser();
        user.setUserId(userId);
        user.setPasswordHash(hashedPassword);
        user.setFullName(fullName);
        user.setBranchCode(branchCode);
        user.setInitial(initial);
        user.setActive(true);
        user.setCreatedBy(createdBy);
        user.setCreatedAt(LocalDateTime.now());
        userMgmtDao.saveUser(user);

        SystemUser savedUser = userMgmtDao.findByUserId(userId);
        UserRole userRole = new UserRole();
        userRole.setSystemUser(savedUser);
        userRole.setSystemRole(role);
        userRole.setAssignedBy(createdBy);
        userRole.setAssignedAt(LocalDateTime.now());
        userMgmtDao.saveUserRole(userRole);

        System.out.println("executeAddUser done: " + userId);
    }

    private void executeDeleteUser(String userId) {
        userMgmtDao.deleteUser(userId);
        System.out.println("executeDeleteUser done: " + userId);
    }

    private void executeEnableUser(String userId) {
        SystemUser user = getExistingUser(userId);
        user.setActive(true);
        userMgmtDao.updateUser(user);
        System.out.println("executeEnableUser done: " + userId);
    }

    private void executeDisableUser(String userId) {
        SystemUser user = getExistingUser(userId);
        user.setActive(false);
        userMgmtDao.updateUser(user);
        System.out.println("executeDisableUser done: " + userId);
    }

    private void executeUpdateUser(String userId, String fullName,
                                   String branchCode, String initial) {
        SystemUser user = getExistingUser(userId);
        user.setFullName(fullName);
        user.setBranchCode(branchCode);
        user.setInitial(initial);
        userMgmtDao.updateUser(user);
        System.out.println("executeUpdateUser done: " + userId);
    }

    private void executeChangeRole(String userId, String newRoleCode, String actionedBy) {
        SystemUser user = getExistingUser(userId);
        SystemRole newRole = userMgmtDao.findRoleByCode(newRoleCode);
        if (newRole == null) throw new RuntimeException("Role not found: " + newRoleCode);

        UserRole existingUserRole = userMgmtDao.findUserRole(user.getId());
        if (existingUserRole != null) {
            userMgmtDao.updateUserRole(user.getId(), newRole.getId());
        } else {
            UserRole userRole = new UserRole();
            userRole.setSystemUser(user);
            userRole.setSystemRole(newRole);
            userRole.setAssignedBy(actionedBy);
            userRole.setAssignedAt(LocalDateTime.now());
            userMgmtDao.saveUserRole(userRole);
        }
        System.out.println("executeChangeRole done: " + userId + " → " + newRoleCode);
    }

    private void executeResetPassword(String userId, String hashedPassword) {
        SystemUser user = getExistingUser(userId);
        user.setPasswordHash(hashedPassword);
        userMgmtDao.updateUser(user);
        System.out.println("executeResetPassword done: " + userId);
    }

    // ── Private helpers ───────────────────────────────────────────────

    // Builds and saves a UserRequest with PENDING status
    private void saveRequest(String type, String targetUserId,
                              String requestedBy, String details) {
        UserRequest request = new UserRequest();
        request.setRequestId(generateRequestId());
        request.setRequestType(type);
        request.setTargetUserId(targetUserId);
        request.setRequestedBy(requestedBy);
        request.setDetails(details);
        request.setStatus("PENDING");
        request.setRequestDate(LocalDate.now().toString());
        userMgmtDao.saveRequest(request);
    }

    private SystemUser getExistingUser(String userId) {
        SystemUser user = userMgmtDao.findByUserId(userId);
        if (user == null) throw new RuntimeException("User not found: " + userId);
        return user;
    }

    private UserRequest getExistingRequest(String requestId) {
        UserRequest request = userMgmtDao.findRequestById(requestId);
        if (request == null) throw new RuntimeException("Request not found: " + requestId);
        return request;
    }

    public static String generateRequestId() {
        String date = LocalDate.now().toString().replace("-", "");
        String uid  = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "REQ-" + date + "-" + uid;
    }
}