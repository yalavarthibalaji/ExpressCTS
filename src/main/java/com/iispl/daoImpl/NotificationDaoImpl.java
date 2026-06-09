package com.iispl.daoImpl;

import com.iispl.dao.NotificationDao;
import com.iispl.entity.outward.Notification;
import com.iispl.entity.User;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * File    : com/iispl/daoImpl/NotificationDaoImpl.java
 * Purpose : Native SQL implementation of NotificationDao.
 *           All queries use plain SQL — no HQL.
 */
public class NotificationDaoImpl implements NotificationDao {

    // ════════════════════════════════════════════════════
    //  Save
    // ════════════════════════════════════════════════════

    @Override
    public boolean save(Notification notification) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(notification);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("NotificationDao → save failed: " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Find Unread
    // ════════════════════════════════════════════════════

    @Override
    public List<Notification> findUnreadByRecipient(Long recipientId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql =
                "SELECT * FROM notification "
              + "WHERE recipient_id = :rid "
              + "  AND is_read = false "
              + "ORDER BY created_at DESC "
              + "LIMIT 20";

            NativeQuery<Notification> q =
                session.createNativeQuery(sql, Notification.class);
            q.setParameter("rid", recipientId);
            return q.list();
        } catch (Exception e) {
            System.err.println("NotificationDao → findUnreadByRecipient failed: "
                    + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Count Unread
    // ════════════════════════════════════════════════════

    @Override
    public int countUnread(Long recipientId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String sql =
                "SELECT COUNT(*) FROM notification "
              + "WHERE recipient_id = :rid AND is_read = false";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("rid", recipientId);
            Number n = (Number) q.uniqueResult();
            return n != null ? n.intValue() : 0;
        } catch (Exception e) {
            System.err.println("NotificationDao → countUnread failed: "
                    + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Mark Read — single
    // ════════════════════════════════════════════════════

    @Override
    public boolean markRead(Long notificationId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            String sql =
                "UPDATE notification SET is_read = true "
              + "WHERE id = :nid";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("nid", notificationId);
            q.executeUpdate();
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("NotificationDao → markRead failed: "
                    + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Mark All Read
    // ════════════════════════════════════════════════════

    @Override
    public boolean markAllRead(Long recipientId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            String sql =
                "UPDATE notification SET is_read = true "
              + "WHERE recipient_id = :rid AND is_read = false";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("rid", recipientId);
            q.executeUpdate();
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("NotificationDao → markAllRead failed: "
                    + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    // ════════════════════════════════════════════════════
    //  Find Checker Outward User IDs
    // ════════════════════════════════════════════════════

    @Override
    public List<Long> findCheckerOutwardUserIds() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            // Join users → role to filter by role_code = 'CHECKER_OUTWARD'
            // Only active, non-locked users receive notifications
            String sql =
                "SELECT u.id FROM users u "
              + "JOIN role r ON u.role_id = r.id "
              + "WHERE r.role_code = 'CHECKER_OUTWARD' "
              + "  AND u.status    = 'ACTIVE' "
              + "  AND u.is_locked = false";
            NativeQuery<?> q = session.createNativeQuery(sql);
            List<?> rows = q.list();

            List<Long> ids = new ArrayList<>();
            for (Object row : rows) {
                ids.add(((Number) row).longValue());
            }
            return ids;
        } catch (Exception e) {
            System.err.println("NotificationDao → findCheckerOutwardUserIds failed: "
                    + e.getMessage());
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }
}