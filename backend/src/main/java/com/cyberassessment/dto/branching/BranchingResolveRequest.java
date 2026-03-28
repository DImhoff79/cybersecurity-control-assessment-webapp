package com.cyberassessment.dto.branching;

import lombok.Data;

@Data
public class BranchingResolveRequest {
    private Long versionId;
    private Long fromNodeId;
    private String value;
}
