package com.cyberassessment.controller;

import com.cyberassessment.dto.RemediationActionDto;
import com.cyberassessment.dto.RemediationPlanDto;
import com.cyberassessment.dto.RiskControlLinkDto;
import com.cyberassessment.dto.RiskRegisterItemDto;
import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.ControlFramework;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserPermission;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.ControlRepository;
import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:risk_remediation_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class RiskRemediationControllerIntegrationTest {

    @Autowired
    private RiskController riskController;
    @Autowired
    private RemediationController remediationController;
    @Autowired
    private ReportController reportController;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ControlRepository controlRepository;

    @Test
    void riskAndRemediationControllerFlowsWork() {
        User manager = authenticateAsManager("risk-remediation-controller@test.com");
        User owner = userRepository.save(User.builder()
                .email("rr-owner@test.com")
                .passwordHash("x")
                .displayName("RR Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());

        RiskRegisterItemDto risk = riskController.create(Map.of(
                "title", "Privileged account abuse",
                "description", "Risk scenario from controller test",
                "businessImpact", "Potential control breakdown",
                "likelihoodScore", 4,
                "impactScore", 5,
                "ownerUserId", owner.getId(),
                "otherApplicationText", "Shared identity platform"
        ));
        assertThat(risk.getId()).isNotNull();
        assertThat(risk.getInherentRiskScore()).isEqualTo(20);
        assertThat(risk.getResidualRiskScore()).isNull();
        assertThat(risk.getOtherApplicationText()).isEqualTo("Shared identity platform");

        RiskRegisterItemDto updated = riskController.update(risk.getId(), Map.of(
                "status", "MONITORING",
                "residualRiskScore", 9
        ));
        assertThat(updated.getStatus().name()).isEqualTo("MONITORING");
        assertThat(updated.getResidualRiskScore()).isEqualTo(9);

        Control control = controlRepository.findAll().stream()
                .filter(c -> c.getFramework() == ControlFramework.NIST_800_53_LOW)
                .findFirst()
                .orElseThrow();
        RiskControlLinkDto link = riskController.linkControl(risk.getId(), Map.of(
                "controlId", control.getId(),
                "notes", "Controller coverage mapping"
        ));
        assertThat(link.getControlId()).isEqualTo(control.getId());
        assertThat(riskController.controls(risk.getId())).isNotEmpty();

        RemediationPlanDto plan = remediationController.createPlan(Map.of(
                "riskId", risk.getId(),
                "title", "Privileged access hardening"
        ));
        assertThat(plan.getId()).isNotNull();

        RemediationPlanDto progressed = remediationController.updatePlan(plan.getId(), Map.of(
                "status", "IN_PROGRESS"
        ));
        assertThat(progressed.getStatus().name()).isEqualTo("IN_PROGRESS");

        RemediationActionDto action = remediationController.createAction(plan.getId(), Map.of(
                "actionTitle", "Rotate privileged credentials",
                "ownerUserId", manager.getId(),
                "sequenceNo", 1
        ));
        assertThat(action.getPlanId()).isEqualTo(plan.getId());

        RemediationActionDto done = remediationController.updateAction(action.getId(), Map.of(
                "status", "DONE"
        ));
        assertThat(done.getStatus().name()).isEqualTo("DONE");
        assertThat(remediationController.actions(plan.getId())).hasSize(1);

        assertThat(reportController.riskKpis()).isNotNull();
        assertThat(reportController.complianceKpis()).isNotNull();
    }

    private User authenticateAsManager(String email) {
        User user = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(email)
                        .passwordHash("x")
                        .displayName("RiskRemediation Manager")
                        .role(UserRole.AUDIT_MANAGER)
                        .permissions(EnumSet.allOf(UserPermission.class))
                        .build())
        );

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        user.getPermissions().forEach(permission ->
                authorities.add(new SimpleGrantedAuthority("PERM_" + permission.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "pw", authorities)
        );
        return user;
    }
}
