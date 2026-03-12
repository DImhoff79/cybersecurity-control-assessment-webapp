package com.cyberassessment.service;

import com.cyberassessment.dto.NotificationDto;
import com.cyberassessment.entity.Notification;
import com.cyberassessment.entity.NotificationCategory;
import com.cyberassessment.entity.User;
import com.cyberassessment.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public static NotificationDto toDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .category(notification.getCategory())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .auditId(notification.getAuditId())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> myNotifications() {
        User user = currentUserService.getCurrentUserOrThrow();
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(NotificationService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long myUnreadCount() {
        User user = currentUserService.getCurrentUserOrThrow();
        return notificationRepository.countByUserIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public void markRead(Long notificationId) {
        User user = currentUserService.getCurrentUserOrThrow();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Notification does not belong to current user");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllRead() {
        User user = currentUserService.getCurrentUserOrThrow();
        List<Notification> unread = notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(user.getId());
        if (unread.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        unread.forEach(n -> n.setReadAt(now));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void createForUser(User user, NotificationCategory category, String title, String message, Long auditId) {
        if (user == null) return;
        Notification notification = Notification.builder()
                .user(user)
                .category(category)
                .title(title)
                .message(message)
                .auditId(auditId)
                .build();
        notificationRepository.save(notification);
    }
}
