package com.cyberassessment.dto;

import com.cyberassessment.entity.AuditActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditActivityLogDto {
    private Long id;
    private Long auditId;
    private AuditActivityType activityType;
    private String details;
    private Long actorUserId;
    private String actorEmail;
    private Instant createdAt;
}
