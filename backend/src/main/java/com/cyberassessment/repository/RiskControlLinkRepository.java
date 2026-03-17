package com.cyberassessment.repository;

import com.cyberassessment.entity.RiskControlLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskControlLinkRepository extends JpaRepository<RiskControlLink, Long> {
    List<RiskControlLink> findByRiskIdOrderByIdDesc(Long riskId);
}
