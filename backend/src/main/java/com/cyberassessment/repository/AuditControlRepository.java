package com.cyberassessment.repository;

import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditControlRepository extends JpaRepository<AuditControl, Long> {

    List<AuditControl> findByAudit(Audit audit);

    List<AuditControl> findByAuditId(Long auditId);

    Optional<AuditControl> findByAuditIdAndControlId(Long auditId, Long controlId);
}
