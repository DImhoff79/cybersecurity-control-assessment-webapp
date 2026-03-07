package com.cyberassessment.service;

import com.cyberassessment.dto.AuditControlAnswerDto;
import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.AuditQuestionItemDto;
import com.cyberassessment.dto.SubmitAnswersRequest;
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

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAssignSendAndAnswerFlowWorks() {
        User owner = userRepository.save(User.builder()
                .email("owner-audit-service@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .build());

        Application app = applicationRepository.save(Application.builder()
                .name("Audit Service App")
                .description("test")
                .owner(owner)
                .build());

        AuditDto created = auditService.create(app.getId(), 2031);
        assertThat(created.getStatus()).isEqualTo(AuditStatus.DRAFT);
        assertThat(auditControlRepository.findByAuditId(created.getId())).isNotEmpty();

        AuditDto assigned = auditService.assign(created.getId(), owner.getId());
        assertThat(assigned.getAssignedToUserId()).isEqualTo(owner.getId());
        assertThat(assigned.getAssignedAt()).isNotNull();

        AuditDto sent = auditService.sendToOwner(created.getId());
        assertThat(sent.getStatus()).isEqualTo(AuditStatus.IN_PROGRESS);
        assertThat(sent.getSentAt()).isNotNull();
        assertThat(sent.getStartedAt()).isNotNull();

        authenticate(owner.getEmail());

        List<AuditQuestionItemDto> questions = auditService.getQuestionsForAudit(created.getId());
        assertThat(questions).isNotEmpty();
        assertThat(questions).allMatch(q -> q.getQuestionText() != null && !q.getQuestionText().isBlank());

        SubmitAnswersRequest request = new SubmitAnswersRequest(List.of(
                new SubmitAnswersRequest.AnswerItem(
                        questions.get(0).getQuestionId(),
                        questions.get(0).getAuditControlId(),
                        "YES"
                )
        ));
        auditService.submitAnswers(created.getId(), request);

        assertThat(auditControlAnswerRepository.findByAuditControlIdAndQuestionId(
                questions.get(0).getAuditControlId(),
                questions.get(0).getQuestionId()
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
        User owner = userRepository.save(User.builder()
                .email("owner-access@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .build());
        User other = userRepository.save(User.builder()
                .email("other-access@test.com")
                .passwordHash("x")
                .displayName("Other")
                .role(UserRole.APPLICATION_OWNER)
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Access App")
                .description("test")
                .owner(owner)
                .build());

        AuditDto created = auditService.create(app.getId(), 2032);
        auditService.assign(created.getId(), owner.getId());

        authenticate(other.getEmail());
        assertThatThrownBy(() -> auditService.getQuestionsForAudit(created.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    void submitAuditMarksSubmittedWhenComplete() {
        User owner = userRepository.save(User.builder()
                .email("owner-submit@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .build());
        Application app = applicationRepository.save(Application.builder()
                .name("Submit App")
                .description("test")
                .owner(owner)
                .build());

        AuditDto created = auditService.create(app.getId(), 2034);
        auditService.assign(created.getId(), owner.getId());
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
        assertThat(submitted.getStatus()).isEqualTo(AuditStatus.SUBMITTED);
        assertThat(submitted.getCompletedAt()).isNotNull();
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
