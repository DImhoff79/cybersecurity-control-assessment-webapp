package com.cyberassessment.dto;

import com.cyberassessment.entity.AuditAssignmentRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditAssignmentDto {
    private Long id;
    private Long auditId;
    private Long userId;
    private String userEmail;
    private String userDisplayName;
    private AuditAssignmentRole assignmentRole;
    private Boolean active;
    private Instant assignedAt;
}
