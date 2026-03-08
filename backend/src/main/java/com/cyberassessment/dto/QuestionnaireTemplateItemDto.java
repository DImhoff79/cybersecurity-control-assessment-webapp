package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireTemplateItemDto {
    private Long id;
    private Long templateId;
    private Long questionId;
    private Long controlId;
    private String controlControlId;
    private String controlName;
    private String questionText;
    private String helpText;
    private Integer displayOrder;
    private Boolean askOwner;
    private String mappingRationale;
    private BigDecimal mappingWeight;
    private Instant effectiveFrom;
    private Instant effectiveTo;
}
