package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskKpiDto {
    private long openRisks;
    private long highRisks;
    private long overdueRemediationActions;
    private long plansInProgress;
}
