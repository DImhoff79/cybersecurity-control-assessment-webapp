package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demo_branching_workflow_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoBranchingWorkflowVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id")
    private DemoBranchingWorkflow workflow;

    @Column(name = "version_label", nullable = false, length = 64)
    private String versionLabel;

    @Column(name = "start_node_id")
    private Long startNodeId;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DemoBranchingNode> nodes = new ArrayList<>();

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DemoBranchingEdge> edges = new ArrayList<>();
}
