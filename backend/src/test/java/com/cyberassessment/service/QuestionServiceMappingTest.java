package com.cyberassessment.service;

import com.cyberassessment.dto.QuestionDto;
import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.ControlFramework;
import com.cyberassessment.entity.Question;
import com.cyberassessment.repository.ControlRepository;
import com.cyberassessment.repository.QuestionControlMappingRepository;
import com.cyberassessment.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:question_service_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class QuestionServiceMappingTest {

    @Autowired
    private QuestionService questionService;
    @Autowired
    private ControlRepository controlRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private QuestionControlMappingRepository questionControlMappingRepository;

    @Test
    void createReusesCanonicalQuestionAcrossControls() {
        Control c1 = createControl("MAP-1");
        Control c2 = createControl("MAP-2");

        QuestionDto q1 = questionService.create(
                c1.getId(),
                "Do you review access regularly?",
                0,
                "Review at least annually.",
                true
        );
        QuestionDto q2 = questionService.create(
                c2.getId(),
                "Do you review access regularly?",
                0,
                "Review at least annually.",
                true
        );

        assertThat(q2.getId()).isEqualTo(q1.getId());
        assertThat(questionControlMappingRepository.countByQuestion_Id(q1.getId())).isEqualTo(2);
    }

    @Test
    void updateSetsAskOwnerFlag() {
        Control c1 = createControl("ASK-1");
        QuestionDto created = questionService.create(
                c1.getId(),
                "Is MFA enabled?",
                0,
                "Include remote/admin access.",
                true
        );

        QuestionDto updated = questionService.update(
                c1.getId(),
                created.getId(),
                null,
                null,
                null,
                false
        );

        assertThat(updated.getAskOwner()).isFalse();
        Question stored = questionRepository.findById(created.getId()).orElseThrow();
        assertThat(stored.getAskOwner()).isFalse();
    }

    @Test
    void deleteIsBlockedWhenControlWouldHaveNoQuestions() {
        Control c1 = createControl("DEL-1");
        QuestionDto created = questionService.create(
                c1.getId(),
                "Is logging enabled?",
                0,
                null,
                true
        );

        assertThatThrownBy(() -> questionService.delete(c1.getId(), created.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one plain-language question");
    }

    private Control createControl(String controlId) {
        return controlRepository.save(Control.builder()
                .controlId(controlId)
                .name("Control " + controlId)
                .description("Description for " + controlId)
                .framework(ControlFramework.NIST_800_53_LOW)
                .enabled(true)
                .category("Test")
                .build());
    }
}
