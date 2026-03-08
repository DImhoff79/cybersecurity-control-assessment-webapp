package com.cyberassessment.controller;

import com.cyberassessment.dto.AuditControlAnswerDto;
import com.cyberassessment.dto.AuditControlDto;
import com.cyberassessment.dto.AuditActivityLogDto;
import com.cyberassessment.dto.AuditAssignmentDto;
import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.AuditQuestionItemDto;
import com.cyberassessment.dto.SubmitAnswersRequest;
import com.cyberassessment.entity.AuditAssignmentRole;
import com.cyberassessment.entity.AuditStatus;
import com.cyberassessment.service.AuditActivityLogService;
import com.cyberassessment.service.AuditControlService;
import com.cyberassessment.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final AuditControlService auditControlService;
    private final AuditActivityLogService auditActivityLogService;

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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditDto> updateAudit(@PathVariable Long auditId, @RequestBody Map<String, Object> body) {
        String statusStr = (String) body.get("status");
        AuditStatus status = statusStr != null ? AuditStatus.valueOf(statusStr) : null;
        Instant dueAt = parseInstant(body.get("dueAt"));
        try {
            AuditDto updated = auditService.update(auditId, status, dueAt);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/audits/{auditId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAudit(@PathVariable Long auditId) {
        try {
            auditService.delete(auditId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/audits/{auditId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/audits/{auditId}/attest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditDto> attestAudit(@PathVariable Long auditId, @RequestBody(required = false) Map<String, Object> body) {
        String statement = body != null && body.containsKey("statement") ? (String) body.get("statement") : null;
        try {
            AuditDto updated = auditService.attest(auditId, statement);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/audits/{auditId}/remind")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditDto> remindAudit(@PathVariable Long auditId) {
        try {
            AuditDto updated = auditService.sendReminder(auditId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/audits/bulk-assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditDto>> bulkAssign(@RequestBody Map<String, Object> body) {
        List<Number> rawIds = (List<Number>) body.getOrDefault("auditIds", List.of());
        List<Long> auditIds = rawIds.stream().map(Number::longValue).toList();
        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
        boolean sendNow = body.get("sendNow") != null && Boolean.TRUE.equals(body.get("sendNow"));
        return ResponseEntity.ok(auditService.bulkAssign(auditIds, userId, sendNow));
    }

    @GetMapping("/audits/{auditId}/activity-logs")
    public ResponseEntity<List<AuditActivityLogDto>> getActivityLogs(@PathVariable Long auditId) {
        auditService.assertCanAccessAudit(auditId);
        return ResponseEntity.ok(auditActivityLogService.listByAuditId(auditId));
    }

    @GetMapping("/audits/{auditId}/assignments")
    public ResponseEntity<List<AuditAssignmentDto>> getAssignments(@PathVariable Long auditId) {
        return ResponseEntity.ok(auditService.listAssignments(auditId));
    }

    @PostMapping("/audits/{auditId}/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditAssignmentDto>> addAssignment(@PathVariable Long auditId, @RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
        AuditAssignmentRole role = body.get("role") != null ? AuditAssignmentRole.valueOf(body.get("role").toString()) : AuditAssignmentRole.DELEGATE;
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(auditService.addAssignment(auditId, userId, role));
    }

    @DeleteMapping("/audits/{auditId}/assignments/{assignmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditAssignmentDto>> removeAssignment(@PathVariable Long auditId, @PathVariable Long assignmentId) {
        return ResponseEntity.ok(auditService.removeAssignment(auditId, assignmentId));
    }

    private Instant parseInstant(Object val) {
        if (val == null) return null;
        return Instant.parse(val.toString());
    }
}
