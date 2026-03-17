package com.cyberassessment.repository;

import com.cyberassessment.entity.ComplianceRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplianceRequirementRepository extends JpaRepository<ComplianceRequirement, Long> {
    List<ComplianceRequirement> findByRegulationIdOrderByRequirementCodeAsc(Long regulationId);
}
