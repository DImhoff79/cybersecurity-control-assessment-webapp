package com.cyberassessment.controller;

import com.cyberassessment.dto.ApplicationDto;
import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.IntakeAnswerSubmitDto;
import com.cyberassessment.dto.IntakeWizardStepDto;
import com.cyberassessment.service.ApplicationService;
import com.cyberassessment.service.AuditService;
import com.cyberassessment.service.CurrentUserService;
import com.cyberassessment.service.IntakeWizardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Application owners register a new application and start their assessment (triage + CCF questionnaire).
 */
@RestController
@RequestMapping("/api/application-intake")
@RequiredArgsConstructor
public class ApplicationIntakeController {

    private final ApplicationService applicationService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final IntakeWizardService intakeWizardService;

    @GetMapping("/wizard")
    @PreAuthorize("hasRole('APPLICATION_OWNER')")
    public List<IntakeWizardStepDto> wizardSteps() {
        return intakeWizardService.loadWizardSteps();
    }

    @PostMapping
    @PreAuthorize("hasRole('APPLICATION_OWNER')")
    public ResponseEntity<?> intake(@RequestBody Map<String, Object> body) {
        try {
            Long ownerId = currentUserService.getCurrentUser()
                    .orElseThrow(() -> new IllegalArgumentException("Not authenticated"))
                    .getId();
            body.put("ownerId", ownerId);
            List<IntakeAnswerSubmitDto> intakeAnswers = parseIntakeAnswers(body.get("intakeAnswers"));
            ApplicationDto app = applicationService.create(body);
            if (!intakeAnswers.isEmpty()) {
                applicationService.replaceIntakeAnswers(app.getId(), intakeAnswers);
            }
            int year = Year.now().getValue();
            if (body.get("assessmentYear") instanceof Number n) {
                year = n.intValue();
            } else if (body.get("assessmentYear") != null) {
                year = Integer.parseInt(body.get("assessmentYear").toString());
            }
            AuditDto audit = auditService.createOwnerIntakeAudit(app.getId(), year);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "application", app,
                    "audit", audit
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static List<IntakeAnswerSubmitDto> parseIntakeAnswers(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<IntakeAnswerSubmitDto> out = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) {
                continue;
            }
            Object qid = m.get("questionId");
            if (qid == null) {
                continue;
            }
            long questionId = qid instanceof Number ? ((Number) qid).longValue() : Long.parseLong(String.valueOf(qid));
            Object at = m.get("answerText");
            out.add(IntakeAnswerSubmitDto.builder()
                    .questionId(questionId)
                    .answerText(at != null ? String.valueOf(at) : null)
                    .build());
        }
        return out;
    }
}
