package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditControl;
import com.cyberassessment.entity.AuditControlAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuditControlAnswerRepository extends JpaRepository<AuditControlAnswer, Long> {

    List<AuditControlAnswer> findByAuditControl(AuditControl auditControl);

    List<AuditControlAnswer> findByAuditControlId(Long auditControlId);

    Optional<AuditControlAnswer> findByAuditControlIdAndQuestionId(Long auditControlId, Long questionId);

    @Query("""
            select a.auditControl.audit.id, count(distinct a.question.id)
            from AuditControlAnswer a
            where a.auditControl.audit.id in :auditIds
              and trim(coalesce(a.answerText, '')) <> ''
            group by a.auditControl.audit.id
            """)
    List<Object[]> countDistinctAnsweredQuestionsByAuditIds(@Param("auditIds") List<Long> auditIds);
}
