package com.iispl.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iispl.dao.NotificationDao;
import com.iispl.daoImpl.NotificationDaoImpl;
import com.iispl.dto.NotificationDto;
import com.iispl.entity.Notification;
import com.iispl.service.NotificationService;

public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOG =
            Logger.getLogger(NotificationServiceImpl.class.getName());

    private final NotificationDao notificationDao = new NotificationDaoImpl();

    @Override
    public List<NotificationDto> getUnreadByUser(Long userId) {
        List<NotificationDto> result = new ArrayList<>();
        try {
            List<Notification> rows = notificationDao.findUnreadByUserId(userId);
            LOG.info("Fetched " + rows.size() + " unread notifications for userId=" + userId);
            for (Notification n : rows) {
                result.add(toDto(n));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to fetch notifications for userId=" + userId, e);
        }
        return result;
    }

    @Override
    public void markAllRead(Long userId) {
        try {
            notificationDao.markAllReadByUserId(userId);
            LOG.info("Marked all notifications read for userId=" + userId);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to mark notifications read for userId=" + userId, e);
        }
    }

    @Override
    public void createNotification(Long userId, String message) {
        try {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setMessage(message);
            n.setCreatedAt(LocalDateTime.now());
            n.setRead(false);
            notificationDao.save(n);
            LOG.info("Notification created for userId=" + userId + " | msg=" + message);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create notification for userId=" + userId, e);
        }
    }

    private NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setMessage(n.getMessage());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setRead(n.isRead());
        return dto;
    }
}