package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditQuestionnaireItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditQuestionnaireItemRepository extends JpaRepository<AuditQuestionnaireItem, Long> {
}
