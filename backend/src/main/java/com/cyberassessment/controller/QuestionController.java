package com.cyberassessment.controller;

import com.cyberassessment.dto.QuestionDto;
import com.cyberassessment.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
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
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<QuestionDto> create(@PathVariable Long controlId, @RequestBody Map<String, Object> body) {
        String questionText = (String) body.get("questionText");
        Integer displayOrder = body.get("displayOrder") != null ? ((Number) body.get("displayOrder")).intValue() : null;
        String helpText = (String) body.get("helpText");
        Boolean askOwner = body.containsKey("askOwner") ? (Boolean) body.get("askOwner") : null;
        if (questionText == null || questionText.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            QuestionDto created = questionService.create(controlId, questionText, displayOrder, helpText, askOwner);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<QuestionDto> update(@PathVariable Long controlId, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String questionText = body.containsKey("questionText") ? (String) body.get("questionText") : null;
        Integer displayOrder = body.get("displayOrder") != null ? ((Number) body.get("displayOrder")).intValue() : null;
        String helpText = body.containsKey("helpText") ? (String) body.get("helpText") : null;
        Boolean askOwner = body.containsKey("askOwner") ? (Boolean) body.get("askOwner") : null;
        try {
            QuestionDto updated = questionService.update(controlId, id, questionText, displayOrder, helpText, askOwner);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/mapping")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<QuestionDto> updateMapping(@PathVariable Long controlId, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String mappingRationale = body.containsKey("mappingRationale") ? (String) body.get("mappingRationale") : null;
        BigDecimal mappingWeight = body.containsKey("mappingWeight") && body.get("mappingWeight") != null
                ? new BigDecimal(body.get("mappingWeight").toString()) : null;
        Instant effectiveFrom = body.containsKey("effectiveFrom") && body.get("effectiveFrom") != null
                ? Instant.parse(body.get("effectiveFrom").toString()) : null;
        Instant effectiveTo = body.containsKey("effectiveTo") && body.get("effectiveTo") != null
                ? Instant.parse(body.get("effectiveTo").toString()) : null;
        try {
            QuestionDto updated = questionService.updateMapping(controlId, id, mappingRationale, mappingWeight, effectiveFrom, effectiveTo);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<Void> delete(@PathVariable Long controlId, @PathVariable Long id) {
        try {
            questionService.delete(controlId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
