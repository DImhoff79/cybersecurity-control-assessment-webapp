package com.cyberassessment.controller;

import com.cyberassessment.dto.QuestionDto;
import com.cyberassessment.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/controls/{controlId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public List<QuestionDto> list(@PathVariable Long controlId) {
        return questionService.findByControlId(controlId);
    }

    @PostMapping
    public ResponseEntity<QuestionDto> create(@PathVariable Long controlId, @RequestBody Map<String, Object> body) {
        String questionText = (String) body.get("questionText");
        Integer displayOrder = body.get("displayOrder") != null ? ((Number) body.get("displayOrder")).intValue() : null;
        String helpText = (String) body.get("helpText");
        if (questionText == null || questionText.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            QuestionDto created = questionService.create(controlId, questionText, displayOrder, helpText);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionDto> update(@PathVariable Long controlId, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String questionText = body.containsKey("questionText") ? (String) body.get("questionText") : null;
        Integer displayOrder = body.get("displayOrder") != null ? ((Number) body.get("displayOrder")).intValue() : null;
        String helpText = body.containsKey("helpText") ? (String) body.get("helpText") : null;
        try {
            QuestionDto updated = questionService.update(id, questionText, displayOrder, helpText);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long controlId, @PathVariable Long id) {
        try {
            questionService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
