package com.cyberassessment.service;

import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.AuditEvidenceDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.AuditControlRepository;
import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:evidence_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.evidence-storage-path=./target/test-evidence"
})
@Transactional
class AuditEvidenceServiceIntegrationTest {

    @Autowired
    private AuditService auditService;
    @Autowired
    private AuditEvidenceService auditEvidenceService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private AuditControlRepository auditControlRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadAndDownloadEvidenceFileWorks() {
        User admin = userRepository.save(User.builder()
                .email("evidence-admin@test.com")
                .passwordHash("x")
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .build());
        User owner = userRepository.save(User.builder()
                .email("evidence-owner@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Evidence App")
                .description("test")
                .owner(owner)
                .build());

        authenticate(admin.getEmail());
        AuditDto audit = auditService.create(app.getId(), 2036);
        auditService.assign(audit.getId(), owner.getId());
        auditService.sendToOwner(audit.getId());
        Long auditControlId = auditControlRepository.findByAuditId(audit.getId()).get(0).getId();

        authenticate(owner.getEmail());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.txt",
                "text/plain",
                "sample evidence body".getBytes()
        );
        AuditEvidenceDto dto = auditEvidenceService.upload(
                auditControlId,
                EvidenceType.DOCUMENT,
                "Policy attachment",
                "Uploaded by owner",
                file
        );
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getFileName()).isEqualTo("evidence.txt");
        assertThat(dto.getSizeBytes()).isEqualTo(file.getSize());
        assertThat(dto.getUri()).contains("/api/evidences/");

        Resource resource = auditEvidenceService.loadEvidenceFile(dto.getId());
        assertThat(resource.exists()).isTrue();
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
