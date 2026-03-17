package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "risk_register_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRegisterItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(name = "business_impact", length = 3000)
    private String businessImpact;

    @Column(name = "likelihood_score", nullable = false)
    private Integer likelihoodScore;

    @Column(name = "impact_score", nullable = false)
    private Integer impactScore;

    @Column(name = "inherent_risk_score", nullable = false)
    private Integer inherentRiskScore;

    @Column(name = "residual_risk_score")
    private Integer residualRiskScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

    @Column(name = "other_application_text", length = 500)
    private String otherApplicationText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RiskStatus status = RiskStatus.OPEN;

    @Column(name = "target_close_at")
    private Instant targetCloseAt;

    @Column(name = "closed_at")
    private Instant closedAt;

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
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
