package com.cyberassessment.service;

import com.cyberassessment.dto.AuditQuestionItemDto;
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
        "spring.datasource.url=jdbc:h2:mem:assessment_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class AuditQuestionVisibilityAndDeleteTest {

    @Autowired
    private AuditService auditService;
    @Autowired
    private QuestionService questionService;
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
    private QuestionRepository questionRepository;
    @Autowired
    private QuestionControlMappingRepository questionControlMappingRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hiddenOwnerQuestionsAreExcludedFromOwnerAuditFeed() {
        User owner = userRepository.save(User.builder()
                .email("owner-visibility@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());

        Application app = applicationRepository.save(Application.builder()
                .name("Visibility App")
                .description("test")
                .owner(owner)
                .build());

        Audit audit = auditRepository.save(Audit.builder()
                .application(app)
                .year(2026)
                .status(AuditStatus.IN_PROGRESS)
                .assignedTo(owner)
                .build());

        Control control = controlRepository.save(Control.builder()
                .controlId("TEST-CONTROL-1")
                .name("Test Control")
                .description("desc")
                .framework(ControlFramework.NIST_800_53_LOW)
                .enabled(true)
                .category("Test")
                .build());

        AuditControl auditControl = auditControlRepository.save(AuditControl.builder()
                .audit(audit)
                .control(control)
                .status(ControlAssessmentStatus.NOT_STARTED)
                .build());

        Question shownQuestion = questionRepository.save(Question.builder()
                .control(control)
                .questionText("Shown question?")
                .displayOrder(0)
                .askOwner(true)
                .build());

        Question hiddenQuestion = questionRepository.save(Question.builder()
                .control(control)
                .questionText("Hidden question?")
                .displayOrder(1)
                .askOwner(true)
                .build());

        questionControlMappingRepository.save(QuestionControlMapping.builder()
                .id(new QuestionControlId(shownQuestion.getId(), control.getId()))
                .question(shownQuestion)
                .control(control)
                .build());
        questionControlMappingRepository.save(QuestionControlMapping.builder()
                .id(new QuestionControlId(hiddenQuestion.getId(), control.getId()))
                .question(hiddenQuestion)
                .control(control)
                .build());

        authenticate(owner.getEmail());

        questionService.update(control.getId(), hiddenQuestion.getId(), null, null, null, false);

        List<AuditQuestionItemDto> items = auditService.getQuestionsForAudit(audit.getId());
        assertThat(items).extracting(AuditQuestionItemDto::getQuestionText)
                .contains("Shown question?")
                .doesNotContain("Hidden question?");
        assertThat(items).extracting(AuditQuestionItemDto::getAuditControlId)
                .containsOnly(auditControl.getId());
    }

    @Test
    void onlyAdminsCanDeleteAudits() {
        User admin = userRepository.save(User.builder()
                .email("admin-delete@test.com")
                .passwordHash("x")
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .permissions(UserRole.ADMIN.defaultPermissions())
                .build());
        User owner = userRepository.save(User.builder()
                .email("owner-delete@test.com")
                .passwordHash("x")
                .displayName("Owner")
                .role(UserRole.APPLICATION_OWNER)
                .permissions(UserRole.APPLICATION_OWNER.defaultPermissions())
                .build());

        Application app = applicationRepository.save(Application.builder()
                .name("Delete App")
                .description("test")
                .owner(owner)
                .build());

        Audit audit = auditRepository.save(Audit.builder()
                .application(app)
                .year(2027)
                .status(AuditStatus.DRAFT)
                .assignedTo(owner)
                .build());

        authenticate(owner.getEmail());
        assertThatThrownBy(() -> auditService.delete(audit.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing permission: AUDIT_MANAGEMENT");

        authenticate(admin.getEmail());
        auditService.delete(audit.getId());
        assertThat(auditRepository.findById(audit.getId())).isEmpty();
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
