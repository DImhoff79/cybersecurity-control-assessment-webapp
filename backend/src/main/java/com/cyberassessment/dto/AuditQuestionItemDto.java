package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditQuestionItemDto {
    private Long questionId;
    private Long auditControlId;
    private Long controlId;
    private String controlControlId;
    private String controlName;
    private String questionText;
    private String helpText;
    private Integer displayOrder;
    private String existingAnswerText;
}
