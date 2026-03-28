package com.cyberassessment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demo_branching_workflow")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoBranchingWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DemoBranchingWorkflowVersion> versions = new ArrayList<>();
}
