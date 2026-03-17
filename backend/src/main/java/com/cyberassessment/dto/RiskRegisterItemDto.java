package com.cyberassessment.dto;

import com.cyberassessment.entity.RiskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRegisterItemDto {
    private Long id;
    private String title;
    private String description;
    private String businessImpact;
    private Integer likelihoodScore;
    private Integer impactScore;
    private Integer inherentRiskScore;
    private Integer residualRiskScore;
    private RiskStatus status;
    private Long ownerUserId;
    private String ownerEmail;
    private Long applicationId;
    private String applicationName;
    private String otherApplicationText;
    private Instant targetCloseAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
