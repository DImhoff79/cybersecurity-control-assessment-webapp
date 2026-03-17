package com.cyberassessment.repository;

import com.cyberassessment.entity.PolicyRequirementMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PolicyRequirementMappingRepository extends JpaRepository<PolicyRequirementMapping, Long> {
    List<PolicyRequirementMapping> findByPolicyIdOrderByIdDesc(Long policyId);

    @Query("select count(distinct m.requirement.id) from PolicyRequirementMapping m")
    long countDistinctRequirementIds();
}
