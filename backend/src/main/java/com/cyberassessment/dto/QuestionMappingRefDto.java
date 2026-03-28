package com.cyberassessment.dto;

import com.cyberassessment.entity.ControlFramework;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionMappingRefDto {
    private Long controlDbId;
    private String controlId;
    private String name;
    private String description;
    private ControlFramework framework;
    private String mappingRationale;
    private BigDecimal mappingWeight;
    private Instant effectiveFrom;
    private Instant effectiveTo;
}
