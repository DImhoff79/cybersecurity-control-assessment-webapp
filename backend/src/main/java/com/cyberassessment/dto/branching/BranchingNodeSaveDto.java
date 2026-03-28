package com.cyberassessment.dto.branching;

import lombok.Data;

@Data
public class BranchingNodeSaveDto {
    private String stableKey;
    private String title;
    private String body;
    private String questionType;
    private String choicesJson;
    private Integer posX;
    private Integer posY;
}
