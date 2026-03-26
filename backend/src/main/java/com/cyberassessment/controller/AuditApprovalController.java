package com.cyberassessment.controller;

import com.cyberassessment.dto.AuditApprovalStepDto;
import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.ReplaceAuditApprovalWorkflowRequest;
import com.cyberassessment.service.AuditApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audits/{auditId}/approval")
@RequiredArgsConstructor
public class AuditApprovalController {

    private final AuditApprovalService auditApprovalService;

    @GetMapping("/workflow")
    public ResponseEntity<List<AuditApprovalStepDto>> getWorkflow(@PathVariable Long auditId) {
        try {
            return ResponseEntity.ok(auditApprovalService.getWorkflow(auditId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/workflow")
    public ResponseEntity<List<AuditApprovalStepDto>> replaceWorkflow(
            @PathVariable Long auditId,
            @RequestBody ReplaceAuditApprovalWorkflowRequest body) {
        try {
            return ResponseEntity.ok(auditApprovalService.replaceWorkflow(auditId, body.getAssigneeUserIds()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/approve")
    public ResponseEntity<AuditDto> approve(@PathVariable Long auditId, @RequestBody(required = false) Map<String, Object> body) {
        String notes = body != null && body.get("notes") != null ? body.get("notes").toString() : null;
        try {
            return ResponseEntity.ok(auditApprovalService.approve(auditId, notes));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/return")
    public ResponseEntity<AuditDto> returnForRevision(@PathVariable Long auditId, @RequestBody Map<String, Object> body) {
        String notes = body != null && body.get("notes") != null ? body.get("notes").toString() : null;
        try {
            return ResponseEntity.ok(auditApprovalService.requestRevision(auditId, notes));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
