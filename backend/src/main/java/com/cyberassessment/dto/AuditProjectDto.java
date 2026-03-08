package com.cyberassessment.dto;

import com.cyberassessment.entity.AuditProjectStatus;
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
public class AuditProjectDto {
    private Long id;
    private String name;
    private String frameworkTag;
    private Integer year;
    private String notes;
    private Instant startsAt;
    private Instant dueAt;
    private AuditProjectStatus status;
    private Long createdByUserId;
    private String createdByEmail;
    private Instant createdAt;
    @Builder.Default
    private List<ApplicationDto> scopedApplications = new ArrayList<>();
    @Builder.Default
    private List<AuditDto> audits = new ArrayList<>();
    private long totalAudits;
    private long completeAudits;
}
