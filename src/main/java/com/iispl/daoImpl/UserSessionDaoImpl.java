package com.iispl.daoImpl;

import com.iispl.dao.UserSessionDao;
import com.iispl.entity.UserSession;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;

import java.util.ArrayList;
import java.util.List;

public class UserSessionDaoImpl implements UserSessionDao {

    @Override
    public UserSession save(UserSession s) {
        if (s == null) return null;

        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();
            session.persist(s);
            tx.commit();
            return s;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("UserSessionDao → save failed: " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean expireByToken(String sessionToken) {
        if (sessionToken == null || sessionToken.isEmpty()) return false;

        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();
            String sql = "UPDATE user_sessions "
                       + "SET logout_at  = NOW(), "
                       + "    is_expired = true "
                       + "WHERE session_token = :tk "
                       + "  AND is_expired    = false";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("tk", sessionToken);
            int rows = q.executeUpdate();
            tx.commit();
            return rows > 0;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("UserSessionDao → expireByToken failed: "
                    + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    @Override
    public int expireAllForUser(Long userId) {
        if (userId == null) return 0;

        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();
            String sql = "UPDATE user_sessions "
                       + "SET logout_at  = NOW(), "
                       + "    is_expired = true "
                       + "WHERE user_id    = :uid "
                       + "  AND is_expired = false";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("uid", userId);
            int rows = q.executeUpdate();
            tx.commit();
            System.out.println("UserSessionDao → expireAllForUser: userId="
                    + userId + " expired=" + rows);
            return rows;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("UserSessionDao → expireAllForUser failed: "
                    + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }

    @Override
    public List<UserSession> findActiveByUser(Long userId) {
        if (userId == null) return new ArrayList<>();

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql = "SELECT * FROM user_sessions "
                       + "WHERE user_id    = :uid "
                       + "  AND is_expired = false "
                       + "ORDER BY login_at DESC";
            NativeQuery<UserSession> q =
                    session.createNativeQuery(sql, UserSession.class);
            q.setParameter("uid", userId);
            return q.list();
        } catch (Exception e) {
            System.err.println("UserSessionDao → findActiveByUser failed: "
                    + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }
}