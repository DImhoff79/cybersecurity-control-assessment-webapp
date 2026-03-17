package com.cyberassessment.dto;

import com.cyberassessment.entity.PolicyAcknowledgementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyAcknowledgementDto {
    private Long id;
    private Long policyId;
    private String policyCode;
    private String policyName;
    private Long policyVersionId;
    private Integer policyVersionNumber;
    private String policyVersionTitle;
    private Long userId;
    private String userEmail;
    private PolicyAcknowledgementStatus status;
    private Instant dueAt;
    private Instant assignedAt;
    private Instant acknowledgedAt;
}
