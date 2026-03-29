package com.cyberassessment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fast HTTP-level smoke and regression checks: public auth metadata, actuator, demo APIs.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api_smoke_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        // No SMTP in test JVM; mail health would make aggregate status DOWN and break this smoke check.
        "management.health.mail.enabled=false"
})
@AutoConfigureMockMvc
class ApplicationApiSmokeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authProvidersIsPublic() throws Exception {
        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authMode").exists())
                .andExpect(jsonPath("$.basic.enabled").exists());
    }

    @Test
    void actuatorHealthReachableWithSeedAdminCredentials() throws Exception {
        mockMvc.perform(get("/actuator/health").with(httpBasic("admin@example.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void authMeRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void authMeReturnsUserForOwnerBasicAuth() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(httpBasic("owner@example.com", "owner123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"));
    }

    @Test
    void demoBranchingGraphRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/demo/branching-workflows/graph")).andExpect(status().isUnauthorized());
    }

    @Test
    void demoBranchingGraphReturnsSeededWorkflowForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/demo/branching-workflows/graph").with(httpBasic("audit.manager@example.com", "manager123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionId").value(1))
                .andExpect(jsonPath("$.startNodeId").value(1))
                .andExpect(jsonPath("$.nodes.length()").value(4))
                .andExpect(jsonPath("$.edges.length()").value(7));
    }

    @Test
    void demoBranchingSaveGraphForbiddenWithoutAuditManagementPermission() throws Exception {
        mockMvc.perform(put("/api/demo/branching-workflows/graph")
                        .with(httpBasic("owner@example.com", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void demoBranchingResolveYesTransition() throws Exception {
        String body = """
                {"versionId":1,"fromNodeId":1,"value":"yes"}
                """;
        mockMvc.perform(post("/api/demo/branching-workflows/resolve")
                        .with(httpBasic("owner@example.com", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finished").value(false))
                .andExpect(jsonPath("$.nextNode.stableKey").value("describe_scope"));
    }
}
