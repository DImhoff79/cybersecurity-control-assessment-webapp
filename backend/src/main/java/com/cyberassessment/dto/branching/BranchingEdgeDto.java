package com.cyberassessment.dto.branching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchingEdgeDto {
    private Long id;
    private Long fromNodeId;
    private Long toNodeId;
    private Integer sortOrder;
    private String conditionKind;
    private String conditionValue;
}
