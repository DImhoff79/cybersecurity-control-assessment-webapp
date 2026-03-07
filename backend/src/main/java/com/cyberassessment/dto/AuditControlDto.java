package com.cyberassessment.dto;

import com.cyberassessment.entity.ControlAssessmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditControlDto {
    private Long id;
    private Long auditId;
    private Long controlId;
    private String controlControlId;
    private String controlName;
    private ControlAssessmentStatus status;
    private String evidence;
    private String notes;
    private Instant assessedAt;
    @Builder.Default
    private List<AuditControlAnswerDto> answers = new ArrayList<>();
}
