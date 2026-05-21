package com.iispl.dao;

import com.iispl.entity.SystemUser;

public interface UserDao {

   
    SystemUser findByUserId(String userId); // find a user by their userId string (e.g. "maker1", "checker1")

    
    String findRoleCodeByUserId(Long systemUserId); // find the role code assigned to a user (e.g. "maker", "checker", "cts")
    // returns null if no role is assigned

}