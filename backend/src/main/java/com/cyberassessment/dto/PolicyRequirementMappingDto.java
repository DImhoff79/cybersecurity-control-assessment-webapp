package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyRequirementMappingDto {
    private Long id;
    private Long policyId;
    private String policyCode;
    private Long requirementId;
    private String requirementCode;
    private String requirementTitle;
    private String notes;
}
