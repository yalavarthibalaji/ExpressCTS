package com.iispl.service;

import java.util.List;

import com.iispl.entity.SystemRole;
import com.iispl.entity.SystemUser;
import com.iispl.entity.UserRequest;

public interface UserManagementService {

    // add a new user with a role
    void addUser(String userId, String fullName, String branchCode,
                 String initial, String roleCode, String createdBy);

    // enable a user account
    void enableUser(String userId, String updatedBy);

    // disable a user account
    void disableUser(String userId, String updatedBy);

    // update user details
    void updateUser(String userId, String fullName,
                    String branchCode, String initial, String updatedBy);

    // change user role
    void changeUserRole(String userId, String newRoleCode, String updatedBy);

    // reset user password
    void resetPassword(String userId, String newPassword, String updatedBy);

    // get all users
    List<SystemUser> getAllUsers();

    // get all users by branch
    List<SystemUser> getUsersByBranch(String branchCode);

    // get all roles for dropdown
    List<SystemRole> getAllRoles();

    // find user by userId
    SystemUser findByUserId(String userId);

    // get role code for a user
    String getRoleCodeForUser(String userId);

	//Giri added
    
    // 2. Delete a user permanently
    void deleteUser(String userId, String adminId);
    
 // 9. User Request panel methods
    List<UserRequest> getPendingRequests();
    void acceptRequest(String requestId, String actionedBy);
    void declineRequest(String requestId, String actionedBy);

}