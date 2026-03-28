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
public class BranchingWorkflowGraphDto {
    private Long workflowId;
    private String workflowName;
    private String workflowDescription;
    private Long versionId;
    private String versionLabel;
    private Long startNodeId;
    private List<BranchingNodeDto> nodes;
    private List<BranchingEdgeDto> edges;
}
