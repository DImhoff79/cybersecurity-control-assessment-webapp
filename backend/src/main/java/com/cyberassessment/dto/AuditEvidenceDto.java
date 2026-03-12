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
public class AuditEvidenceDto {
    private Long id;
    private Long auditControlId;
    private EvidenceType evidenceType;
    private String title;
    private String uri;
    private String fileName;
    private String mimeType;
    private Long sizeBytes;
    private String source;
    private String owner;
    private String notes;
    private Instant collectedAt;
    private Instant expiresAt;
    private EvidenceReviewStatus reviewStatus;
    private Long reviewedByUserId;
    private String reviewedByEmail;
    private Instant reviewedAt;
    private Long createdByUserId;
    private String createdByEmail;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean stale;
}
