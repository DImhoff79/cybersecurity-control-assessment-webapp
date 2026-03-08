package com.cyberassessment.service;

import com.cyberassessment.dto.QuestionnaireTemplateDto;
import com.cyberassessment.entity.QuestionnaireTemplateStatus;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.UserRepository;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:template_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class QuestionnaireTemplateServiceIntegrationTest {

    @Autowired
    private QuestionnaireTemplateService questionnaireTemplateService;
    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDraftAndPublishTemplateFlowWorks() {
        User admin = userRepository.save(User.builder()
                .email("template-admin@test.com")
                .passwordHash("x")
                .displayName("Template Admin")
                .role(UserRole.ADMIN)
                .build());
        authenticate(admin.getEmail());

        QuestionnaireTemplateDto draft = questionnaireTemplateService.createDraftFromCurrent("Quarterly update");
        assertThat(draft.getVersionNo()).isPositive();
        assertThat(draft.getStatus()).isEqualTo(QuestionnaireTemplateStatus.DRAFT);
        assertThat(draft.getItemCount()).isGreaterThan(0);

        QuestionnaireTemplateDto published = questionnaireTemplateService.publish(draft.getId());
        assertThat(published.getStatus()).isEqualTo(QuestionnaireTemplateStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();

        List<QuestionnaireTemplateDto> templates = questionnaireTemplateService.listTemplates();
        assertThat(templates).isNotEmpty();
        assertThat(templates.get(0).getVersionNo()).isEqualTo(published.getVersionNo());
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", Collections.emptyList())
        );
    }
}
