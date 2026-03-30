package com.cyberassessment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "controls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Control {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String controlId;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ControlFramework framework;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(length = 255)
    private String category;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "control_regulatory_scope", joinColumns = @JoinColumn(name = "control_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    @Builder.Default
    private Set<RegulatoryScopeTag> regulatoryScopes = new HashSet<>();

    @OneToMany(mappedBy = "control", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();
}
