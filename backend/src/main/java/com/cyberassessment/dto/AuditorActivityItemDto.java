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
public class AuditorActivityItemDto {
    private Long id;
    private Long auditId;
    private Long projectId;
    private String projectName;
    private String applicationName;
    private Integer year;
    private AuditActivityType activityType;
    private String details;
    private String actorEmail;
    private Instant createdAt;
}
