package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementControlMappingDto {
    private Long id;
    private Long requirementId;
    private Long controlId;
    private String controlControlId;
    private String controlName;
    private Integer coveragePct;
    private String notes;
}
