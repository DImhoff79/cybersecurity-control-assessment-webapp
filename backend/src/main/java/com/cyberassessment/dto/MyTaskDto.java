package com.cyberassessment.dto;

import com.cyberassessment.entity.AuditControlAssignmentRole;
import com.cyberassessment.entity.ControlAssessmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyTaskDto {
    private Long assignmentId;
    private Long auditId;
    private Long auditControlId;
    private String applicationName;
    private Integer auditYear;
    private String controlControlId;
    private String controlName;
    private ControlAssessmentStatus status;
    private String notes;
    private AuditControlAssignmentRole assignmentRole;
    private Instant dueAt;
}
