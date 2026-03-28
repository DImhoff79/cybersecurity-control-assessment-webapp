package com.cyberassessment.service;

import com.cyberassessment.dto.branching.BranchingResolveRequest;
import com.cyberassessment.dto.branching.BranchingResolveResponse;
import com.cyberassessment.dto.branching.BranchingWorkflowGraphDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo_branching_workflow_it;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class DemoBranchingWorkflowServiceIntegrationTest {

    @Autowired
    private DemoBranchingWorkflowService demoBranchingWorkflowService;

    @Test
    void getGraphResolvesLatestVersionAndReturnsSeededNodes() {
        BranchingWorkflowGraphDto g = demoBranchingWorkflowService.getGraph(null);
        assertThat(g.getVersionId()).isEqualTo(1L);
        assertThat(g.getStartNodeId()).isEqualTo(1L);
        assertThat(g.getNodes()).hasSize(4);
        assertThat(g.getEdges()).hasSize(7);
    }

    @Test
    void resolveYesFromStartGoesToDescribeScope() {
        BranchingResolveRequest req = new BranchingResolveRequest();
        req.setVersionId(1L);
        req.setFromNodeId(1L);
        req.setValue("yes");
        BranchingResolveResponse r = demoBranchingWorkflowService.resolve(req);
        assertThat(r.isFinished()).isFalse();
        assertThat(r.getNextNode()).isNotNull();
        assertThat(r.getNextNode().getStableKey()).isEqualTo("describe_scope");
    }

    @Test
    void resolveEndNodeMarksFinished() {
        BranchingResolveRequest req = new BranchingResolveRequest();
        req.setVersionId(1L);
        req.setFromNodeId(4L);
        req.setValue("x");
        BranchingResolveResponse r = demoBranchingWorkflowService.resolve(req);
        assertThat(r.isFinished()).isTrue();
    }
}
