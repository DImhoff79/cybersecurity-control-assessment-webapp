package com.cyberassessment.repository;

import com.cyberassessment.entity.PolicyAcknowledgement;
import com.cyberassessment.entity.PolicyAcknowledgementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PolicyAcknowledgementRepository extends JpaRepository<PolicyAcknowledgement, Long> {
    List<PolicyAcknowledgement> findByUserIdOrderByAssignedAtDesc(Long userId);
    List<PolicyAcknowledgement> findByPolicyIdOrderByAssignedAtDesc(Long policyId);
    void deleteByPolicyId(Long policyId);
    Optional<PolicyAcknowledgement> findByIdAndUserId(Long id, Long userId);
    long countByStatus(PolicyAcknowledgementStatus status);
    long countByStatusAndDueAtBefore(PolicyAcknowledgementStatus status, Instant dueAt);

    @Query("select count(distinct a.user.id) from PolicyAcknowledgement a where a.policy.id = :policyId")
    long countDistinctUsersByPolicyId(@Param("policyId") Long policyId);
}
