package com.cyberassessment.dto;

import com.cyberassessment.entity.PolicyRevisionEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyRevisionEventDto {
    private Long id;
    private Long policyId;
    private Long policyVersionId;
    private PolicyRevisionEventType eventType;
    private String eventSummary;
    private Long actorUserId;
    private String actorEmail;
    private Instant createdAt;
}
