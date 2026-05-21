package com.iispl.daoImpl;

import com.iispl.dao.UserDao;
import com.iispl.db.HibernateUtil;
import com.iispl.entity.SystemUser;
import com.iispl.entity.UserRole;

import org.hibernate.Session;
import org.hibernate.query.Query;

public class UserDaoImpl implements UserDao {

    @Override
    public SystemUser findByUserId(String userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Query<SystemUser> query = session.createQuery(
                "FROM SystemUser WHERE userId = :userId AND active = true",
                SystemUser.class
            );
            query.setParameter("userId", userId);
            return query.uniqueResult();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find user: " + e.getMessage());
        }
    }

    @Override
    public String findRoleCodeByUserId(Long systemUserId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // Join user_roles → roles to get the role_code for this user
            Query<String> query = session.createQuery(
                "SELECT ur.systemRole.roleCode FROM UserRole ur " +
                "WHERE ur.systemUser.id = :userId",
                String.class
            );
            query.setParameter("userId", systemUserId);

            // A user may have multiple roles, take the first one
            // In a real project, handle multi-role users properly
            return query.setMaxResults(1).uniqueResult();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find role: " + e.getMessage());
        }
    }
}