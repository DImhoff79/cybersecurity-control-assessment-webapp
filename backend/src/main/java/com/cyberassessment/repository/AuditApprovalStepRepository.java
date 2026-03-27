package com.cyberassessment.repository;

import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditApprovalStep;
import com.cyberassessment.entity.AuditApprovalStepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditApprovalStepRepository extends JpaRepository<AuditApprovalStep, Long> {
    List<AuditApprovalStep> findByAuditIdOrderByStepOrderAsc(Long auditId);

    long countByAuditId(Long auditId);

    void deleteByAudit_Id(Long auditId);

    List<AuditApprovalStep> findByAuditIdAndStepStatusOrderByStepOrderAsc(Long auditId, AuditApprovalStepStatus status);

    boolean existsByAuditIdAndAssignedTo_Id(Long auditId, Long userId);

    @Query("SELECT DISTINCT s.audit FROM AuditApprovalStep s WHERE s.assignedTo.id = :userId")
    List<Audit> findDistinctAuditsByAssignedToUserId(@Param("userId") Long userId);
}
