package com.cyberassessment.dto;

import com.cyberassessment.entity.PolicyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import com.cyberassessment.entity.NistCsfFunction;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private PolicyStatus status;
    private Long ownerUserId;
    private String ownerEmail;
    private Instant effectiveAt;
    private Instant nextReviewAt;
    private Long publishedVersionId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<NistCsfFunction> csfFunctions;
    private List<PolicyVersionDto> versions;
    private List<PolicyRevisionEventDto> revisionHistory;
}
