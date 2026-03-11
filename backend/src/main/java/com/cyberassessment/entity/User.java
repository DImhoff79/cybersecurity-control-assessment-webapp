package com.cyberassessment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    @Email
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<UserPermission> permissions = new HashSet<>();

    @OneToMany(mappedBy = "owner", cascade = {})
    @Builder.Default
    private List<Application> ownedApplications = new ArrayList<>();

    @OneToMany(mappedBy = "assignedTo", cascade = {})
    @Builder.Default
    private List<Audit> assignedAudits = new ArrayList<>();
}
