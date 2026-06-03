package com.iispl.dao;

import com.iispl.entity.Role;
import java.util.List;

public interface RoleDao {
    List<Role> findAll();
    Role findById(Integer id);
}