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
public class QuestionDto {
    private Long id;
    private Long controlId;
    private String questionText;
    private Integer displayOrder;
    private String helpText;
    private Boolean askOwner;
    private String mappingRationale;
    private BigDecimal mappingWeight;
    private Instant effectiveFrom;
    private Instant effectiveTo;
}
