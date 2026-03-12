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
public class FindingUpsertRequest {
    private Long auditId;
    private Long auditControlId;
    private String title;
    private String description;
    private FindingSeverity severity;
    private FindingStatus status;
    private Long ownerUserId;
    private Instant dueAt;
}
