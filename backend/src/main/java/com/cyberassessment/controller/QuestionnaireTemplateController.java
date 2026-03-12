package com.cyberassessment.controller;

import com.cyberassessment.dto.QuestionnaireTemplateDto;
import com.cyberassessment.dto.QuestionnaireTemplateItemDto;
import com.cyberassessment.service.QuestionnaireTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questionnaire-templates")
@RequiredArgsConstructor
public class QuestionnaireTemplateController {
    private final QuestionnaireTemplateService questionnaireTemplateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUDIT_MANAGER')")
    public List<QuestionnaireTemplateDto> list() {
        return questionnaireTemplateService.listTemplates();
    }

    @GetMapping("/{templateId}/items")
    @PreAuthorize("hasAnyRole('ADMIN','AUDIT_MANAGER')")
    public List<QuestionnaireTemplateItemDto> items(@PathVariable Long templateId) {
        return questionnaireTemplateService.listItems(templateId);
    }

    @PostMapping("/draft-from-current")
    @PreAuthorize("hasAnyRole('ADMIN','AUDIT_MANAGER')")
    public QuestionnaireTemplateDto createDraft(@RequestBody(required = false) Map<String, Object> body) {
        String notes = body != null && body.containsKey("notes") ? (String) body.get("notes") : null;
        return questionnaireTemplateService.createDraftFromCurrent(notes);
    }

    @PostMapping("/bootstrap-initial")
    @PreAuthorize("hasAnyRole('ADMIN','AUDIT_MANAGER')")
    public QuestionnaireTemplateDto bootstrapInitial(@RequestBody(required = false) Map<String, Object> body) {
        String notes = body != null && body.containsKey("notes") ? (String) body.get("notes") : null;
        return questionnaireTemplateService.bootstrapInitialFromCurrent(notes);
    }

    @PostMapping("/{templateId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','AUDIT_MANAGER')")
    public QuestionnaireTemplateDto publish(@PathVariable Long templateId) {
        return questionnaireTemplateService.publish(templateId);
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN','AUDIT_MANAGER')")
    public ResponseEntity<Void> deleteDraft(@PathVariable Long templateId) {
        questionnaireTemplateService.deleteDraft(templateId);
        return ResponseEntity.noContent().build();
    }
}
