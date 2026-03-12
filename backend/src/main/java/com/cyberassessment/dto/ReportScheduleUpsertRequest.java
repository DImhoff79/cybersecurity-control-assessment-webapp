package com.cyberassessment.dto;

import com.cyberassessment.entity.ReportScheduleFrequency;
import com.cyberassessment.entity.ReportScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportScheduleUpsertRequest {
    private String name;
    private ReportScheduleType reportType;
    private ReportScheduleFrequency frequency;
    private String recipientEmails;
    private String categoryFilter;
    private String searchFilter;
    private Long projectIdFilter;
    private Boolean noProjectOnly;
    private Instant firstRunAt;
    private Boolean active;
}
