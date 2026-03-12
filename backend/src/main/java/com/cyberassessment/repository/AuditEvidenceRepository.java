package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditEvidence;
import com.cyberassessment.entity.EvidenceReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AuditEvidenceRepository extends JpaRepository<AuditEvidence, Long> {

    List<AuditEvidence> findByAuditControlId(Long auditControlId);

    List<AuditEvidence> findByReviewStatusOrderByCreatedAtDesc(EvidenceReviewStatus reviewStatus);

    List<AuditEvidence> findByExpiresAtBeforeAndReviewStatusIn(Instant expiresAt, List<EvidenceReviewStatus> statuses);

    List<AuditEvidence> findByExpiresAtBefore(Instant expiresAt);
}
