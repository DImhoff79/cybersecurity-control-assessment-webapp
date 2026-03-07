package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditControlAnswerDto {
    private Long id;
    private Long auditControlId;
    private Long questionId;
    private String questionText;
    private String answerText;
    private Instant answeredAt;
}
