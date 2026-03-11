package com.cyberassessment.controller;

import com.cyberassessment.dto.QuestionnaireTemplateDto;
import com.cyberassessment.dto.QuestionnaireTemplateItemDto;
import com.cyberassessment.service.QuestionnaireTemplateService;
import lombok.RequiredArgsConstructor;
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
    @PreAuthorize("@currentUserService.isAdmin()")
    public List<QuestionnaireTemplateDto> list() {
        return questionnaireTemplateService.listTemplates();
    }

    @GetMapping("/{templateId}/items")
    @PreAuthorize("@currentUserService.isAdmin()")
    public List<QuestionnaireTemplateItemDto> items(@PathVariable Long templateId) {
        return questionnaireTemplateService.listItems(templateId);
    }

    @PostMapping("/draft-from-current")
    @PreAuthorize("@currentUserService.isAdmin()")
    public QuestionnaireTemplateDto createDraft(@RequestBody(required = false) Map<String, Object> body) {
        String notes = body != null && body.containsKey("notes") ? (String) body.get("notes") : null;
        return questionnaireTemplateService.createDraftFromCurrent(notes);
    }

    @PostMapping("/bootstrap-initial")
    @PreAuthorize("@currentUserService.isAdmin()")
    public QuestionnaireTemplateDto bootstrapInitial(@RequestBody(required = false) Map<String, Object> body) {
        String notes = body != null && body.containsKey("notes") ? (String) body.get("notes") : null;
        return questionnaireTemplateService.bootstrapInitialFromCurrent(notes);
    }

    @PostMapping("/{templateId}/publish")
    @PreAuthorize("@currentUserService.isAdmin()")
    public QuestionnaireTemplateDto publish(@PathVariable Long templateId) {
        return questionnaireTemplateService.publish(templateId);
    }
}
