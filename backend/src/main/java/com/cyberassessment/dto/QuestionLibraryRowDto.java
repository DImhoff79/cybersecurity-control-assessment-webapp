package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionLibraryRowDto {
    private Long id;
    private String questionText;
    private String helpText;
    private Boolean askOwner;
    private Integer displayOrder;
    private Long ownerAnswerOptionProfileId;
    private String ownerAnswerOptionProfileCode;
    private String ownerAnswerOptionProfileDisplayName;
    @Builder.Default
    private List<QuestionMappingRefDto> mappings = new ArrayList<>();
}
