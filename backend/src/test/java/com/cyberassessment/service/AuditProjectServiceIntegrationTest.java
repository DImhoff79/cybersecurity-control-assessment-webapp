package com.cyberassessment.service;

import com.cyberassessment.dto.AuditProjectDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.AuditRepository;
import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:audit_project_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class AuditProjectServiceIntegrationTest {

    @Autowired
    private AuditProjectService auditProjectService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private AuditRepository auditRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBuildsProjectAndGeneratesScopedAudits() {
        User admin = userRepository.save(User.builder()
                .email("project-admin@test.com")
                .passwordHash("x")
                .displayName("Project Admin")
                .role(UserRole.ADMIN)
                .build());
        User owner = userRepository.save(User.builder()
                .email("project-owner@test.com")
                .passwordHash("x")
                .displayName("Project Owner")
                .role(UserRole.APPLICATION_OWNER)
                .build());

        Application app1 = applicationRepository.save(Application.builder().name("PCI Billing").owner(owner).build());
        Application app2 = applicationRepository.save(Application.builder().name("PCI Orders").owner(owner).build());

        authenticate(admin.getEmail());
        auditService.create(app1.getId(), 2099); // pre-existing audit for duplicate guard

        AuditProjectDto created = auditProjectService.create(
                "PCI 2036",
                "PCI",
                2099,
                "Annual PCI audit",
                null,
                Instant.parse("2099-12-31T23:59:59Z"),
                List.of(app1.getId(), app2.getId())
        );

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("PCI 2036");
        assertThat(created.getScopedApplications()).hasSize(2);
        assertThat(created.getTotalAudits()).isEqualTo(1); // one app already had a year-matching audit
        assertThat(created.getAudits()).allMatch(a -> "PCI 2036".equals(a.getProjectName()));

        Audit linkedAudit = auditRepository.findByApplicationIdAndYear(app2.getId(), 2099).orElseThrow();
        assertThat(linkedAudit.getAuditProject()).isNotNull();
        assertThat(linkedAudit.getAuditProject().getId()).isEqualTo(created.getId());
    }

    @Test
    void createRequiresAdmin() {
        User nonAdmin = userRepository.save(User.builder()
                .email("project-non-admin@test.com")
                .passwordHash("x")
                .displayName("Not Admin")
                .role(UserRole.APPLICATION_OWNER)
                .build());
        Application app = applicationRepository.save(Application.builder().name("Project App").build());
        authenticate(nonAdmin.getEmail());

        assertThatThrownBy(() -> auditProjectService.create(
                "SOX 2036",
                "SOX",
                2036,
                null,
                null,
                null,
                List.of(app.getId())
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only admins");
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
