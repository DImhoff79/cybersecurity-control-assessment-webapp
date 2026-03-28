package com.cyberassessment.dto.branching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchingNodeDto {
    private Long id;
    private String stableKey;
    private String title;
    private String body;
    private String questionType;
    private List<BranchingChoiceDto> choices;
    private Integer posX;
    private Integer posY;
}
