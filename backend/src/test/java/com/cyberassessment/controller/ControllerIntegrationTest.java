package com.cyberassessment.controller;

import com.cyberassessment.dto.ControlDto;
import com.cyberassessment.dto.UserDto;
import com.cyberassessment.entity.ControlFramework;
import com.cyberassessment.repository.ControlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class ControllerIntegrationTest {

    @Autowired
    private ControlController controlController;
    @Autowired
    private UserController userController;
    @Autowired
    private QuestionController questionController;
    @Autowired
    private ApplicationController applicationController;
    @Autowired
    private ControlRepository controlRepository;

    @Test
    void controlAndUserAndQuestionFlowsWork() {
        List<ControlDto> controls = controlController.list(ControlFramework.NIST_800_53_LOW, true, false);
        assertThat(controls).isNotEmpty();

        Long existingControlId = controls.get(0).getId();
        assertThat(controlController.get(existingControlId, true).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controlController.get(999999L, true).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<?> missingFields = userController.create(Map.of("email", "unit@test.com"));
        assertThat(missingFields.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<?> created = userController.create(Map.of(
                "email", "unit@test.com",
                "password", "pw",
                "displayName", "Unit User",
                "role", "APPLICATION_OWNER"
        ));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UserDto dto = (UserDto) created.getBody();
        assertThat(dto).isNotNull();
        assertThat(userController.get(dto.getId()).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<?> duplicate = userController.create(Map.of(
                "email", "unit@test.com",
                "password", "pw"
        ));
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<?> createdQuestion = questionController.create(existingControlId, Map.of(
                "questionText", "Is this question added by integration test?",
                "askOwner", true
        ));
        assertThat(createdQuestion.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void applicationAuditValidationBranchesWork() {
        ResponseEntity<?> missingYear = applicationController.createAudit(1L, Map.of());
        assertThat(missingYear.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<?> invalidYear = applicationController.createAudit(1L, Map.of("year", 1900));
        assertThat(invalidYear.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Long appId = applicationController.create(Map.of(
                "name", "Controller Integration App",
                "description", "test"
        )).getBody().getId();
        ResponseEntity<?> validAudit = applicationController.createAudit(appId, Map.of("year", 2030));
        assertThat(validAudit.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
