package com.cyberassessment.config;

import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:default_users_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class DefaultUsersIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void defaultAdminAndOwnerAccountsExistAndPasswordsMatch() {
        User admin = userRepository.findByEmail("admin@example.com").orElse(null);
        User owner = userRepository.findByEmail("owner@example.com").orElse(null);
        User auditor = userRepository.findByEmail("auditor@example.com").orElse(null);
        User auditManager = userRepository.findByEmail("audit.manager@example.com").orElse(null);

        assertThat(admin).isNotNull();
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(passwordEncoder.matches("admin123", admin.getPasswordHash())).isTrue();

        assertThat(owner).isNotNull();
        assertThat(owner.getRole()).isEqualTo(UserRole.APPLICATION_OWNER);
        assertThat(passwordEncoder.matches("owner123", owner.getPasswordHash())).isTrue();

        assertThat(auditor).isNotNull();
        assertThat(auditor.getRole()).isEqualTo(UserRole.AUDITOR);
        assertThat(passwordEncoder.matches("auditor123", auditor.getPasswordHash())).isTrue();

        assertThat(auditManager).isNotNull();
        assertThat(auditManager.getRole()).isEqualTo(UserRole.AUDIT_MANAGER);
        assertThat(passwordEncoder.matches("manager123", auditManager.getPasswordHash())).isTrue();
    }
}
