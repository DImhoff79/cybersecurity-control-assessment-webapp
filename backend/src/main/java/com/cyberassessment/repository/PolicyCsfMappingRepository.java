package com.cyberassessment.repository;

import com.cyberassessment.entity.PolicyCsfMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyCsfMappingRepository extends JpaRepository<PolicyCsfMapping, Long> {
    List<PolicyCsfMapping> findByPolicyIdOrderByIdAsc(Long policyId);
    void deleteByPolicyId(Long policyId);
}
