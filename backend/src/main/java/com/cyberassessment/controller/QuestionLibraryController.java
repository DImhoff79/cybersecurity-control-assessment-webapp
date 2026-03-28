package com.cyberassessment.controller;

import com.cyberassessment.dto.QuestionDto;
import com.cyberassessment.dto.QuestionLibraryRowDto;
import com.cyberassessment.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionLibraryController {

    private final QuestionService questionService;

    @GetMapping
    public List<QuestionLibraryRowDto> list() {
        return questionService.listLibrary();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<QuestionDto> createUnmapped(@RequestBody Map<String, Object> body) {
        String questionText = (String) body.get("questionText");
        Integer displayOrder = body.get("displayOrder") != null ? ((Number) body.get("displayOrder")).intValue() : null;
        String helpText = (String) body.get("helpText");
        Boolean askOwner = body.containsKey("askOwner") ? (Boolean) body.get("askOwner") : null;
        if (questionText == null || questionText.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            QuestionDto created = questionService.createUnmapped(questionText, displayOrder, helpText, askOwner);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<Void> deleteUnmapped(@PathVariable Long id) {
        try {
            questionService.deleteUnmapped(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
