package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "demo_branching_node")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoBranchingNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id")
    private DemoBranchingWorkflowVersion version;

    @Column(name = "stable_key", nullable = false, length = 64)
    private String stableKey;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 4000)
    private String body;

    @Column(name = "question_type", nullable = false, length = 24)
    private String questionType;

    @Column(name = "choices_json", length = 4000)
    private String choicesJson;

    @Column(name = "pos_x", nullable = false)
    @Builder.Default
    private Integer posX = 0;

    @Column(name = "pos_y", nullable = false)
    @Builder.Default
    private Integer posY = 0;
}
