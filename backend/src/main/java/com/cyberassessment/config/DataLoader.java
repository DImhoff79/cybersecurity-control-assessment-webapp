package com.cyberassessment.config;

import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.seed-local-users:true}")
    private boolean seedLocalUsers;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedLocalUsers) {
            log.info("Skipping local default user seeding (app.auth.seed-local-users=false)");
            return;
        }
        ensureDefaultUser("admin@example.com", "admin123", "System Administrator", UserRole.ADMIN);
        ensureDefaultUser("owner@example.com", "owner123", "Sample Application Owner", UserRole.APPLICATION_OWNER);
        ensureDefaultUser("auditor@example.com", "auditor123", "Sample Auditor", UserRole.AUDITOR);
        ensureDefaultUser("audit.manager@example.com", "manager123", "Sample Audit Manager", UserRole.AUDIT_MANAGER);
    }

    private void ensureDefaultUser(String email, String rawPassword, String displayName, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .displayName(displayName)
                .role(role)
                .permissions(role.defaultPermissions())
                .build();
        userRepository.save(user);
        log.info("Created default user: {} / {}", email, rawPassword);
    }
}
