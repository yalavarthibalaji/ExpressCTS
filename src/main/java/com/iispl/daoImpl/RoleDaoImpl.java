package com.iispl.daoImpl;

import com.iispl.dao.RoleDao;
import com.iispl.entity.Role;
import com.iispl.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import java.util.List;

public class RoleDaoImpl implements RoleDao {

    @Override
    public List<Role> findAll() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            NativeQuery<Role> q = session.createNativeQuery(
                "SELECT * FROM role WHERE is_active = TRUE ORDER BY role_name", Role.class);
            return q.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        } finally {
            session.close();
        }
    }

    @Override
    public Role findById(Integer id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.get(Role.class, id);
        } finally {
            session.close();
        }
    }
}