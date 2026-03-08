package com.cyberassessment.service;

import com.cyberassessment.dto.AuditYearSummaryDto;
import com.cyberassessment.dto.AuditTrendPointDto;
import com.cyberassessment.dto.AuditorDashboardDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.AuditControlRepository;
import com.cyberassessment.repository.AuditEvidenceRepository;
import com.cyberassessment.repository.AuditRepository;
import com.cyberassessment.repository.ControlRepository;
import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:report_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class ReportServiceIntegrationTest {

    @Autowired
    private ReportService reportService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private AuditRepository auditRepository;
    @Autowired
    private ControlRepository controlRepository;
    @Autowired
    private AuditControlRepository auditControlRepository;
    @Autowired
    private AuditEvidenceRepository auditEvidenceRepository;

    @Test
    void byYearAndCsvExportContainExpectedData() {
        User owner = userRepository.save(User.builder()
                .email("report-owner@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Report App")
                .description("test")
                .owner(owner)
                .build());

        Audit submitted = auditRepository.save(Audit.builder()
                .application(app)
                .year(2030)
                .status(AuditStatus.SUBMITTED)
                .dueAt(Instant.now().plusSeconds(3600))
                .build());
        auditRepository.save(Audit.builder()
                .application(app)
                .year(2030)
                .status(AuditStatus.COMPLETE)
                .dueAt(Instant.now().plusSeconds(7200))
                .build());

        List<AuditYearSummaryDto> byYear = reportService.byYear();
        AuditYearSummaryDto row2030 = byYear.stream().filter(r -> r.getYear() == 2030).findFirst().orElseThrow();
        assertThat(row2030.getTotal()).isGreaterThanOrEqualTo(2);
        assertThat(row2030.getSubmitted()).isGreaterThanOrEqualTo(1);
        assertThat(row2030.getComplete()).isGreaterThanOrEqualTo(1);

        String csv = reportService.auditsCsv();
        assertThat(csv).contains("audit_id,application,year,status");
        assertThat(csv).contains("Report App");
        assertThat(csv).contains("2030");

        Control control = controlRepository.save(Control.builder()
                .controlId("REP-1")
                .name("Reporting Control")
                .framework(ControlFramework.NIST_800_53_LOW)
                .enabled(true)
                .build());
        AuditControl ac = auditControlRepository.save(AuditControl.builder()
                .audit(submitted)
                .control(control)
                .status(ControlAssessmentStatus.IN_PROGRESS)
                .build());
        auditEvidenceRepository.save(AuditEvidence.builder()
                .auditControl(ac)
                .evidenceType(EvidenceType.DOCUMENT)
                .title("Pending Document")
                .reviewStatus(EvidenceReviewStatus.PENDING)
                .build());

        AuditorDashboardDto dashboard = reportService.auditorDashboard();
        assertThat(dashboard.getAuditsNeedingAttention()).isNotEmpty();
        assertThat(dashboard.getEvidenceQueue()).isNotEmpty();
        assertThat(dashboard.getAuditsNeedingAttention().get(0).getFrameworks()).isNotBlank();
        assertThat(dashboard.getEvidenceQueue().get(0).getFramework()).isNotBlank();

        List<AuditTrendPointDto> trends = reportService.trends();
        assertThat(trends).isNotEmpty();
        assertThat(trends.stream().anyMatch(t -> t.getYear() == 2030)).isTrue();

        byte[] pdf = reportService.boardPackPdf();
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }
}
