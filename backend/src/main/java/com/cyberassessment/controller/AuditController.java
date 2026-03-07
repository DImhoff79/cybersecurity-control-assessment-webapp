package com.cyberassessment.controller;

import com.cyberassessment.dto.AuditControlAnswerDto;
import com.cyberassessment.dto.AuditControlDto;
import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.AuditQuestionItemDto;
import com.cyberassessment.dto.SubmitAnswersRequest;
import com.cyberassessment.entity.AuditStatus;
import com.cyberassessment.service.AuditControlService;
import com.cyberassessment.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final AuditControlService auditControlService;

    @GetMapping("/my-audits")
    public List<AuditDto> myAudits() {
        return auditService.findMyAudits();
    }

    @GetMapping("/audits/{auditId}/controls")
    public List<AuditControlDto> getAuditControls(@PathVariable Long auditId) {
        return auditControlService.findByAuditId(auditId);
    }

    @GetMapping("/audits/{auditId}")
    public ResponseEntity<AuditDto> getAudit(@PathVariable Long auditId) {
        AuditDto dto = auditService.findById(auditId);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PutMapping("/audits/{auditId}")
    public ResponseEntity<AuditDto> updateAudit(@PathVariable Long auditId, @RequestBody Map<String, Object> body) {
        String statusStr = (String) body.get("status");
        AuditStatus status = statusStr != null ? AuditStatus.valueOf(statusStr) : null;
        try {
            AuditDto updated = auditService.update(auditId, status);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/audits/{auditId}")
    public ResponseEntity<Void> deleteAudit(@PathVariable Long auditId) {
        try {
            auditService.delete(auditId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/audits/{auditId}/assign")
    public ResponseEntity<AuditDto> assign(@PathVariable Long auditId, @RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
        try {
            AuditDto updated = auditService.assign(auditId, userId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/audits/{auditId}/send")
    public ResponseEntity<AuditDto> sendToOwner(@PathVariable Long auditId) {
        try {
            AuditDto updated = auditService.sendToOwner(auditId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/audits/{auditId}/submit")
    public ResponseEntity<AuditDto> submitAudit(@PathVariable Long auditId) {
        try {
            AuditDto updated = auditService.submitAudit(auditId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/audits/{auditId}/questions")
    public List<AuditQuestionItemDto> getQuestions(@PathVariable Long auditId) {
        return auditService.getQuestionsForAudit(auditId);
    }

    @GetMapping("/audits/{auditId}/answers")
    public List<AuditControlAnswerDto> getAnswers(@PathVariable Long auditId) {
        return auditService.getAnswersForAudit(auditId);
    }

    @PostMapping("/audits/{auditId}/answers")
    public ResponseEntity<Void> submitAnswers(@PathVariable Long auditId, @RequestBody SubmitAnswersRequest request) {
        try {
            auditService.submitAnswers(auditId, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
