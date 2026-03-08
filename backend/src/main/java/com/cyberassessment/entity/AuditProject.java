package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "framework_tag", length = 100)
    private String frameworkTag;

    @Column(name = "project_year", nullable = false)
    private Integer year;

    @Column(length = 2000)
    private String notes;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuditProjectStatus status = AuditProjectStatus.ACTIVE;

    @ManyToMany
    @JoinTable(
            name = "audit_project_applications",
            joinColumns = @JoinColumn(name = "audit_project_id"),
            inverseJoinColumns = @JoinColumn(name = "application_id")
    )
    @Builder.Default
    private List<Application> applications = new ArrayList<>();

    @OneToMany(mappedBy = "auditProject")
    @Builder.Default
    private List<Audit> audits = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
