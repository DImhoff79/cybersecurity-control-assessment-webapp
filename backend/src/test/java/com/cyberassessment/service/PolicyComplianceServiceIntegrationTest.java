package com.cyberassessment.service;

import com.cyberassessment.dto.*;
import com.cyberassessment.entity.*;
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
        "spring.datasource.url=jdbc:h2:mem:policy_compliance_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class PolicyComplianceServiceIntegrationTest {

    @Autowired
    private PolicyService policyService;
    @Autowired
    private ComplianceService complianceService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ControlRepository controlRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void policyPublishAndAcknowledgementFlowWorks() {
        User manager = userRepository.save(User.builder()
                .email("policy-manager@test.com")
                .passwordHash("x")
                .displayName("Policy Manager")
                .role(UserRole.AUDIT_MANAGER)
                .permissions(UserRole.AUDIT_MANAGER.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("policy-owner@test.com")
                .passwordHash("x")
                .displayName("Policy Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());

        authenticate(manager.getEmail());
        PolicyDto policy = policyService.create(
                "POL-100",
                "Access Control Policy",
                "Baseline access control requirements.",
                manager.getId(),
                "Access Control v1",
                "## Policy\n\nInitial draft"
        );
        assertThat(policy.getVersions()).hasSize(1);

        PolicyVersionDto v2 = policyService.createVersion(policy.getId(), "Access Control v2", "## Policy\n\nUpdated content");
        PolicyDto published = policyService.publish(policy.getId(), v2.getId(), Instant.now().plusSeconds(3600));
        assertThat(published.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(published.getPublishedVersionId()).isEqualTo(v2.getId());

        authenticate(owner.getEmail());
        List<PolicyAcknowledgementDto> mine = policyService.myAcknowledgements();
        assertThat(mine).isNotEmpty();
        PolicyAcknowledgementDto ack = policyService.acknowledge(mine.get(0).getId());
        assertThat(ack.getStatus()).isEqualTo(PolicyAcknowledgementStatus.ACKNOWLEDGED);
    }

    @Test
    void regulationRequirementAndMappingFlowFeedsKpis() {
        Control control = controlRepository.save(Control.builder()
                .controlId("CMP-1")
                .name("Compliance Control")
                .framework(ControlFramework.NIST_800_53_LOW)
                .enabled(true)
                .build());

        RegulationDto regulation = complianceService.createRegulation("SOX", "Sarbanes Oxley", "2024", null, true);
        ComplianceRequirementDto requirement = complianceService.createRequirement(
                regulation.getId(), "SOX-302", "Disclosure Controls", "Disclosure obligations", true
        );
        RequirementControlMappingDto reqControl = complianceService.mapRequirementToControl(
                requirement.getId(), control.getId(), 95, "Primary coverage"
        );

        User manager = userRepository.save(User.builder()
                .email("policy-map-manager@test.com")
                .passwordHash("x")
                .displayName("Policy Manager")
                .role(UserRole.AUDIT_MANAGER)
                .permissions(UserRole.AUDIT_MANAGER.defaultPermissions())
                .build());
        authenticate(manager.getEmail());
        PolicyDto policy = policyService.create("POL-200", "SOX Program Policy", null, manager.getId(), null, "## SOX");
        PolicyRequirementMappingDto policyReq = complianceService.mapPolicyToRequirement(policy.getId(), requirement.getId(), null);

        assertThat(reqControl.getCoveragePct()).isEqualTo(95);
        assertThat(policyReq.getPolicyId()).isEqualTo(policy.getId());

        ComplianceKpiDto kpis = complianceService.complianceKpis();
        assertThat(kpis.getTotalRequirements()).isGreaterThanOrEqualTo(1);
        assertThat(kpis.getMappedRequirementsToControls()).isGreaterThanOrEqualTo(1);
        assertThat(kpis.getMappedRequirementsToPolicies()).isGreaterThanOrEqualTo(1);
        assertThat(kpis.getControlCoveragePct()).isGreaterThan(0.0);
        assertThat(kpis.getPolicyCoveragePct()).isGreaterThan(0.0);
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
