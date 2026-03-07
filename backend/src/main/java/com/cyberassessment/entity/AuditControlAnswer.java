package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_control_answers", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "audit_control_id", "question_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditControlAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_control_id", nullable = false)
    private AuditControl auditControl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "answer_text", length = 4000)
    private String answerText;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;
}
