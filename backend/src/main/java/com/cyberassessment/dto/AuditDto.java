package com.cyberassessment.dto;

import com.cyberassessment.entity.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditDto {
    private Long id;
    private Long applicationId;
    private String applicationName;
    private Long projectId;
    private String projectName;
    private Integer year;
    private AuditStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private Long assignedToUserId;
    private String assignedToEmail;
    private String assignedToDisplayName;
    private Instant assignedAt;
    private Instant sentAt;
    private Instant dueAt;
    private Instant reminderSentAt;
    private Instant escalatedAt;
    private Instant attestedAt;
    private Long attestedByUserId;
    private String attestedByEmail;
    private String attestationStatement;
    private Integer totalQuestions;
    private Integer completedQuestions;
    private Integer totalAdditionalControls;
    private Integer completedAdditionalControls;
    private Integer completionPct;
    @Builder.Default
    private List<AuditAssignmentDto> assignments = new ArrayList<>();
}
