package com.cyberassessment.repository;

import com.cyberassessment.entity.RemediationAction;
import com.cyberassessment.entity.RemediationActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface RemediationActionRepository extends JpaRepository<RemediationAction, Long> {
    List<RemediationAction> findByPlanIdOrderBySequenceNoAscIdAsc(Long planId);
    long countByStatusAndDueAtBefore(RemediationActionStatus status, Instant dueAt);
}
