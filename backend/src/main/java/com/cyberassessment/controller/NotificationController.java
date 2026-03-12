package com.cyberassessment.controller;

import com.cyberassessment.dto.NotificationDto;
import com.cyberassessment.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<NotificationDto> myNotifications() {
        return notificationService.myNotifications();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notificationService.myUnreadCount());
    }

    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public void markRead(@PathVariable Long notificationId) {
        notificationService.markRead(notificationId);
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public void markAllRead() {
        notificationService.markAllRead();
    }
}
