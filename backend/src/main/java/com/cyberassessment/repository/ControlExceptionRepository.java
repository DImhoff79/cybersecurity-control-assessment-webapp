package com.cyberassessment.repository;

import com.cyberassessment.entity.ControlExceptionRequest;
import com.cyberassessment.entity.ControlExceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ControlExceptionRepository extends JpaRepository<ControlExceptionRequest, Long> {
    List<ControlExceptionRequest> findAllByOrderByRequestedAtDesc();
    List<ControlExceptionRequest> findByAuditIdOrderByRequestedAtDesc(Long auditId);
    List<ControlExceptionRequest> findByStatusAndExpiresAtBefore(ControlExceptionStatus status, Instant now);
}
