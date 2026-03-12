package com.cyberassessment.service;

import com.cyberassessment.dto.ReportScheduleDto;
import com.cyberassessment.dto.ReportScheduleUpsertRequest;
import com.cyberassessment.entity.ReportScheduleFrequency;
import com.cyberassessment.entity.ReportScheduleType;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.ReportScheduleRepository;
import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:report_schedule_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class ReportScheduleServiceIntegrationTest {
    @Autowired
    private ReportScheduleService reportScheduleService;
    @Autowired
    private ReportScheduleRepository reportScheduleRepository;
    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAndRunScheduleUpdatesRunMetadata() {
        User admin = userRepository.save(User.builder()
                .email("schedule-admin@test.com")
                .passwordHash("x")
                .displayName("Schedule Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        authenticate(admin.getEmail());

        ReportScheduleDto created = reportScheduleService.create(ReportScheduleUpsertRequest.builder()
                .name("Weekly Audits CSV")
                .reportType(ReportScheduleType.AUDITS_CSV)
                .frequency(ReportScheduleFrequency.WEEKLY)
                .recipientEmails("owner@example.com")
                .firstRunAt(Instant.now().minusSeconds(60))
                .build());
        assertThat(created.getId()).isNotNull();

        reportScheduleService.runDueSchedules(Instant.now());

        var updated = reportScheduleRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getLastRunAt()).isNotNull();
        assertThat(updated.getLastRunStatus()).isNotBlank();
        assertThat(updated.getNextRunAt()).isAfter(Instant.now().minusSeconds(5));
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
