package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditApprovalStep;
import com.cyberassessment.entity.AuditApprovalStepStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditApprovalStepRepository extends JpaRepository<AuditApprovalStep, Long> {
    List<AuditApprovalStep> findByAuditIdOrderByStepOrderAsc(Long auditId);

    long countByAuditId(Long auditId);

    void deleteByAudit_Id(Long auditId);

    List<AuditApprovalStep> findByAuditIdAndStepStatusOrderByStepOrderAsc(Long auditId, AuditApprovalStepStatus status);

    boolean existsByAuditIdAndAssignedTo_Id(Long auditId, Long userId);
}
