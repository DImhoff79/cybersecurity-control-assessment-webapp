package com.cyberassessment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "criticality")
    private ApplicationCriticality criticality;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_classification")
    private DataClassification dataClassification;

    @Column(name = "regulatory_scope", length = 500)
    private String regulatoryScope;

    @Column(name = "business_owner_name")
    private String businessOwnerName;

    @Column(name = "technical_owner_name")
    private String technicalOwnerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status")
    private ApplicationLifecycleStatus lifecycleStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Audit> audits = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
