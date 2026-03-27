package com.cyberassessment.config;

import com.cyberassessment.repository.UserRepository;
import com.cyberassessment.util.UserNameFormatting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time style backfill: split legacy {@code displayName} into first/last when columns are empty.
 */
@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class UserNameBackfillRunner implements ApplicationRunner {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findAll().forEach(u -> {
            if (u.getFirstName() != null || u.getLastName() != null) {
                return;
            }
            if (u.getDisplayName() == null || u.getDisplayName().isBlank()) {
                return;
            }
            String[] parts = UserNameFormatting.splitLegacyDisplayName(u.getDisplayName());
            u.setFirstName(parts[0]);
            u.setLastName(parts[1]);
            userRepository.save(u);
        });
        log.debug("User first/last name backfill pass complete");
    }
}
