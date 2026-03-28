package com.cyberassessment.dto.branching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchingResolveResponse {
    private BranchingNodeDto nextNode;
    private boolean finished;
}
