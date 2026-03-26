package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_approval_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id", nullable = false)
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_status", nullable = false, length = 30)
    @Builder.Default
    private AuditApprovalStepStatus stepStatus = AuditApprovalStepStatus.PENDING;

    @Column(name = "decision_notes", length = 4000)
    private String decisionNotes;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
