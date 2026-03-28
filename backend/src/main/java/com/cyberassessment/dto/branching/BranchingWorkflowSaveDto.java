package com.cyberassessment.dto.branching;

import lombok.Data;

import java.util.List;

@Data
public class BranchingWorkflowSaveDto {
    private Long versionId;
    private String startStableKey;
    private List<BranchingNodeSaveDto> nodes;
    private List<BranchingEdgeSaveDto> edges;
}
