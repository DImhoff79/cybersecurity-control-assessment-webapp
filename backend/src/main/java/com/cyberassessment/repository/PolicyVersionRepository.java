package com.cyberassessment.repository;

import com.cyberassessment.entity.PolicyVersion;
import com.cyberassessment.entity.PolicyVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyVersionRepository extends JpaRepository<PolicyVersion, Long> {
    List<PolicyVersion> findByPolicyIdOrderByVersionNumberDesc(Long policyId);
    long countByPolicyId(Long policyId);
    List<PolicyVersion> findByPolicyIdAndStatus(Long policyId, PolicyVersionStatus status);
    void deleteByPolicyId(Long policyId);
}
