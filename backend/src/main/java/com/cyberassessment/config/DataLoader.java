package com.cyberassessment.config;

import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = User.builder()
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .displayName("System Administrator")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build();
        userRepository.save(admin);
        log.info("Created default admin user: admin@example.com / admin123");

        User appOwner = User.builder()
                .email("owner@example.com")
                .passwordHash(passwordEncoder.encode("owner123"))
                .displayName("Sample Application Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build();
        userRepository.save(appOwner);
        log.info("Created sample application owner: owner@example.com / owner123");

        User auditor = User.builder()
                .email("auditor@example.com")
                .passwordHash(passwordEncoder.encode("auditor123"))
                .displayName("Sample Auditor")
                .role(UserRole.AUDITOR)
                .permissions(UserRole.AUDITOR.defaultPermissions())
                .build();
        userRepository.save(auditor);
        log.info("Created sample auditor: auditor@example.com / auditor123");

        User auditManager = User.builder()
                .email("audit.manager@example.com")
                .passwordHash(passwordEncoder.encode("manager123"))
                .displayName("Sample Audit Manager")
                .role(UserRole.AUDIT_MANAGER)
                .permissions(UserRole.AUDIT_MANAGER.defaultPermissions())
                .build();
        userRepository.save(auditManager);
        log.info("Created sample audit manager: audit.manager@example.com / manager123");
    }
}
