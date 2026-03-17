package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskControlLinkDto {
    private Long id;
    private Long riskId;
    private Long controlId;
    private String controlControlId;
    private String controlName;
    private String notes;
}
