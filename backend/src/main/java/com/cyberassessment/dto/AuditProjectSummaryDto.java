package com.cyberassessment.dto;

import com.cyberassessment.entity.AuditProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditProjectSummaryDto {
    private Long projectId;
    private String projectName;
    private String frameworkTag;
    private Integer year;
    private AuditProjectStatus status;
    private long scopedApplications;
    private long totalAudits;
    private long openAudits;
    private long submittedAudits;
    private long attestedAudits;
    private long completeAudits;
    private Instant createdAt;
}
