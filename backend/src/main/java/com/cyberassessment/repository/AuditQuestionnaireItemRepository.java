package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditQuestionnaireItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditQuestionnaireItemRepository extends JpaRepository<AuditQuestionnaireItem, Long> {

    List<AuditQuestionnaireItem> findBySnapshotIdOrderByDisplayOrderAscControlControlIdAsc(Long snapshotId);

    List<AuditQuestionnaireItem> findByAuditControlIdAndAskOwnerTrue(Long auditControlId);
}
