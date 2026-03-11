package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditQuestionnaireItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditQuestionnaireItemRepository extends JpaRepository<AuditQuestionnaireItem, Long> {

    List<AuditQuestionnaireItem> findBySnapshotIdOrderByDisplayOrderAscControlControlIdAsc(Long snapshotId);

    List<AuditQuestionnaireItem> findByAuditControlIdAndAskOwnerTrue(Long auditControlId);

    @Query("""
            select i.auditControl.audit.id, count(distinct i.question.id)
            from AuditQuestionnaireItem i
            where i.askOwner = true and i.auditControl.audit.id in :auditIds
            group by i.auditControl.audit.id
            """)
    List<Object[]> countDistinctOwnerQuestionsByAuditIds(@Param("auditIds") List<Long> auditIds);
}
