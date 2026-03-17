package com.cyberassessment.controller;

import com.cyberassessment.entity.UserPermission;
import com.cyberassessment.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:access_guard_smoke_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class AccessGuardSmokeIntegrationTest {

    @Autowired
    private AuditProjectController auditProjectController;
    @Autowired
    private PolicyController policyController;
    @Autowired
    private RiskController riskController;
    @Autowired
    private RemediationController remediationController;

    @Test
    void auditorPersonaCanReadProgramButCannotManagePolicyOrApproveRemediation() {
        authenticate(
                UserRole.AUDITOR,
                Set.of(
                        UserPermission.REPORT_VIEW,
                        UserPermission.AUDIT_EXECUTION,
                        UserPermission.COMPLIANCE_MANAGEMENT,
                        UserPermission.REMEDIATION_MANAGEMENT
                )
        );

        assertThatCode(() -> auditProjectController.list()).doesNotThrowAnyException();
        assertThatCode(() -> policyController.list()).doesNotThrowAnyException();
        assertThatCode(() -> riskController.list()).doesNotThrowAnyException();

        assertThatThrownBy(() -> policyController.create(Map.of(
                "code", "POL-SMOKE-1",
                "name", "Should fail"
        ))).isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> remediationController.decideApproval(1L, Map.of(
                "approved", true,
                "notes", "Should fail"
        ))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void auditManagerCanApproveRemediation() {
        authenticate(
                UserRole.AUDIT_MANAGER,
                Set.of(
                        UserPermission.REPORT_VIEW,
                        UserPermission.AUDIT_MANAGEMENT,
                        UserPermission.REMEDIATION_MANAGEMENT
                )
        );

        try {
            remediationController.decideApproval(1L, Map.of(
                    "approved", true,
                    "notes", "manager allowed by role guard"
            ));
        } catch (AccessDeniedException e) {
            fail("Audit manager should pass role guard for remediation approvals");
        } catch (Exception ignored) {
            // Expected in this smoke test when planId does not exist; we only care about guard behavior.
        }
    }

    private void authenticate(UserRole role, Set<UserPermission> permissions) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority("PERM_" + permission.name())));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(role.name().toLowerCase() + "@test.com", "pw", authorities)
        );
    }
}
