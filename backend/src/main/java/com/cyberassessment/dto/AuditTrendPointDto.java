package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditTrendPointDto {
    private Integer year;
    private long total;
    private long open;
    private long overdue;
    private long submitted;
    private long attested;
    private long complete;
    private double completionRatePct;
    private double overdueRatePct;
}
