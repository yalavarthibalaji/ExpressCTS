package com.iispl.service;

import java.util.List;
import com.iispl.dto.NotificationDto;

public interface NotificationService {
    List<NotificationDto> getUnreadByUser(Long userId);
    void markAllRead(Long userId);
    void createNotification(Long userId, String message);
}