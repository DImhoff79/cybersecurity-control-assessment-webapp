package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "remediation_action_dependencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationActionDependency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private RemediationPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_action_id", nullable = false)
    private RemediationAction predecessorAction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "successor_action_id", nullable = false)
    private RemediationAction successorAction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
