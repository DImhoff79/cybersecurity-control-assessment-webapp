package com.cyberassessment.repository;

import com.cyberassessment.entity.PolicyRevisionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyRevisionEventRepository extends JpaRepository<PolicyRevisionEvent, Long> {
    List<PolicyRevisionEvent> findByPolicyIdOrderByCreatedAtDesc(Long policyId);
    void deleteByPolicyId(Long policyId);
}
