package com.iispl.dao;

import java.util.List;

import com.iispl.entity.SystemRole;
import com.iispl.entity.SystemUser;
import com.iispl.entity.UserRequest;
import com.iispl.entity.UserRole;

public interface UserManagementDao {

	void saveUser(SystemUser user);

	void updateUser(SystemUser user);

	SystemUser findByUserId(String userId);

	SystemUser findById(Long id);

	List<SystemUser> getAllUsers();

	List<SystemUser> getUsersByBranch(String branchCode);

	void saveUserRole(UserRole userRole);

	List<SystemRole> getAllRoles();

	SystemRole findRoleByCode(String roleCode);

	SystemRole findRoleById(Long id);

	UserRole findUserRole(Long systemUserId);
 
	void updateUserRole(Long systemUserId, Long newRoleId);

	boolean userIdExists(String userId);
	
	//Added by Giri
	
	void deleteUser(String userId);
	void saveRequest(UserRequest request);
	void updateRequest(UserRequest request);
	List<UserRequest> getPendingRequests();
	UserRequest findRequestById(String requestId);
}