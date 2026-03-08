package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditControlAssignment;
import com.cyberassessment.entity.AuditControlAssignmentRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditControlAssignmentRepository extends JpaRepository<AuditControlAssignment, Long> {
    List<AuditControlAssignment> findByAuditControlIdAndActiveTrue(Long auditControlId);
    List<AuditControlAssignment> findByUserIdAndActiveTrue(Long userId);
    boolean existsByAuditControlIdAndUserIdAndActiveTrue(Long auditControlId, Long userId);
    Optional<AuditControlAssignment> findByAuditControlIdAndUserIdAndAssignmentRole(Long auditControlId, Long userId, AuditControlAssignmentRole role);
}
