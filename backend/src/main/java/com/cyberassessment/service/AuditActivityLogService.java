package com.cyberassessment.service;

import com.cyberassessment.dto.AuditActivityLogDto;
import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditActivityLog;
import com.cyberassessment.entity.AuditActivityType;
import com.cyberassessment.entity.User;
import com.cyberassessment.repository.AuditActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditActivityLogService {

    private final AuditActivityLogRepository auditActivityLogRepository;
    private final CurrentUserService currentUserService;

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
    }

    @Transactional(readOnly = true)
    public List<AuditActivityLogDto> listByAuditId(Long auditId) {
        return auditActivityLogRepository.findByAuditIdOrderByCreatedAtDesc(auditId)
                .stream()
                .map(AuditActivityLogService::toDto)
                .toList();
    }
}
