package com.cyberassessment.repository;

import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.Finding;
import com.cyberassessment.entity.FindingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface FindingRepository extends JpaRepository<Finding, Long> {
    List<Finding> findAllByOrderByDueAtAscCreatedAtDesc();
    List<Finding> findByAuditIdOrderByDueAtAscCreatedAtDesc(Long auditId);

    long countByAudit_Id(Long auditId);
    List<Finding> findByStatusInAndDueAtBefore(List<FindingStatus> statuses, Instant dueAtBefore);

    boolean existsByAudit_IdAndOwner_Id(Long auditId, Long ownerId);

    List<Finding> findByAudit_IdAndOwner_Id(Long auditId, Long ownerId);

    @Query("SELECT DISTINCT f.audit FROM Finding f WHERE f.owner.id = :userId")
    List<Audit> findDistinctAuditsByOwnerUserId(@Param("userId") Long userId);
}
