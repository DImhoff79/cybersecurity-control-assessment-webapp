package com.cyberassessment.service;

import com.cyberassessment.dto.AuditControlDto;
import com.cyberassessment.dto.MyTaskDto;
import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.ControlDto;
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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:control_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class ControlAndAuditControlServiceIntegrationTest {

    @Autowired
    private ControlService controlService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private AuditControlService auditControlService;
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
    void controlServiceFiltersAndUpdatesWork() {
        List<ControlDto> all = controlService.findAll(null, null, true);
        assertThat(all).isNotEmpty();

        List<ControlDto> hipaaEnabled = controlService.findAll(ControlFramework.HIPAA, true, false);
        assertThat(hipaaEnabled).allMatch(c -> c.getFramework() == ControlFramework.HIPAA && Boolean.TRUE.equals(c.getEnabled()));

        ControlDto first = all.get(0);
        ControlDto patched = controlService.patch(first.getId(), "Updated Control Name", null, null);
        assertThat(patched.getName()).isEqualTo("Updated Control Name");

        assertThatThrownBy(() -> controlService.update(999999L, "x", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Control not found");
    }

    @Test
    void auditControlServiceAccessAndUpdateWork() {
        User owner = userRepository.save(User.builder()
                .email("owner-control@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .build());
        User other = userRepository.save(User.builder()
                .email("other-control@test.com")
                .passwordHash("x")
                .displayName("Other")
                .role(UserRole.APPLICATION_OWNER)
                .build());
        User admin = userRepository.save(User.builder()
                .email("admin-control@test.com")
                .passwordHash("x")
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .build());

        Application app = applicationRepository.save(Application.builder()
                .name("Control Service App")
                .description("test")
                .owner(owner)
                .build());

        authenticate(admin.getEmail());
        AuditDto created = auditService.create(app.getId(), 2033);
        auditService.assign(created.getId(), owner.getId());
        Long auditId = created.getId();

        authenticate(owner.getEmail());
        List<AuditControlDto> ownerControls = auditControlService.findByAuditId(auditId);
        assertThat(ownerControls).isNotEmpty();

        AuditControlDto updated = auditControlService.update(
                ownerControls.get(0).getId(),
                ControlAssessmentStatus.PASS,
                "Evidence",
                "Notes"
        );
        assertThat(updated.getStatus()).isEqualTo(ControlAssessmentStatus.PASS);
        assertThat(updated.getAssessedAt()).isNotNull();

        authenticate(other.getEmail());
        assertThatThrownBy(() -> auditControlService.findByAuditId(auditId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not have access");

        authenticate(admin.getEmail());
        auditControlService.addAssignment(ownerControls.get(0).getId(), other.getId(), AuditControlAssignmentRole.CONTRIBUTOR);

        authenticate(other.getEmail());
        List<AuditControlDto> delegatedControls = auditControlService.findByAuditId(auditId);
        assertThat(delegatedControls).hasSize(1);
        List<MyTaskDto> myTasks = auditControlService.myTasks();
        assertThat(myTasks).hasSize(1);
        assertThat(myTasks.get(0).getAuditControlId()).isEqualTo(ownerControls.get(0).getId());

        authenticate(admin.getEmail());
        List<AuditControlDto> adminControls = auditControlService.findByAuditId(auditId);
        assertThat(adminControls).isNotEmpty();
        assertThat(auditRepository.findById(auditId)).isPresent();
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
