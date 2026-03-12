package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditActivityLogRepository extends JpaRepository<AuditActivityLog, Long> {

    List<AuditActivityLog> findByAuditIdOrderByCreatedAtDesc(Long auditId);

    List<AuditActivityLog> findTop200ByOrderByCreatedAtDesc();
}
