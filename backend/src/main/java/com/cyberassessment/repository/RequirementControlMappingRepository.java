package com.cyberassessment.repository;

import com.cyberassessment.entity.RequirementControlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RequirementControlMappingRepository extends JpaRepository<RequirementControlMapping, Long> {
    List<RequirementControlMapping> findByRequirementIdOrderByIdDesc(Long requirementId);

    @Query("select count(distinct m.requirement.id) from RequirementControlMapping m")
    long countDistinctRequirementIds();
}
