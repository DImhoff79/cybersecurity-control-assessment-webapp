package com.cyberassessment.service;

import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditStatus;
import com.cyberassessment.entity.Finding;
import com.cyberassessment.entity.FindingSeverity;
import com.cyberassessment.entity.FindingStatus;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.AuditRepository;
import com.cyberassessment.repository.FindingRepository;
import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:audit_automation_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.automation.enabled=true",
        "app.remediation.reminder-min-hours-between=0",
        "app.remediation.escalation-days-overdue=1"
})
@Transactional
class AuditAutomationServiceIntegrationTest {

    @Autowired
    private AuditAutomationService auditAutomationService;
    @Autowired
    private FindingRepository findingRepository;
    @Autowired
    private AuditRepository auditRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void overdueFindingsGetReminderAndEscalationTimestamps() {
        User owner = userRepository.save(User.builder()
                .email("remediation-owner@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .build());
        userRepository.save(User.builder()
                .email("remediation-admin@test.com")
                .passwordHash("x")
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .build());
        userRepository.save(User.builder()
                .email("remediation-manager@test.com")
                .passwordHash("x")
                .displayName("Manager")
                .role(UserRole.AUDIT_MANAGER)
                .build());

        Application app = applicationRepository.save(Application.builder()
                .name("Remediation App")
                .description("test")
                .owner(owner)
                .build());
        Audit audit = auditRepository.save(Audit.builder()
                .application(app)
                .year(2047)
                .status(AuditStatus.IN_PROGRESS)
                .assignedTo(owner)
                .dueAt(Instant.now().plusSeconds(86400))
                .build());

        Finding finding = findingRepository.save(Finding.builder()
                .audit(audit)
                .title("Overdue remediation item")
                .description("Needs closure")
                .severity(FindingSeverity.HIGH)
                .status(FindingStatus.OPEN)
                .owner(owner)
                .dueAt(Instant.now().minusSeconds(3 * 86400))
                .build());

        auditAutomationService.runDailyAuditAutomation();

        Finding refreshed = findingRepository.findById(finding.getId()).orElseThrow();
        assertThat(refreshed.getReminderSentAt()).isNotNull();
        assertThat(refreshed.getEscalatedAt()).isNotNull();
    }
}
