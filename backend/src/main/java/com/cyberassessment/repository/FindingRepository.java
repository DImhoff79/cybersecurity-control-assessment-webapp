package com.cyberassessment.repository;

import com.cyberassessment.entity.Finding;
import com.cyberassessment.entity.FindingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface FindingRepository extends JpaRepository<Finding, Long> {
    List<Finding> findAllByOrderByDueAtAscCreatedAtDesc();
    List<Finding> findByAuditIdOrderByDueAtAscCreatedAtDesc(Long auditId);

    long countByAudit_Id(Long auditId);
    List<Finding> findByStatusInAndDueAtBefore(List<FindingStatus> statuses, Instant dueAtBefore);
}
