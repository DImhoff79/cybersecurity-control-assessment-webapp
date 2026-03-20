package com.cyberassessment.repository;

import com.cyberassessment.entity.RemediationPlan;
import com.cyberassessment.entity.RemediationPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RemediationPlanRepository extends JpaRepository<RemediationPlan, Long> {
    long countByStatus(RemediationPlanStatus status);

    long countByTitleStartingWith(String prefix);
}
