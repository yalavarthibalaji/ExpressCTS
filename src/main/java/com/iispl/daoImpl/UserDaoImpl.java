package com.iispl.daoImpl;

import com.iispl.dao.UserDao;
import com.iispl.entity.User;
import com.iispl.util.HibernateUtil;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;

import java.util.ArrayList;
import java.util.List;

public class UserDaoImpl implements UserDao {

    @Override
    public User findByLoginId(String userLoginId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            NativeQuery<User> q = session.createNativeQuery(
                "SELECT * FROM users WHERE user_login_id = :loginId", User.class);
            q.setParameter("loginId", userLoginId);
            User user = q.uniqueResult();
            if (user != null && user.getRole() != null) {
                Hibernate.initialize(user.getRole());
            }
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            session.close();
        }
    }

    @Override
    public List<User> findAll() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            NativeQuery<User> q = session.createNativeQuery(
                "SELECT * FROM users ORDER BY created_at DESC", User.class);
            List<User> users = q.list();
            // Force-load role for each user before session closes
            for (User u : users) {
                if (u.getRole() != null) {
                    Hibernate.initialize(u.getRole());
                }
            }
            return users;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    @Override
    public boolean existsByLoginId(String userLoginId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            NativeQuery q = session.createNativeQuery(
                "SELECT COUNT(*) FROM users WHERE user_login_id = :loginId");
            q.setParameter("loginId", userLoginId);
            Number count = (Number) q.uniqueResult();
            return count != null && count.intValue() > 0;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            NativeQuery q = session.createNativeQuery(
                "SELECT COUNT(*) FROM users WHERE email = :email");
            q.setParameter("email", email);
            Number count = (Number) q.uniqueResult();
            return count != null && count.intValue() > 0;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean save(User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(user);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean updateStatus(Long userId, String newStatus) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            NativeQuery q = session.createNativeQuery(
                "UPDATE users SET status = :s, updated_at = NOW() WHERE id = :id");
            q.setParameter("s", newStatus);
            q.setParameter("id", userId);
            int rows = q.executeUpdate();
            tx.commit();
            return rows > 0;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        } finally {
            session.close();
        }
    }
    
    
 // ════════════════════════════════════════════════════════════════
//  Login Lifecycle Helpers (Phase F1)
// ════════════════════════════════════════════════════════════════

@Override
public boolean updateLastLogin(Long userId) {
    if (userId == null) return false;
    Session     session = HibernateUtil.getSessionFactory().openSession();
    Transaction tx      = null;
    try {
        tx = session.beginTransaction();
        String sql = "UPDATE users "
                   + "SET last_login_at = NOW() "
                   + "WHERE id = :id";
        NativeQuery<?> q = session.createNativeQuery(sql);
        q.setParameter("id", userId);
        int rows = q.executeUpdate();
        tx.commit();
        return rows > 0;
    } catch (Exception e) {
        if (tx != null) tx.rollback();
        System.err.println("UserDao → updateLastLogin failed: " + e.getMessage());
        return false;
    } finally {
        session.close();
    }
}

@Override
public int incrementFailedAttempts(Long userId) {
    if (userId == null) return 0;
    Session     session = HibernateUtil.getSessionFactory().openSession();
    Transaction tx      = null;
    try {
        tx = session.beginTransaction();
        String sql = "UPDATE users "
                   + "SET failed_attempts = failed_attempts + 1 "
                   + "WHERE id = :id "
                   + "RETURNING failed_attempts";
        NativeQuery<?> q = session.createNativeQuery(sql);
        q.setParameter("id", userId);
        Number n = (Number) q.uniqueResult();
        tx.commit();
        return n != null ? n.intValue() : 0;
    } catch (Exception e) {
        if (tx != null) tx.rollback();
        System.err.println("UserDao → incrementFailedAttempts failed: " + e.getMessage());
        return 0;
    } finally {
        session.close();
    }
}

@Override
public boolean resetFailedAttempts(Long userId) {
    if (userId == null) return false;
    Session     session = HibernateUtil.getSessionFactory().openSession();
    Transaction tx      = null;
    try {
        tx = session.beginTransaction();
        String sql = "UPDATE users "
                   + "SET failed_attempts = 0 "
                   + "WHERE id = :id";
        NativeQuery<?> q = session.createNativeQuery(sql);
        q.setParameter("id", userId);
        int rows = q.executeUpdate();
        tx.commit();
        return rows > 0;
    } catch (Exception e) {
        if (tx != null) tx.rollback();
        System.err.println("UserDao → resetFailedAttempts failed: " + e.getMessage());
        return false;
    } finally {
        session.close();
    }
}

@Override
public boolean lockUser(Long userId) {
    if (userId == null) return false;
    Session     session = HibernateUtil.getSessionFactory().openSession();
    Transaction tx      = null;
    try {
        tx = session.beginTransaction();
        String sql = "UPDATE users "
                   + "SET is_locked = true "
                   + "WHERE id = :id";
        NativeQuery<?> q = session.createNativeQuery(sql);
        q.setParameter("id", userId);
        int rows = q.executeUpdate();
        tx.commit();
        System.out.println("UserDao → LOCKED user id=" + userId);
        return rows > 0;
    } catch (Exception e) {
        if (tx != null) tx.rollback();
        System.err.println("UserDao → lockUser failed: " + e.getMessage());
        return false;
    } finally {
        session.close();
    }
}
}