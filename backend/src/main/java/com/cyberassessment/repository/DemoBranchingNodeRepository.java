package com.cyberassessment.repository;

import com.cyberassessment.entity.DemoBranchingNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemoBranchingNodeRepository extends JpaRepository<DemoBranchingNode, Long> {

    List<DemoBranchingNode> findByVersion_Id(Long versionId);
}
