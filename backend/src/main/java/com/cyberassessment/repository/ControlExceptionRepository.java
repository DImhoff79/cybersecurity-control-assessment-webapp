package com.cyberassessment.repository;

import com.cyberassessment.entity.ControlExceptionRequest;
import com.cyberassessment.entity.ControlExceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ControlExceptionRepository extends JpaRepository<ControlExceptionRequest, Long> {
    List<ControlExceptionRequest> findAllByOrderByRequestedAtDesc();
    List<ControlExceptionRequest> findByAuditIdOrderByRequestedAtDesc(Long auditId);
    List<ControlExceptionRequest> findByAuditIdAndFinding_IdOrderByRequestedAtDesc(Long auditId, Long findingId);
    List<ControlExceptionRequest> findByFinding_IdOrderByRequestedAtDesc(Long findingId);
    List<ControlExceptionRequest> findByStatusAndExpiresAtBefore(ControlExceptionStatus status, Instant now);

    long countByFinding_Id(Long findingId);

    @Query("SELECT e.finding.id, COUNT(e) FROM ControlExceptionRequest e WHERE e.finding IS NOT NULL AND e.finding.id IN :ids GROUP BY e.finding.id")
    List<Object[]> countGroupedByFindingId(@Param("ids") Collection<Long> ids);

    long countByReasonContaining(String fragment);
}
