package com.cyberassessment.dto;

import com.cyberassessment.entity.ControlAssessmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAuditControlUpdateItemDto {
    private Long auditControlId;
    private ControlAssessmentStatus status;
    private String notes;
}
