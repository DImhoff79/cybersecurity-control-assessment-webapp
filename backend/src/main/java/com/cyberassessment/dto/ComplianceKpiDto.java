package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceKpiDto {
    private long totalPolicies;
    private long activePolicies;
    private long totalRequirements;
    private long mappedRequirementsToControls;
    private long mappedRequirementsToPolicies;
    private double controlCoveragePct;
    private double policyCoveragePct;
    private long pendingAttestations;
    private long overdueAttestations;
}
