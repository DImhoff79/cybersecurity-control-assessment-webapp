package com.cyberassessment.service;

import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.ControlExceptionCreateRequest;
import com.cyberassessment.dto.ControlExceptionDecisionRequest;
import com.cyberassessment.dto.ControlExceptionDto;
import com.cyberassessment.dto.FindingDto;
import com.cyberassessment.dto.FindingUpsertRequest;
import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.ControlExceptionStatus;
import com.cyberassessment.entity.FindingSeverity;
import com.cyberassessment.entity.FindingStatus;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.AuditControlRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finding_exception_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class FindingAndExceptionServiceIntegrationTest {

    @Autowired
    private FindingService findingService;
    @Autowired
    private ControlExceptionService controlExceptionService;
    @Autowired
    private AuditService auditService;
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
    void findingCreateUpdateAndListFlowWorks() {
        User admin = userRepository.save(User.builder()
                .email("finding-admin@test.com")
                .passwordHash("x")
                .displayName("Finding Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("finding-owner@test.com")
                .passwordHash("x")
                .displayName("Finding Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Finding Test App")
                .description("test")
                .owner(owner)
                .build());

        authenticate(admin.getEmail());
        AuditDto audit = auditService.create(app.getId(), 2045);
        Long auditControlId = auditControlRepository.findByAuditId(audit.getId()).get(0).getId();

        FindingDto created = findingService.create(FindingUpsertRequest.builder()
                .auditId(audit.getId())
                .auditControlId(auditControlId)
                .title("Control gap")
                .description("Control is not fully implemented")
                .severity(FindingSeverity.HIGH)
                .ownerUserId(owner.getId())
                .dueAt(Instant.now().plusSeconds(3600))
                .build());

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(FindingStatus.OPEN);
        assertThat(created.getControlId()).isNotBlank();
        assertThat(created.getOwnerUserId()).isEqualTo(owner.getId());

        FindingDto updated = findingService.update(created.getId(), FindingUpsertRequest.builder()
                .auditId(audit.getId())
                .auditControlId(auditControlId)
                .title("Control gap resolved")
                .status(FindingStatus.RESOLVED)
                .severity(FindingSeverity.MEDIUM)
                .ownerUserId(owner.getId())
                .dueAt(null)
                .build());

        assertThat(updated.getStatus()).isEqualTo(FindingStatus.RESOLVED);
        assertThat(updated.getResolvedAt()).isNotNull();
        assertThat(updated.getTitle()).contains("resolved");
        assertThat(updated.getDueAt()).isNull();

        List<FindingDto> listed = findingService.list(audit.getId());
        assertThat(listed).extracting(FindingDto::getId).contains(created.getId());
        assertThat(listed.stream().filter(f -> f.getId().equals(created.getId())).findFirst().orElseThrow().getLinkedExceptionCount())
                .isZero();
    }

    @Test
    void controlExceptionCanLinkToFindingAndUpdatesFindingExceptionCount() {
        User admin = userRepository.save(User.builder()
                .email("link-admin@test.com")
                .passwordHash("x")
                .displayName("Link Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("link-owner@test.com")
                .passwordHash("x")
                .displayName("Link Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Link Test App")
                .description("test")
                .owner(owner)
                .build());

        authenticate(admin.getEmail());
        AuditDto audit = auditService.create(app.getId(), 2047);
        Long auditControlId = auditControlRepository.findByAuditId(audit.getId()).get(0).getId();

        FindingDto finding = findingService.create(FindingUpsertRequest.builder()
                .auditId(audit.getId())
                .auditControlId(auditControlId)
                .title("Needs temporary exception")
                .severity(FindingSeverity.MEDIUM)
                .build());

        ControlExceptionDto ex = controlExceptionService.request(ControlExceptionCreateRequest.builder()
                .auditId(audit.getId())
                .findingId(finding.getId())
                .reason("Compensating control while patching")
                .expiresAt(Instant.now().plusSeconds(86400))
                .build());

        assertThat(ex.getFindingId()).isEqualTo(finding.getId());
        assertThat(ex.getFindingTitle()).contains("temporary exception");

        List<FindingDto> afterLink = findingService.list(audit.getId());
        assertThat(afterLink.stream().filter(f -> f.getId().equals(finding.getId())).findFirst().orElseThrow().getLinkedExceptionCount())
                .isEqualTo(1);

        List<ControlExceptionDto> forFinding = controlExceptionService.list(audit.getId(), finding.getId());
        assertThat(forFinding).extracting(ControlExceptionDto::getId).contains(ex.getId());
    }

    @Test
    void controlExceptionRequestApproveAndExpireFlowWorks() {
        User admin = userRepository.save(User.builder()
                .email("exception-admin@test.com")
                .passwordHash("x")
                .displayName("Exception Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("exception-owner@test.com")
                .passwordHash("x")
                .displayName("Exception Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Exception Test App")
                .description("test")
                .owner(owner)
                .build());

        authenticate(admin.getEmail());
        AuditDto audit = auditService.create(app.getId(), 2046);
        Long auditControlId = auditControlRepository.findByAuditId(audit.getId()).get(0).getId();

        ControlExceptionDto requested = controlExceptionService.request(ControlExceptionCreateRequest.builder()
                .auditId(audit.getId())
                .auditControlId(auditControlId)
                .reason("Temporary compensating control in place")
                .compensatingControl("Manual review by security team")
                .expiresAt(Instant.now().minusSeconds(60))
                .build());
        assertThat(requested.getStatus()).isEqualTo(ControlExceptionStatus.REQUESTED);

        ControlExceptionDto approved = controlExceptionService.approve(requested.getId(), ControlExceptionDecisionRequest.builder()
                .decisionNotes("Approved for emergency change window")
                .expiresAt(Instant.now().minusSeconds(30))
                .build());
        assertThat(approved.getStatus()).isEqualTo(ControlExceptionStatus.APPROVED);

        List<ControlExceptionDto> listed = controlExceptionService.list(audit.getId(), null);
        ControlExceptionDto expired = listed.stream()
                .filter(it -> it.getId().equals(requested.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(ControlExceptionStatus.EXPIRED);
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
