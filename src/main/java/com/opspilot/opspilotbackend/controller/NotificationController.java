package com.opspilot.opspilotbackend.controller;

import com.opspilot.opspilotbackend.entity.Notification;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.repository.NotificationRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationService notificationService,
            NotificationRepository notificationRepository,
            UserRepository userRepository) {

        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    public ResponseEntity<List<Notification>> getMyNotifications(
            Authentication authentication) {

        return ResponseEntity.ok(
                notificationService.getUserNotifications(
                        getCurrentUserId(authentication)
                )
        );
    }

    @GetMapping("/my/unread")
    public ResponseEntity<List<Notification>> getMyUnreadNotifications(
            Authentication authentication) {

        return ResponseEntity.ok(
                notificationService.getUnreadNotifications(
                        getCurrentUserId(authentication)
                )
        );
    }

    @GetMapping("/my/unread/count")
    public ResponseEntity<Long> getMyUnreadCount(
            Authentication authentication) {

        return ResponseEntity.ok(
                notificationService.getUnreadCount(
                        getCurrentUserId(authentication)
                )
        );
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        assertNotificationBelongsToCurrentUser(id, authentication);

        return ResponseEntity.ok(
                notificationService.markAsRead(id)
        );
    }

    @PutMapping("/my/read-all")
    public ResponseEntity<Void> markAllAsRead(
            Authentication authentication) {

        notificationService.markAllAsRead(
                getCurrentUserId(authentication)
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            Authentication authentication) {

        assertNotificationBelongsToCurrentUser(id, authentication);

        notificationService.deleteNotification(id);

        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(Authentication authentication) {

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );

        return user.getId();
    }

    private void assertNotificationBelongsToCurrentUser(
            Long notificationId,
            Authentication authentication) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Notification not found"
                        )
                );

        if (!notification.getUserId().equals(
                getCurrentUserId(authentication))) {

            throw new AccessDeniedException(
                    "You cannot access this notification"
            );
        }
    }
}