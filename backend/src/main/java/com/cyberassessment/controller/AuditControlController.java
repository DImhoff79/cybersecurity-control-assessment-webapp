package com.cyberassessment.controller;

import com.cyberassessment.dto.AuditControlDto;
import com.cyberassessment.dto.AuditControlAssignmentDto;
import com.cyberassessment.entity.AuditControlAssignmentRole;
import com.cyberassessment.entity.ControlAssessmentStatus;
import com.cyberassessment.service.AuditControlService;
import com.cyberassessment.dto.MyTaskDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("/my-tasks")
    public List<MyTaskDto> myTasks() {
        return auditControlService.myTasks();
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

    @GetMapping("/{id}/assignments")
    public ResponseEntity<List<AuditControlAssignmentDto>> listAssignments(@PathVariable Long id) {
        return ResponseEntity.ok(auditControlService.listAssignments(id));
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("@currentUserService.isAdmin()")
    public ResponseEntity<List<AuditControlAssignmentDto>> addAssignment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
        AuditControlAssignmentRole role = body.get("role") != null
                ? AuditControlAssignmentRole.valueOf(body.get("role").toString())
                : AuditControlAssignmentRole.CONTRIBUTOR;
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(auditControlService.addAssignment(id, userId, role));
    }

    @DeleteMapping("/{id}/assignments/{assignmentId}")
    @PreAuthorize("@currentUserService.isAdmin()")
    public ResponseEntity<List<AuditControlAssignmentDto>> removeAssignment(@PathVariable Long id, @PathVariable Long assignmentId) {
        return ResponseEntity.ok(auditControlService.removeAssignment(id, assignmentId));
    }
}
