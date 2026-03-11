package com.cyberassessment.repository;

import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditControl;
import com.cyberassessment.entity.ControlAssessmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuditControlRepository extends JpaRepository<AuditControl, Long> {

    List<AuditControl> findByAudit(Audit audit);

    List<AuditControl> findByAuditId(Long auditId);

    Optional<AuditControl> findByAuditIdAndControlId(Long auditId, Long controlId);

    @Query("""
            select ac.audit.id, count(ac.id)
            from AuditControl ac
            where ac.audit.id in :auditIds
              and not exists (
                select 1 from AuditQuestionnaireItem i
                where i.auditControl.id = ac.id and i.askOwner = true
              )
            group by ac.audit.id
            """)
    List<Object[]> countAdditionalControlsByAuditIds(@Param("auditIds") List<Long> auditIds);

    @Query("""
            select ac.audit.id, count(ac.id)
            from AuditControl ac
            where ac.audit.id in :auditIds
              and ac.status in :completeStatuses
              and not exists (
                select 1 from AuditQuestionnaireItem i
                where i.auditControl.id = ac.id and i.askOwner = true
              )
            group by ac.audit.id
            """)
    List<Object[]> countCompletedAdditionalControlsByAuditIds(
            @Param("auditIds") List<Long> auditIds,
            @Param("completeStatuses") List<ControlAssessmentStatus> completeStatuses
    );
}
