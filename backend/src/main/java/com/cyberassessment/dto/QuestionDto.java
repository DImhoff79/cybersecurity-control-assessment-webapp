package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
