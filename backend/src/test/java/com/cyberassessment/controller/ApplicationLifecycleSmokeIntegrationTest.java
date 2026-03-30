package com.cyberassessment.controller;

import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.User;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fast smoke checks for unified intake, assessment-controls, and security architecture review APIs.
 */
@Tag("smoke")
@Tag("integration")
@Tag("regression")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:lifecycle_smoke;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.mail.enabled=false",
        "app.auth.seed-local-users=true",
        "app.seed.demo-dataset=true"
})
@AutoConfigureMockMvc
class ApplicationLifecycleSmokeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private UserRepository userRepository;

    private long applicationIdForLifecycleTests() {
        return applicationRepository.findAll().stream().findFirst().orElseGet(() -> {
            User owner = userRepository.findByEmail("owner@example.com")
                    .orElseThrow(() -> new IllegalStateException("seed owner user missing"));
            return applicationRepository.save(Application.builder()
                    .name("Lifecycle smoke application")
                    .owner(owner)
                    .build());
        }).getId();
    }

    @Test
    void intakeWizardRequiresOwnerRole() throws Exception {
        mockMvc.perform(get("/api/application-intake/wizard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/application-intake/wizard").with(httpBasic("admin@example.com", "admin123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void intakeWizardReturnsStepsForOwner() throws Exception {
        mockMvc.perform(get("/api/application-intake/wizard").with(httpBasic("owner@example.com", "owner123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void assessmentControlsForbiddenForAuditorWithoutAppAccess() throws Exception {
        long appId = applicationIdForLifecycleTests();
        mockMvc.perform(get("/api/applications/" + appId + "/assessment-controls")
                        .with(httpBasic("auditor@example.com", "auditor123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void assessmentControlsOkForAdmin() throws Exception {
        long appId = applicationIdForLifecycleTests();
        mockMvc.perform(get("/api/applications/" + appId + "/assessment-controls")
                        .with(httpBasic("admin@example.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void securityArchitectureReviewPatchRequiresElevatedPermission() throws Exception {
        long appId = applicationIdForLifecycleTests();
        String body = "{\"status\":\"IN_REVIEW\",\"notes\":\"ok\"}";
        mockMvc.perform(patch("/api/applications/" + appId + "/security-architecture-review")
                        .with(httpBasic("owner@example.com", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void securityArchitectureReviewPatchAcceptedForAuditManager() throws Exception {
        long appId = applicationIdForLifecycleTests();
        String body = "{\"status\":\"IN_REVIEW\",\"notes\":\"queued\"}";
        mockMvc.perform(patch("/api/applications/" + appId + "/security-architecture-review")
                        .with(httpBasic("audit.manager@example.com", "manager123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }
}
