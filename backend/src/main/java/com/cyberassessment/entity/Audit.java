package com.cyberassessment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audits", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "application_id", "audit_year" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_project_id")
    private AuditProject auditProject;

    @NotNull
    @Column(name = "audit_year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuditStatus status = AuditStatus.DRAFT;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "attested_at")
    private Instant attestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attested_by_user_id")
    private User attestedBy;

    @Column(name = "attestation_statement", length = 2000)
    private String attestationStatement;

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuditControl> auditControls = new ArrayList<>();

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuditAssignment> assignments = new ArrayList<>();
}
