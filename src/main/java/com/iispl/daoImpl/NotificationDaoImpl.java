package com.iispl.daoImpl;

import com.iispl.dao.NotificationDao;
import com.iispl.entity.Notification;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationDaoImpl implements NotificationDao {

    private static final Logger LOG = Logger.getLogger(NotificationDaoImpl.class.getName());

    @Override
    public void save(Notification notification) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();
            session.persist(notification);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "Failed to save notification", e);
            throw new RuntimeException("Failed to save notification", e);
        } finally {
            session.close();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Notification> findUnreadByUserId(Long userId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.createNativeQuery(
                "SELECT * FROM notifications WHERE user_id = :uid AND is_read = false " +
                "ORDER BY created_at DESC", Notification.class)
                .setParameter("uid", userId)
                .getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to fetch notifications for userId=" + userId, e);
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    @Override
    public void markAllReadByUserId(Long userId) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();
            session.createNativeQuery(
                "UPDATE notifications SET is_read = true " +
                "WHERE user_id = :uid AND is_read = false")
                .setParameter("uid", userId)
                .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            LOG.log(Level.SEVERE, "Failed to mark notifications read for userId=" + userId, e);
        } finally {
            session.close();
        }
    }
}