package com.cyberassessment.service;

import com.cyberassessment.dto.AuditControlAnswerDto;
import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.AuditQuestionItemDto;
import com.cyberassessment.dto.SubmitAnswersRequest;
import com.cyberassessment.dto.FindingUpsertRequest;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:audit_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class AuditServiceIntegrationTest {

    @Autowired
    private AuditService auditService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private AuditRepository auditRepository;
    @Autowired
    private AuditControlRepository auditControlRepository;
    @Autowired
    private AuditControlAnswerRepository auditControlAnswerRepository;
    @Autowired
    private FindingService findingService;
    @Autowired
    private AuditApprovalStepRepository auditApprovalStepRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAssignSendAndAnswerFlowWorks() {
        User admin = userRepository.save(User.builder()
                .email("admin-audit-service@test.com")
                .passwordHash("x")
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("owner-audit-service@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());
        User auditor = userRepository.save(User.builder()
                .email("auditor-audit-service@test.com")
                .passwordHash("x")
                .displayName("Auditor")
                .role(UserRole.AUDITOR)
                .permissions(UserRole.AUDITOR.defaultPermissions())
                .build());

        Application app = applicationRepository.save(Application.builder()
                .name("Audit Service App")
                .description("test")
                .owner(owner)
                .build());

        authenticate(admin.getEmail());
        AuditDto created = auditService.create(app.getId(), 2031);
        assertThat(created.getStatus()).isEqualTo(AuditStatus.DRAFT);
        assertThat(auditControlRepository.findByAuditId(created.getId())).isNotEmpty();

        AuditDto assigned = auditService.assign(created.getId(), auditor.getId());
        assertThat(assigned.getAssignedToUserId()).isEqualTo(auditor.getId());
        assertThat(assigned.getAssignedAt()).isNotNull();

        AuditDto sent = auditService.sendToOwner(created.getId());
        assertThat(sent.getStatus()).isEqualTo(AuditStatus.IN_PROGRESS);
        assertThat(sent.getSentAt()).isNotNull();
        assertThat(sent.getStartedAt()).isNotNull();

        authenticate(owner.getEmail());

        List<AuditQuestionItemDto> questions = auditService.getQuestionsForAudit(created.getId());
        assertThat(questions).isNotEmpty();
        assertThat(questions).allMatch(q -> q.getQuestionText() != null && !q.getQuestionText().isBlank());

        AuditQuestionItemDto answerable = questions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getAskOwner()))
                .findFirst()
                .orElseThrow();

        SubmitAnswersRequest request = new SubmitAnswersRequest(List.of(
                new SubmitAnswersRequest.AnswerItem(
                        answerable.getQuestionId(),
                        answerable.getAuditControlId(),
                        "YES"
                )
        ));
        auditService.submitAnswers(created.getId(), request);

        assertThat(auditControlAnswerRepository.findByAuditControlIdAndQuestionId(
                answerable.getAuditControlId(),
                answerable.getQuestionId()
        )).isPresent();

        List<AuditControlAnswerDto> answers = auditService.getAnswersForAudit(created.getId());
        if (!answers.isEmpty()) {
            assertThat(answers.get(0).getAnswerText()).isEqualTo("YES");
        }

        Audit updatedAudit = auditRepository.findById(created.getId()).orElseThrow();
        assertThat(updatedAudit.getStatus()).isEqualTo(AuditStatus.IN_PROGRESS);
    }

    @Test
    void ownerAccessControlIsEnforced() {
        User admin = userRepository.save(User.builder()
                .email("admin-access@test.com")
                .passwordHash("x")
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("owner-access@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());
        User other = userRepository.save(User.builder()
                .email("other-access@test.com")
                .passwordHash("x")
                .displayName("Other")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());
        User auditor = userRepository.save(User.builder()
                .email("auditor-access@test.com")
                .passwordHash("x")
                .displayName("Auditor")
                .role(UserRole.AUDITOR)
                .permissions(UserRole.AUDITOR.defaultPermissions())
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Access App")
                .description("test")
                .owner(owner)
                .build());

        authenticate(admin.getEmail());
        AuditDto created = auditService.create(app.getId(), 2032);
        auditService.assign(created.getId(), auditor.getId());

        authenticate(other.getEmail());
        assertThatThrownBy(() -> auditService.getQuestionsForAudit(created.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    void submitAuditMarksSubmittedWhenComplete() {
        User admin = userRepository.save(User.builder()
                .email("admin-submit@test.com")
                .passwordHash("x")
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("owner-submit@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());
        User auditor = userRepository.save(User.builder()
                .email("auditor-submit@test.com")
                .passwordHash("x")
                .displayName("Auditor")
                .role(UserRole.AUDITOR)
                .permissions(UserRole.AUDITOR.defaultPermissions())
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Submit App")
                .description("test")
                .owner(owner)
                .build());

        User leadAuditor = userRepository.save(User.builder()
                .email("lead-auditor-submit@test.com")
                .passwordHash("x")
                .displayName("Lead Auditor")
                .role(UserRole.AUDITOR)
                .permissions(UserRole.AUDITOR.defaultPermissions())
                .build());

        authenticate(admin.getEmail());
        AuditDto created = auditService.create(app.getId(), 2034);
        auditService.assign(created.getId(), leadAuditor.getId());
        auditService.addAssignment(created.getId(), auditor.getId(), AuditAssignmentRole.REVIEWER);
        auditService.sendToOwner(created.getId());
        authenticate(owner.getEmail());

        List<AuditQuestionItemDto> questions = auditService.getQuestionsForAudit(created.getId());
        SubmitAnswersRequest request = new SubmitAnswersRequest(
                questions.stream()
                        .map(q -> new SubmitAnswersRequest.AnswerItem(q.getQuestionId(), q.getAuditControlId(), "YES"))
                        .toList()
        );
        auditService.submitAnswers(created.getId(), request);

        AuditDto submitted = auditService.submitAudit(created.getId());
        assertThat(submitted.getStatus()).isEqualTo(AuditStatus.PENDING_APPROVAL);
        assertThat(submitted.getCompletedAt()).isNotNull();
        assertThat(submitted.getPendingAuditorUserId()).isEqualTo(auditor.getId());
        assertThat(submitted.getPendingAuditorEmail()).isEqualTo(auditor.getEmail());
        assertThat(submitted.getPendingAuditorDisplayName()).isEqualTo(auditor.getDisplayName());

        AuditDto loaded = auditService.findById(created.getId());
        assertThat(loaded.getPendingAuditorUserId()).isEqualTo(auditor.getId());
        assertThat(loaded.getPendingAuditorEmail()).isEqualTo(auditor.getEmail());

        auditApprovalStepRepository.deleteByAudit_Id(created.getId());
        AuditDto legacyNoSteps = auditService.findById(created.getId());
        assertThat(legacyNoSteps.getPendingAuditorUserId()).isEqualTo(auditor.getId());
        assertThat(legacyNoSteps.getPendingAuditorEmail()).isEqualTo(auditor.getEmail());
    }

    @Test
    void bulkAssignAssignsMultipleAudits() {
        User admin = userRepository.save(User.builder()
                .email("bulk-admin@test.com")
                .passwordHash("x")
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("bulk-owner@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());
        User auditor = userRepository.save(User.builder()
                .email("bulk-auditor@test.com")
                .passwordHash("x")
                .displayName("Auditor")
                .role(UserRole.AUDITOR)
                .permissions(UserRole.AUDITOR.defaultPermissions())
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Bulk App")
                .description("test")
                .owner(owner)
                .build());

        authenticate(admin.getEmail());
        AuditDto a1 = auditService.create(app.getId(), 2040);
        AuditDto a2 = auditService.create(app.getId(), 2041);

        List<AuditDto> updated = auditService.bulkAssign(List.of(a1.getId(), a2.getId()), auditor.getId(), true);
        assertThat(updated).hasSize(2);
        assertThat(updated).allMatch(a -> a.getAssignedToUserId().equals(auditor.getId()));
        assertThat(updated).allMatch(a -> a.getSentAt() != null);
        assertThat(updated).allMatch(a -> a.getStatus() == AuditStatus.IN_PROGRESS || a.getStatus() == AuditStatus.DRAFT);
    }

    @Test
    void delegateAssignmentGrantsAuditAccess() {
        User admin = userRepository.save(User.builder()
                .email("delegate-admin@test.com")
                .passwordHash("x")
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("delegate-owner@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());
        User delegate = userRepository.save(User.builder()
                .email("delegate-user@test.com")
                .passwordHash("x")
                .displayName("Delegate")
                .role(UserRole.AUDITOR)
                .permissions(UserRole.AUDITOR.defaultPermissions())
                .build());
        User leadAuditor = userRepository.save(User.builder()
                .email("delegate-lead@test.com")
                .passwordHash("x")
                .displayName("Lead")
                .role(UserRole.AUDITOR)
                .permissions(UserRole.AUDITOR.defaultPermissions())
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Delegate App")
                .description("test")
                .owner(owner)
                .build());

        authenticate(admin.getEmail());
        AuditDto created = auditService.create(app.getId(), 2042);
        auditService.assign(created.getId(), leadAuditor.getId());
        auditService.addAssignment(created.getId(), delegate.getId(), AuditAssignmentRole.DELEGATE);

        authenticate(delegate.getEmail());
        List<AuditQuestionItemDto> questions = auditService.getQuestionsForAudit(created.getId());
        assertThat(questions).isNotEmpty();
        List<AuditDto> myAudits = auditService.findMyAudits();
        assertThat(myAudits.stream().map(AuditDto::getId)).contains(created.getId());
    }

    @Test
    void findingOwnerGetsMyAuditsAndWorkspaceAuditAccessWithoutBeingApplicationOwner() {
        User admin = userRepository.save(User.builder()
                .email("fo-admin@test.com")
                .passwordHash("x")
                .displayName("FO Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        User findingOwner = userRepository.save(User.builder()
                .email("finding-owner-only@test.com")
                .passwordHash("x")
                .displayName("Finding Owner Only")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Finding Owner Access App")
                .description("test")
                .owner(admin)
                .build());

        authenticate(admin.getEmail());
        AuditDto audit = auditService.create(app.getId(), 2051);
        Long auditControlId = auditControlRepository.findByAuditId(audit.getId()).get(0).getId();
        findingService.create(FindingUpsertRequest.builder()
                .auditId(audit.getId())
                .auditControlId(auditControlId)
                .title("Owner finding")
                .description("test")
                .severity(FindingSeverity.MEDIUM)
                .ownerUserId(findingOwner.getId())
                .dueAt(null)
                .build());

        authenticate(findingOwner.getEmail());
        assertThat(auditService.findMyAudits().stream().map(AuditDto::getId)).contains(audit.getId());
        assertThat(auditService.findAccessibleAuditIdsForCurrentUser()).contains(audit.getId());
        auditService.assertCanAccessAudit(audit.getId());
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
