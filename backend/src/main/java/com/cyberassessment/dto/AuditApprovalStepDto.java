package com.cyberassessment.dto;

import com.cyberassessment.entity.AuditApprovalStepStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditApprovalStepDto {
    private Long id;
    private Integer stepOrder;
    private Long assignedUserId;
    private String assignedUserEmail;
    private String assignedUserDisplayName;
    private AuditApprovalStepStatus stepStatus;
    private String decisionNotes;
    private Instant decidedAt;
}
