package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditQuestionnaireSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuditQuestionnaireSnapshotRepository extends JpaRepository<AuditQuestionnaireSnapshot, Long> {

    Optional<AuditQuestionnaireSnapshot> findByAuditId(Long auditId);
}
