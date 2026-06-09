package com.iispl.dao;

import com.iispl.entity.outward.Notification;
import java.util.List;

/**
 * File    : com/iispl/dao/NotificationDao.java
 * Purpose : Database operations for the notification table.
 */
public interface NotificationDao {

    /** Save a new notification row. */
    boolean save(Notification notification);

    /**
     * Returns all unread notifications for a user, newest first.
     * Used to populate the topbar popup on page load and timer refresh.
     */
    List<Notification> findUnreadByRecipient(Long recipientId);

    /**
     * Count of unread notifications for a user.
     * Used to drive the red badge number on the bell icon.
     */
    int countUnread(Long recipientId);

    /**
     * Marks a single notification as read.
     * Called when the user clicks a specific notification item.
     */
    boolean markRead(Long notificationId);

    /**
     * Marks ALL unread notifications for a user as read.
     * Called when the user clicks "Mark all read".
     */
    boolean markAllRead(Long recipientId);

    /**
     * Returns IDs of all active CHECKER_OUTWARD users.
     * Used when sending a RESUBMITTED notification to every checker.
     */
    List<Long> findCheckerOutwardUserIds();
}