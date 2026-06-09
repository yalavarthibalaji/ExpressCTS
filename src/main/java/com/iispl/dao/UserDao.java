package com.iispl.dao;

import com.iispl.entity.User;
import java.util.List;

public interface UserDao {
    User       findByLoginId(String userLoginId);
    List<User> findAll();
    boolean    existsByLoginId(String userLoginId);
    boolean    existsByEmail(String email);
    boolean    save(User user);
    boolean    updateStatus(Long userId, String newStatus);
    
    /**
     * Updates last_login_at = NOW() for the given user.
     * Called by validateLogin() on successful password match.
     */
    boolean updateLastLogin(Long userId);

    /**
     * Increments failed_attempts by 1.
     * Called by validateLogin() on password mismatch.
     *
     * @return the NEW count of failed attempts after the increment
     */
    int incrementFailedAttempts(Long userId);

    /**
     * Resets failed_attempts back to 0.
     * Called by validateLogin() on successful login.
     */
    boolean resetFailedAttempts(Long userId);

    /**
     * Sets is_locked = true on the user account.
     * Called when failed_attempts reaches the threshold (5).
     * Admin must manually unlock the account.
     */
    boolean lockUser(Long userId);
}