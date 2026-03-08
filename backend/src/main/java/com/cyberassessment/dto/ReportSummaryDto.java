package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSummaryDto {
    private long totalApplications;
    private long totalAudits;
    private long openAudits;
    private long overdueAudits;
    private long submittedAudits;
    private long attestedAudits;
    private long completedAudits;
}
