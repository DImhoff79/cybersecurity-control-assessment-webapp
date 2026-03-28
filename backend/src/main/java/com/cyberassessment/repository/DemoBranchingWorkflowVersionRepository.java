package com.cyberassessment.repository;

import com.cyberassessment.entity.DemoBranchingWorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DemoBranchingWorkflowVersionRepository extends JpaRepository<DemoBranchingWorkflowVersion, Long> {

    List<DemoBranchingWorkflowVersion> findByWorkflowIdOrderByIdDesc(Long workflowId);

    Optional<DemoBranchingWorkflowVersion> findFirstByWorkflowIdOrderByIdDesc(Long workflowId);
}
