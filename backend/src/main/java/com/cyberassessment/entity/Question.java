package com.cyberassessment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable: library-only questions use mappings only; legacy rows may still set a primary control. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_id", nullable = true)
    private Control control;

    @NotBlank
    @Column(name = "question_text", nullable = false, length = 2000)
    private String questionText;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "help_text", length = 2000)
    private String helpText;

    @Column(name = "ask_owner", nullable = false)
    @Builder.Default
    private Boolean askOwner = true;
}
