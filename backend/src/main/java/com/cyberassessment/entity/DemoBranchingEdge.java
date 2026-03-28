package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "demo_branching_edge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoBranchingEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id")
    private DemoBranchingWorkflowVersion version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_node_id")
    private DemoBranchingNode fromNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_node_id")
    private DemoBranchingNode toNode;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "condition_kind", nullable = false, length = 24)
    private String conditionKind;

    @Column(name = "condition_value", length = 255)
    private String conditionValue;
}
