package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntakeWizardStepDto {
    private Long questionId;
    private String stepKey;
    private String inputType;
    private String questionText;
    private String helpText;
    /** JSON array of {id,label} for CHOICE */
    private String choicesJson;
    private Integer displayOrder;
}
