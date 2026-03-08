package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditYearSummaryDto {
    private Integer year;
    private long total;
    private long draft;
    private long inProgress;
    private long submitted;
    private long attested;
    private long complete;
}
