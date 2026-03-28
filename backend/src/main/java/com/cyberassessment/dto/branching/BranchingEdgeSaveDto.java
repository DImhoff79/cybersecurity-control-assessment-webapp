package com.cyberassessment.dto.branching;

import lombok.Data;

@Data
public class BranchingEdgeSaveDto {
    private String fromStableKey;
    private String toStableKey;
    private Integer sortOrder;
    private String conditionKind;
    private String conditionValue;
}
