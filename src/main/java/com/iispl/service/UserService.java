package com.iispl.service;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.Role;
import com.iispl.entity.User;
import java.util.List;

public interface UserService {

    /** Login validation. Returns DTO on success, null otherwise. */
    LoginDTO validateLogin(String userLoginId, String password);

    /** List all users for User Management page. */
    List<User> getAllUsers();

    /** Get all active roles for the role dropdown. */
    List<Role> getAllRoles();

    /**
     * Create a new user.
     * @return error message on failure, or null on success.
     */
    String createUser(String loginId, String fullName, String email,
                      String mobile, String plainPassword, Integer roleId);

    /** Toggle user status (ACTIVE / INACTIVE). */
    boolean toggleUserStatus(Long userId);
}