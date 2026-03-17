package com.cyberassessment.dto;

import com.cyberassessment.entity.RemediationPlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationPlanDto {
    private Long id;
    private Long riskId;
    private String riskTitle;
    private String title;
    private RemediationPlanStatus status;
    private Instant targetCompleteAt;
    private Long createdByUserId;
    private String createdByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
