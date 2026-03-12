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
public class ReportScheduleDto {
    private Long id;
    private String name;
    private ReportScheduleType reportType;
    private ReportScheduleFrequency frequency;
    private String recipientEmails;
    private String categoryFilter;
    private String searchFilter;
    private Long projectIdFilter;
    private boolean noProjectOnly;
    private Instant nextRunAt;
    private boolean active;
    private Long createdByUserId;
    private String createdByEmail;
    private Instant lastRunAt;
    private String lastRunStatus;
    private String lastRunMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
