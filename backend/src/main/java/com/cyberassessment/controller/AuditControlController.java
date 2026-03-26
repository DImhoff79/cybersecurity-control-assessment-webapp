package com.cyberassessment.controller;

import com.cyberassessment.dto.AuditControlDto;
import com.cyberassessment.entity.ControlAssessmentStatus;
import com.cyberassessment.service.AuditControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-controls")
@RequiredArgsConstructor
public class AuditControlController {

    private final AuditControlService auditControlService;

    @GetMapping
    public List<AuditControlDto> listByAudit(@RequestParam Long auditId) {
        return auditControlService.findByAuditId(auditId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuditControlDto> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String statusStr = body.containsKey("status") ? (String) body.get("status") : null;
        ControlAssessmentStatus status = statusStr != null ? ControlAssessmentStatus.valueOf(statusStr) : null;
        String evidence = body.containsKey("evidence") ? (String) body.get("evidence") : null;
        String notes = body.containsKey("notes") ? (String) body.get("notes") : null;
        try {
            AuditControlDto updated = auditControlService.update(id, status, evidence, notes);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
