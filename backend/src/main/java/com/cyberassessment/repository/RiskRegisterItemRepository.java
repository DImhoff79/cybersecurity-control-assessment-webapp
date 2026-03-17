package com.cyberassessment.repository;

import com.cyberassessment.entity.RiskRegisterItem;
import com.cyberassessment.entity.RiskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskRegisterItemRepository extends JpaRepository<RiskRegisterItem, Long> {
    List<RiskRegisterItem> findAllByOrderByUpdatedAtDesc();
    long countByStatusIn(List<RiskStatus> statuses);
    long countByResidualRiskScoreGreaterThanEqual(Integer minResidualRiskScore);
}
