package com.cyberassessment.dto;

import com.cyberassessment.entity.FindingSeverity;
import com.cyberassessment.entity.FindingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindingDto {
    private Long id;
    private Long auditId;
    private Integer auditYear;
    private String applicationName;
    private Long auditControlId;
    private String controlId;
    private String controlName;
    private String title;
    private String description;
    private FindingSeverity severity;
    private FindingStatus status;
    private Long ownerUserId;
    private String ownerEmail;
    private String ownerDisplayName;
    private Instant dueAt;
    private Instant resolvedAt;
    private Instant reminderSentAt;
    private Instant escalatedAt;
    private String slaState;
    private Instant createdAt;
    private Instant updatedAt;
    /** Number of control exception requests linked to this finding. */
    @Builder.Default
    private int linkedExceptionCount = 0;
}
