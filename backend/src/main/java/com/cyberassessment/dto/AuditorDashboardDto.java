package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditorDashboardDto {
    private ReportSummaryDto summary;
    @Builder.Default
    private List<AuditorAuditItemDto> auditsNeedingAttention = new ArrayList<>();
    @Builder.Default
    private List<AuditorEvidenceItemDto> evidenceQueue = new ArrayList<>();
}
