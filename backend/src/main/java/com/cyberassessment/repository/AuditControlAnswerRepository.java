package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditControl;
import com.cyberassessment.entity.AuditControlAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditControlAnswerRepository extends JpaRepository<AuditControlAnswer, Long> {

    List<AuditControlAnswer> findByAuditControl(AuditControl auditControl);

    List<AuditControlAnswer> findByAuditControlId(Long auditControlId);

    Optional<AuditControlAnswer> findByAuditControlIdAndQuestionId(Long auditControlId, Long questionId);
}
