package com.iispl.service;

import com.iispl.model.UserModel;
import com.iispl.util.UserUtil;

/**
 * AuthService.java
 * Service class — handles login authentication logic.
 *
 * Package : com.iispl.cts.service
 * Pattern : MVC — Service layer (business logic)
 *
 * Called by LoginController.
 * Does NOT talk to ZK components — pure Java logic only.
 *
 * In a real project:
 *   - Replace UserUtil.findByCredentials() with a JDBC DAO call
 *   - Add password hashing (BCrypt / SHA-256)
 *   - Add account lockout logic (3 failed attempts → lock)
 */
public class AuthService {

    /**
     * Authenticate user with userId and password.
     *
     * @param userId   raw string from the textbox
     * @param password raw string from the password box
     * @return UserModel if credentials are valid, null if invalid
     */
    public UserModel authenticate(String userId, String password) {

        // Basic blank check — controller also checks, this is a safety net
        if (userId == null || userId.trim().isEmpty()) return null;
        if (password == null || password.trim().isEmpty()) return null;

        // Delegate to UserUtil (which holds the credential registry)
        return UserUtil.findByCredentials(userId, password);
    }

    /**
     * Auto-detect role while user is still typing.
     * Only userId + password together must match for detection.
     *
     * @return UserModel if both fields already match a user, null otherwise
     */
    public UserModel detectRole(String userId, String password) {
        if (userId == null || userId.trim().isEmpty()) return null;
        if (password == null || password.trim().isEmpty()) return null;
        return UserUtil.findByCredentials(userId, password);
    }
}
