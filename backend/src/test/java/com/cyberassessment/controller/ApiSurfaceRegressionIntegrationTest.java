package com.cyberassessment.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Broad HTTP regression sweep: authenticated GET surfaces return expected status codes after Flyway + seed data.
 */
@Tag("integration")
@Tag("regression")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api_surface_regression;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.mail.enabled=false",
        "app.auth.seed-local-users=true",
        "app.seed.demo-dataset=true"
})
@AutoConfigureMockMvc
class ApiSurfaceRegressionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0} as {1}")
    @CsvSource(delimiter = '|', value = {
            "/api/applications|admin@example.com|admin123",
            "/api/controls?framework=KROGER_CCF&includeQuestions=false|admin@example.com|admin123",
            "/api/questionnaire-templates|admin@example.com|admin123",
            "/api/users|admin@example.com|admin123",
            "/api/audit-projects|audit.manager@example.com|manager123",
            "/api/compliance/regulations|audit.manager@example.com|manager123",
            "/api/policies|audit.manager@example.com|manager123",
            "/api/risks|audit.manager@example.com|manager123",
            "/api/remediation-plans|audit.manager@example.com|manager123",
            "/api/findings|audit.manager@example.com|manager123",
            "/api/admin/control-exceptions|audit.manager@example.com|manager123",
            "/api/my-audits|owner@example.com|owner123",
            "/api/demo/branching-workflows/graph|owner@example.com|owner123",
            "/api/auth/me|admin@example.com|admin123"
    })
    void getEndpointsReturnOkForPermittedUser(String path, String user, String password) throws Exception {
        mockMvc.perform(get(path).with(httpBasic(user, password)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "/api/questionnaire-templates|owner@example.com|owner123",
            "/api/users|owner@example.com|owner123"
    })
    void adminOnlySurfacesForbiddenForOwner(String path, String user, String password) throws Exception {
        mockMvc.perform(get(path).with(httpBasic(user, password)))
                .andExpect(status().isForbidden());
    }
}
