package com.cyberassessment.service;

import com.cyberassessment.dto.RemediationActionDto;
import com.cyberassessment.dto.RemediationPlanDto;
import com.cyberassessment.dto.RiskRegisterItemDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.ControlRepository;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:risk_remediation_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class RiskRemediationServiceIntegrationTest {

    @Autowired
    private RiskService riskService;
    @Autowired
    private RemediationService remediationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ControlRepository controlRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void riskRegisterCreateUpdateAndLinkFlowsWork() {
        User manager = userRepository.save(User.builder()
                .email("risk-manager@test.com")
                .passwordHash("x")
                .displayName("Risk Manager")
                .role(UserRole.AUDIT_MANAGER)
                .permissions(UserRole.AUDIT_MANAGER.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("risk-owner@test.com")
                .passwordHash("x")
                .displayName("Risk Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());

        Application app = applicationRepository.save(Application.builder()
                .name("Risk Test App")
                .description("Risk test app")
                .owner(owner)
                .build());

        Control control = controlRepository.findAll().stream()
                .filter(c -> c.getFramework() == ControlFramework.NIST_800_53_LOW)
                .findFirst()
                .orElseThrow();

        authenticate(manager.getEmail());

        RiskRegisterItemDto created = riskService.create(
                "Credential theft scenario",
                "Elevated risk around privileged access misuse.",
                "Material impact to financial reporting.",
                4,
                5,
                owner.getId(),
                app.getId(),
                Instant.now().plusSeconds(86400 * 30)
        );
        assertThat(created.getId()).isNotNull();
        assertThat(created.getInherentRiskScore()).isEqualTo(20);
        assertThat(created.getStatus()).isEqualTo(RiskStatus.OPEN);

        RiskRegisterItemDto updated = riskService.update(
                created.getId(),
                "Credential theft scenario (updated)",
                null,
                null,
                3,
                4,
                owner.getId(),
                RiskStatus.MONITORING,
                8,
                Instant.now().plusSeconds(86400 * 45)
        );
        assertThat(updated.getResidualRiskScore()).isEqualTo(8);
        assertThat(updated.getStatus()).isEqualTo(RiskStatus.MONITORING);

        riskService.linkControl(created.getId(), control.getId(), "Primary mitigating control");
        assertThat(riskService.controls(created.getId())).isNotEmpty();
    }

    @Test
    void remediationPlanAndActionFlowWorks() {
        User manager = userRepository.save(User.builder()
                .email("remediation-manager@test.com")
                .passwordHash("x")
                .displayName("Remediation Manager")
                .role(UserRole.AUDIT_MANAGER)
                .permissions(UserRole.AUDIT_MANAGER.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("remediation-owner@test.com")
                .passwordHash("x")
                .displayName("Remediation Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());
        authenticate(manager.getEmail());

        RiskRegisterItemDto risk = riskService.create(
                "Vendor access concentration",
                "Shared vendor credentials increase blast radius.",
                "Operational disruption possible.",
                4,
                4,
                owner.getId(),
                null,
                Instant.now().plusSeconds(86400 * 60)
        );

        RemediationPlanDto plan = remediationService.createPlan(
                risk.getId(),
                "Privilege hardening plan",
                Instant.now().plusSeconds(86400 * 21)
        );
        assertThat(plan.getStatus()).isEqualTo(RemediationPlanStatus.DRAFT);

        RemediationActionDto action = remediationService.createAction(
                plan.getId(),
                "Rotate privileged credentials",
                "Rotate and vault all shared credentials.",
                owner.getId(),
                Instant.now().plusSeconds(86400 * 7),
                1
        );
        assertThat(action.getStatus()).isEqualTo(RemediationActionStatus.TODO);

        RemediationActionDto inProgress = remediationService.updateAction(
                action.getId(),
                RemediationActionStatus.IN_PROGRESS,
                null,
                null
        );
        assertThat(inProgress.getStatus()).isEqualTo(RemediationActionStatus.IN_PROGRESS);

        RemediationActionDto done = remediationService.updateAction(
                action.getId(),
                RemediationActionStatus.DONE,
                Instant.now(),
                null
        );
        assertThat(done.getCompletedAt()).isNotNull();

        RemediationPlanDto progressed = remediationService.updatePlan(
                plan.getId(),
                "Privilege hardening plan",
                RemediationPlanStatus.IN_PROGRESS,
                Instant.now().plusSeconds(86400 * 14)
        );
        assertThat(progressed.getStatus()).isEqualTo(RemediationPlanStatus.IN_PROGRESS);

        List<RemediationActionDto> actions = remediationService.actions(plan.getId());
        assertThat(actions).hasSize(1);
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
