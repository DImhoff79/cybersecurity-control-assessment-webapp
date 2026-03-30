package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
    /** Same mapping as control catalog / question–control studio; false = auditor review only. */
    private Boolean askOwner;
    private String existingAnswerText;
    private Long ownerAnswerOptionProfileId;
    private String ownerAnswerOptionProfileCode;
    private String ownerResponseFieldLabel;
    private String ownerResponseFieldHint;
    @Builder.Default
    private List<OwnerAnswerOptionEntryDto> ownerResponseOptions = new ArrayList<>();
}
