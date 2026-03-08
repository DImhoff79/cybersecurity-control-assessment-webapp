package com.cyberassessment.dto;

import com.cyberassessment.entity.EvidenceReviewStatus;
import com.cyberassessment.entity.EvidenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditorEvidenceItemDto {
    private Long evidenceId;
    private Long auditId;
    private Long auditControlId;
    private String applicationName;
    private Integer year;
    private String controlControlId;
    private String controlName;
    private String framework;
    private String title;
    private EvidenceType evidenceType;
    private EvidenceReviewStatus reviewStatus;
    private String uri;
    private String fileName;
    private String notes;
    private Instant createdAt;
}
