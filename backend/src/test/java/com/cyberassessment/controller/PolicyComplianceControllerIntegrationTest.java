package com.cyberassessment.controller;

import com.cyberassessment.dto.ComplianceRequirementDto;
import com.cyberassessment.dto.PolicyDto;
import com.cyberassessment.dto.PolicyRequirementMappingDto;
import com.cyberassessment.dto.RequirementControlMappingDto;
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
        "spring.datasource.url=jdbc:h2:mem:policy_compliance_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class PolicyComplianceControllerIntegrationTest {

    @Autowired
    private PolicyController policyController;
    @Autowired
    private ComplianceController complianceController;
    @Autowired
    private ControlController controlController;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ControlRepository controlRepository;

    @Test
    void policyAndComplianceControllerFlowsWork() {
        authenticateAsManager("policy-compliance-controller@test.com");

        PolicyDto policy = policyController.create(Map.of(
                "code", "POL-CTRL-1",
                "name", "Controller Policy",
                "description", "Controller-driven policy"
        )).getBody();
        assertThat(policy).isNotNull();
        assertThat(policy.getCode()).isEqualTo("POL-CTRL-1");

        var version = policyController.createVersion(policy.getId(), Map.of(
                "title", "Controller Policy v2",
                "bodyMarkdown", "## Controller Body"
        ));
        assertThat(version.getVersionNumber()).isGreaterThanOrEqualTo(2);

        PolicyDto published = policyController.publish(policy.getId(), Map.of(
                "policyVersionId", version.getId()
        ));
        assertThat(published.getPublishedVersionId()).isEqualTo(version.getId());

        var regulation = complianceController.createRegulation(Map.of(
                "code", "PCI",
                "name", "PCI DSS"
        ));
        assertThat(regulation.getCode()).isEqualTo("PCI");

        ComplianceRequirementDto requirement = complianceController.createRequirement(Map.of(
                "regulationId", regulation.getId(),
                "requirementCode", "PCI-7.1",
                "title", "Access permissions"
        ));
        assertThat(requirement.getRequirementCode()).isEqualTo("PCI-7.1");

        Long controlId = controlRepository.findAll().stream()
                .filter(c -> c.getFramework() == ControlFramework.NIST_800_53_LOW)
                .findFirst()
                .orElseThrow()
                .getId();

        RequirementControlMappingDto reqControl = complianceController.createRequirementControlMapping(Map.of(
                "requirementId", requirement.getId(),
                "controlId", controlId,
                "coveragePct", 90
        ));
        assertThat(reqControl.getCoveragePct()).isEqualTo(90);

        PolicyRequirementMappingDto policyReq = complianceController.createPolicyRequirementMapping(Map.of(
                "policyId", policy.getId(),
                "requirementId", requirement.getId()
        ));
        assertThat(policyReq.getPolicyId()).isEqualTo(policy.getId());

        assertThat(complianceController.kpis().getTotalRequirements()).isGreaterThanOrEqualTo(1);
        assertThat(policyController.acknowledgements(null)).isNotNull();
        assertThat(controlController.list(null, true, false)).isNotEmpty();
    }

    private void authenticateAsManager(String email) {
        User user = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(email)
                        .passwordHash("x")
                        .displayName("Policy Compliance Manager")
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
    }
}
