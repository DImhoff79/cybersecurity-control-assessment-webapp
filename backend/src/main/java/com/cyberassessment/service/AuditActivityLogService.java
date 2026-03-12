package com.cyberassessment.service;

import com.cyberassessment.dto.AuditActivityLogDto;
import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditActivityLog;
import com.cyberassessment.entity.AuditActivityType;
import com.cyberassessment.entity.NotificationCategory;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.AuditActivityLogRepository;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditActivityLogService {

    private final AuditActivityLogRepository auditActivityLogRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public static AuditActivityLogDto toDto(AuditActivityLog log) {
        User actor = log.getActor();
        return AuditActivityLogDto.builder()
                .id(log.getId())
                .auditId(log.getAudit().getId())
                .activityType(log.getActivityType())
                .details(log.getDetails())
                .actorUserId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .createdAt(log.getCreatedAt())
                .build();
    }

    @Transactional
    public void log(Audit audit, AuditActivityType type, String details) {
        User actor = currentUserService.getCurrentUser().orElse(null);
        AuditActivityLog row = AuditActivityLog.builder()
                .audit(audit)
                .actor(actor)
                .activityType(type)
                .details(details)
                .build();
        auditActivityLogRepository.save(row);
        createNotifications(audit, type, details, actor);
    }

    @Transactional(readOnly = true)
    public List<AuditActivityLogDto> listByAuditId(Long auditId) {
        return auditActivityLogRepository.findByAuditIdOrderByCreatedAtDesc(auditId)
                .stream()
                .map(AuditActivityLogService::toDto)
                .toList();
    }

    private void createNotifications(Audit audit, AuditActivityType type, String details, User actor) {
        Set<User> recipients = new LinkedHashSet<>();
        if (audit.getAssignedTo() != null) {
            recipients.add(audit.getAssignedTo());
        }
        if (audit.getApplication() != null && audit.getApplication().getOwner() != null) {
            recipients.add(audit.getApplication().getOwner());
        }
        if (type == AuditActivityType.FINDING_ESCALATED || type == AuditActivityType.EXCEPTION_REQUESTED) {
            recipients.addAll(userRepository.findByRole(UserRole.ADMIN));
            recipients.addAll(userRepository.findByRole(UserRole.AUDIT_MANAGER));
        }
        if (actor != null) {
            recipients.removeIf(u -> u.getId().equals(actor.getId()));
        }
        NotificationCategory category = mapCategory(type);
        for (User recipient : recipients) {
            notificationService.createForUser(
                    recipient,
                    category,
                    type.name(),
                    details != null ? details : type.name(),
                    audit.getId()
            );
        }
    }

    private NotificationCategory mapCategory(AuditActivityType type) {
        String name = type.name();
        if (name.startsWith("FINDING_")) return NotificationCategory.FINDING;
        if (name.startsWith("EXCEPTION_")) return NotificationCategory.EXCEPTION;
        if (name.startsWith("EVIDENCE_")) return NotificationCategory.EVIDENCE;
        if (name.startsWith("AUDIT_")) return NotificationCategory.AUDIT;
        return NotificationCategory.SYSTEM;
    }
}
