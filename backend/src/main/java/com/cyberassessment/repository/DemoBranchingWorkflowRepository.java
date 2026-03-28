package com.cyberassessment.repository;

import com.cyberassessment.entity.DemoBranchingWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DemoBranchingWorkflowRepository extends JpaRepository<DemoBranchingWorkflow, Long> {

    Optional<DemoBranchingWorkflow> findFirstByOrderByIdAsc();
}
