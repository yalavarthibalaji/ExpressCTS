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
    public List<Long> findUserIdsByRole(String roleCode) ;
}