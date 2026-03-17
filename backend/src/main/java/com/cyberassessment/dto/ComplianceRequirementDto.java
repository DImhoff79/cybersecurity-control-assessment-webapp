package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceRequirementDto {
    private Long id;
    private Long regulationId;
    private String regulationCode;
    private String requirementCode;
    private String title;
    private String description;
    private Boolean active;
}
