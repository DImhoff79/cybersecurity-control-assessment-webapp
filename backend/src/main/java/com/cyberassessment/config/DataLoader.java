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
                .build();
        userRepository.save(admin);
        log.info("Created default admin user: admin@example.com / admin123");

        User appOwner = User.builder()
                .email("owner@example.com")
                .passwordHash(passwordEncoder.encode("owner123"))
                .displayName("Sample Application Owner")
                .role(UserRole.APPLICATION_OWNER)
                .build();
        userRepository.save(appOwner);
        log.info("Created sample application owner: owner@example.com / owner123");
    }
}
