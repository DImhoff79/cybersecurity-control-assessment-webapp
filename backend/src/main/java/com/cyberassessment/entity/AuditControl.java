package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_controls", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "audit_id", "control_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_id", nullable = false)
    private Control control;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ControlAssessmentStatus status = ControlAssessmentStatus.NOT_STARTED;

    @Column(length = 4000)
    private String evidence;

    @Column(length = 2000)
    private String notes;

    @Column(name = "assessed_at")
    private Instant assessedAt;

    @OneToMany(mappedBy = "auditControl", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuditControlAnswer> answers = new ArrayList<>();
}
