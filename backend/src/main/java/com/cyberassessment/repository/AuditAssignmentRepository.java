package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditAssignment;
import com.cyberassessment.entity.AuditAssignmentRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditAssignmentRepository extends JpaRepository<AuditAssignment, Long> {
    List<AuditAssignment> findByAuditIdAndActiveTrue(Long auditId);
    List<AuditAssignment> findByUserIdAndActiveTrue(Long userId);
    boolean existsByAuditIdAndUserIdAndActiveTrue(Long auditId, Long userId);
    Optional<AuditAssignment> findByAuditIdAndUserIdAndAssignmentRole(Long auditId, Long userId, AuditAssignmentRole role);
    List<AuditAssignment> findByAuditIdAndAssignmentRole(Long auditId, AuditAssignmentRole role);
}
