package com.iispl.dao;

import com.iispl.entity.Notification;
import java.util.List;

public interface NotificationDao {

    void save(Notification notification);
    List<Notification> findUnreadByUserId(Long userId);
    void markAllReadByUserId(Long userId);
}