
package com.iispl.service;

import at.favre.lib.crypto.bcrypt.BCrypt;


import com.iispl.dao.UserDao;
import com.iispl.daoImpl.UserDaoImpl;
import com.iispl.entity.SystemUser;
import com.iispl.dto.UserModel;

/**
 * AuthService.java
 * Service class — handles login authentication using the database.
 *
 * Package : com.iispl.service
 * Pattern : Service layer (business logic only, no ZK code)
 *
 * Phase 1 Change:
 *   - Before: credentials were hardcoded in UserUtil.java
 *   - Now   : credentials are fetched from system_users table in Supabase
 *   - Password: BCrypt hash stored in DB, verified using at.favre.lib BCrypt
 *
 * Called by: LoginController
 */
public class AuthService {

    private final UserDao userDao = new UserDaoImpl();

    /**
     * Authenticate user against the database.
     *
     * Steps:
     *   1. Fetch SystemUser from DB by userId
     *   2. Verify the plain-text password against the stored BCrypt hash
     *   3. Fetch the role assigned to this user from user_roles table
     *   4. Build and return a UserModel for the session
     *
     * @return UserModel if login is valid, null if invalid
     */
    public UserModel authenticate(String userId, String password) {

        if (userId == null || userId.trim().isEmpty()) {
            System.out.println("DEBUG: userId is empty");
            return null;
        }
        if (password == null || password.trim().isEmpty()) {
            System.out.println("DEBUG: password is empty");
            return null;
        }

        System.out.println("DEBUG: Trying to find user → " + userId.trim());

        // Step 1: Find user in DB
        SystemUser dbUser = userDao.findByUserId(userId.trim());

        if (dbUser == null) {
            System.out.println("DEBUG: No user found in DB for userId = " + userId.trim());
            return null;
        }

        System.out.println("DEBUG: User found → " + dbUser.getUserId());
        System.out.println("DEBUG: passwordHash from DB → " + dbUser.getPasswordHash());

        // Step 2: Verify BCrypt password
        BCrypt.Result result = BCrypt.verifyer().verify(
            password.trim().toCharArray(),
            dbUser.getPasswordHash()
        );

        System.out.println("DEBUG: BCrypt verified → " + result.verified);

        if (!result.verified) {
            System.out.println("DEBUG: Password did not match");
            return null;
        }

        // Step 3: Get role code
        String roleCode = userDao.findRoleCodeByUserId(dbUser.getId());
        System.out.println("DEBUG: roleCode from DB → " + roleCode);

        if (roleCode == null) {
            System.out.println("DEBUG: No role found for user id = " + dbUser.getId());
            return null;
        }

        // Step 4: Build UserModel
        UserModel model = buildUserModel(dbUser, roleCode);
        System.out.println("DEBUG: UserModel built → " + model.toString());
        return model;
    }
    /**
     * Detect role while user is still typing (used by LoginController onChanging).
     * Same logic as authenticate — only shows role pill if both fields match DB.
     */
    public UserModel detectRole(String userId, String password) {
        return authenticate(userId, password);
    }

    /**
     * Maps DB data (SystemUser + roleCode) → UserModel used by ZK session.
     *
     * roleCode values match what was previously in UserUtil:
     *   "MAKER"       → Maker (Data Entry)
     *   "CHECKER"     → Checker (Verifier)
     *   "SUPERVISOR"  → CTS Supervisor
     *   "ADMIN"       → CTS Admin
     *   "MICR_REPAIR" → MICR Operator
     */
    private UserModel buildUserModel(SystemUser dbUser, String roleCode) {

        // Normalize roleCode to lowercase for comparison
        String role = roleCode.toLowerCase();

        String roleLabel;
        String roleIcon;
        String roleInitial;
        String defaultSection;

        // Map roleCode → display info matching the Figma design
        switch (role) {
            case "maker":
                roleLabel      = "Maker (Data Entry)";
                roleIcon       = "✏️";
                roleInitial    = "M";
                defaultSection = "ow-create-batch";
                break;
            case "checker":
                roleLabel      = "Checker (Verifier)";
                roleIcon       = "✅";
                roleInitial    = "C";
                defaultSection = "ow-checker";
                break;
            case "supervisor":
                roleLabel      = "CTS Supervisor";
                roleIcon       = "👁";
                roleInitial    = "V";
                defaultSection = "dashboard";
                break;
            case "admin":
                roleLabel      = "CTS Admin";
                roleIcon       = "🏦";
                roleInitial    = "A";
                defaultSection = "dashboard";
                break;
            case "micr_repair":
                roleLabel      = "MICR Operator";
                roleIcon       = "🔧";
                roleInitial    = "MO";
                defaultSection = "ow-create-batch";
                break;
            default:
                // Unknown role — use roleCode as label
                roleLabel      = roleCode;
                roleIcon       = "👤";
                roleInitial    = roleCode.substring(0, 1).toUpperCase();
                defaultSection = "dashboard";
                break;
        }

        UserModel user = new UserModel(
            dbUser.getUserId(),
            "",                  // never put password in session
            role,
            roleLabel,
            roleInitial,
            roleIcon,
            defaultSection
        );
        user.setActive(Boolean.TRUE.equals(dbUser.getActive()));
        return user;
    }
}