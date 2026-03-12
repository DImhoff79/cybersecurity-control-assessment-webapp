package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_evidences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_control_id", nullable = false)
    private AuditControl auditControl;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false)
    private EvidenceType evidenceType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2000)
    private String uri;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 2000)
    private String filePath;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_content")
    private byte[] fileContent;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(length = 255)
    private String source;

    @Column(length = 255)
    private String owner;

    @Column(length = 2000)
    private String notes;

    @Column(name = "collected_at")
    private Instant collectedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 40)
    @Builder.Default
    private EvidenceLifecycleStatus lifecycleStatus = EvidenceLifecycleStatus.ACTIVE;

    @Column(name = "evidence_version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "retention_until")
    private Instant retentionUntil;

    @Column(name = "legal_hold", nullable = false)
    @Builder.Default
    private Boolean legalHold = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "disposed_at")
    private Instant disposedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    @Builder.Default
    private EvidenceReviewStatus reviewStatus = EvidenceReviewStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
