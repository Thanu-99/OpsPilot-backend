package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.entity.Notification;

import java.util.List;

public interface NotificationService {

    Notification createNotification(
            Long userId,
            String type,
            String title,
            String message
    );

    List<Notification> getUserNotifications(Long userId);

    List<Notification> getUnreadNotifications(Long userId);

    long getUnreadCount(Long userId);

    Notification markAsRead(Long id);

    void markAllAsRead(Long userId);

    void deleteNotification(Long id);
}