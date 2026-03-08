package com.cyberassessment.service;

import com.cyberassessment.dto.AccessRequestDto;
import com.cyberassessment.entity.IdentityProvider;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:access_request_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class AccessRequestServiceIntegrationTest {
    @Autowired
    private AccessRequestService accessRequestService;
    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void socialRequestCanBeSubmittedAndApproved() {
        User admin = userRepository.save(User.builder()
                .email("access-admin@test.com")
                .passwordHash("x")
                .displayName("Access Admin")
                .role(UserRole.ADMIN)
                .build());

        AccessRequestService.SocialAuthResult result = accessRequestService.processSocialSignIn(
                IdentityProvider.GOOGLE,
                "google-sub-123",
                "new-user@test.com",
                "New User"
        );
        assertThat(result.approved()).isFalse();
        assertThat(result.newlyRequested()).isTrue();

        authenticate(admin.getEmail());
        List<AccessRequestDto> pending = accessRequestService.listPending();
        assertThat(pending).hasSize(1);
        AccessRequestDto approved = accessRequestService.approve(pending.get(0).getId(), UserRole.APPLICATION_OWNER, "approved");
        assertThat(approved.getStatus().name()).isEqualTo("APPROVED");
        assertThat(userRepository.findByEmail("new-user@test.com")).isPresent();
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
