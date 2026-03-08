package com.cyberassessment.dto;

import com.cyberassessment.entity.AuditControlAssignmentRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditControlAssignmentDto {
    private Long id;
    private Long auditControlId;
    private Long userId;
    private String userEmail;
    private String userDisplayName;
    private AuditControlAssignmentRole assignmentRole;
    private Boolean active;
    private Instant assignedAt;
}
