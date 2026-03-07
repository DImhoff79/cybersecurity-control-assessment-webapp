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
public class AuditDto {
    private Long id;
    private Long applicationId;
    private String applicationName;
    private Integer year;
    private AuditStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private Long assignedToUserId;
    private String assignedToEmail;
    private String assignedToDisplayName;
    private Instant assignedAt;
    private Instant sentAt;
}
