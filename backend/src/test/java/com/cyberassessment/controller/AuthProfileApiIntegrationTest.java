package com.cyberassessment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ensures /api/auth/profile is registered (POST and PUT). A 404 on save usually means the running
 * JVM was started from an older build before this endpoint existed.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_profile_api_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class AuthProfileApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postProfileUpdatesNames() throws Exception {
        mockMvc.perform(post("/api/auth/profile")
                        .with(httpBasic("owner@example.com", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Jane\",\"lastName\":\"Doe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void putProfileUpdatesNames() throws Exception {
        mockMvc.perform(put("/api/auth/profile")
                        .with(httpBasic("owner@example.com", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Pat\",\"lastName\":\"Lee\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Pat"))
                .andExpect(jsonPath("$.lastName").value("Lee"));
    }
}
