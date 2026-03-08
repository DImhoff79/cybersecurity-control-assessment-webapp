package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_questionnaire_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditQuestionnaireItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private AuditQuestionnaireSnapshot snapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_control_id", nullable = false)
    private AuditControl auditControl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_id", nullable = false)
    private Control control;

    @Column(name = "question_text", nullable = false, length = 2000)
    private String questionText;

    @Column(name = "help_text", length = 2000)
    private String helpText;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "ask_owner", nullable = false)
    private Boolean askOwner;
}
