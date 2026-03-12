package com.cyberassessment.config;

import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:default_users_disabled_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.auth.seed-local-users=false"
})
@Transactional
class DefaultUsersDisabledIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void defaultUsersAreNotSeededWhenDisabled() {
        assertThat(userRepository.findByEmail("admin@example.com")).isEmpty();
        assertThat(userRepository.findByEmail("owner@example.com")).isEmpty();
        assertThat(userRepository.findByEmail("auditor@example.com")).isEmpty();
        assertThat(userRepository.findByEmail("audit.manager@example.com")).isEmpty();
    }
}
