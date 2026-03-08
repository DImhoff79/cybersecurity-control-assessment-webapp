package com.cyberassessment.dto;

import com.cyberassessment.entity.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditorAuditItemDto {
    private Long auditId;
    private String applicationName;
    private Integer year;
    private AuditStatus status;
    private String assignedToEmail;
    private Instant dueAt;
    private String frameworks;
    private long pendingEvidenceCount;
}
