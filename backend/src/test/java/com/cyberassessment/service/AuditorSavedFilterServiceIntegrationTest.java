package com.cyberassessment.service;

import com.cyberassessment.dto.AuditorSavedFilterDto;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auditor_saved_filter_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class AuditorSavedFilterServiceIntegrationTest {

    @Autowired
    private AuditorSavedFilterService auditorSavedFilterService;
    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createListAndDeleteSavedFilterWorks() {
        User admin = userRepository.save(User.builder()
                .email("saved-filter-admin@test.com")
                .passwordHash("x")
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        authenticate(admin.getEmail());

        AuditorSavedFilterDto created = auditorSavedFilterService.create(
                "My Queue",
                true,
                Map.of(
                        "auditFilter", Map.of("queue", "mine"),
                        "evidenceFilter", Map.of("type", "DOCUMENT")
                )
        );
        assertThat(created.getId()).isNotNull();
        assertThat(created.getFilterState()).containsKey("auditFilter");

        List<AuditorSavedFilterDto> listed = auditorSavedFilterService.listForCurrentUser();
        assertThat(listed).isNotEmpty();
        assertThat(listed.stream().map(AuditorSavedFilterDto::getName)).contains("My Queue");

        auditorSavedFilterService.delete(created.getId());
        List<AuditorSavedFilterDto> afterDelete = auditorSavedFilterService.listForCurrentUser();
        assertThat(afterDelete.stream().map(AuditorSavedFilterDto::getId)).doesNotContain(created.getId());
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
