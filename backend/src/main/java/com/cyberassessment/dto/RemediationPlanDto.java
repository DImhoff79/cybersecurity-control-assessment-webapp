package com.cyberassessment.dto;

import com.cyberassessment.entity.RemediationPlanStatus;
import com.cyberassessment.entity.RemediationPlanApprovalStatus;
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
    private String proposedPlan;
    private String timeframeText;
    private String compensatingControls;
    private String planRationale;
    private RemediationPlanStatus status;
    private RemediationPlanApprovalStatus approvalStatus;
    private String approvalNotes;
    private Long approvedByUserId;
    private String approvedByEmail;
    private Instant approvedAt;
    private Instant targetCompleteAt;
    private Long createdByUserId;
    private String createdByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
