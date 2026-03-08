package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "questionnaire_template_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireTemplateItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private QuestionnaireTemplate template;

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

    @Column(name = "mapping_rationale", length = 2000)
    private String mappingRationale;

    @Column(name = "mapping_weight", precision = 5, scale = 2)
    private BigDecimal mappingWeight;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;
}
