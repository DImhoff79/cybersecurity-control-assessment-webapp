package com.cyberassessment.dto;

import com.cyberassessment.entity.RemediationActionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationActionDto {
    private Long id;
    private Long planId;
    private String actionTitle;
    private String actionDetail;
    private Long ownerUserId;
    private String ownerEmail;
    private Instant dueAt;
    private RemediationActionStatus status;
    private Integer sequenceNo;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
