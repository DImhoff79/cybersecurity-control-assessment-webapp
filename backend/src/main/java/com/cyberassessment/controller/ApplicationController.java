package com.cyberassessment.controller;

import com.cyberassessment.dto.ApplicationAssessmentControlDto;
import com.cyberassessment.dto.ApplicationDto;
import com.cyberassessment.dto.ApplicationSecurityReviewSummaryDto;
import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.entity.ApplicationSecurityReviewStatus;
import com.cyberassessment.service.ApplicationAssessmentControlService;
import com.cyberassessment.service.ApplicationSecurityReviewService;
import com.cyberassessment.service.ApplicationService;
import com.cyberassessment.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final AuditService auditService;
    private final ApplicationAssessmentControlService applicationAssessmentControlService;
    private final ApplicationSecurityReviewService applicationSecurityReviewService;

    @GetMapping
    public List<ApplicationDto> list() {
        return applicationService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDto> get(@PathVariable Long id) {
        ApplicationDto dto = applicationService.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/assessment-controls")
    public ResponseEntity<?> listAssessmentControls(@PathVariable Long id) {
        try {
            List<ApplicationAssessmentControlDto> rows = applicationAssessmentControlService.listForApplication(id);
            return ResponseEntity.ok(rows);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/security-architecture-review")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<?> patchSecurityArchitectureReview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String statusStr = body.get("status") != null ? String.valueOf(body.get("status")).trim() : null;
            ApplicationSecurityReviewStatus status =
                    statusStr != null && !statusStr.isEmpty() ? ApplicationSecurityReviewStatus.valueOf(statusStr) : null;
            String notes = body.containsKey("notes") ? (String) body.get("notes") : null;
            ApplicationSecurityReviewSummaryDto updated = applicationSecurityReviewService.updateStatus(id, status, notes);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            ApplicationDto created = applicationService.create(body);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(applicationService.update(id, body));
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            applicationService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{appId}/audits")
    public List<AuditDto> listAudits(@PathVariable Long appId) {
        return auditService.findByApplicationId(appId);
    }

    @PostMapping("/{appId}/audits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAudit(@PathVariable Long appId, @RequestBody Map<String, Object> body) {
        Object yearObj = body.get("year");
        Object projectIdObj = body.get("projectId");
        Instant dueAt = body.containsKey("dueAt") && body.get("dueAt") != null ? Instant.parse(body.get("dueAt").toString()) : null;
        if (yearObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "year is required"));
        }
        if (projectIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "projectId is required; create audits through an Audit Project"));
        }
        int year = yearObj instanceof Number ? ((Number) yearObj).intValue() : Integer.parseInt(yearObj.toString());
        long projectId = projectIdObj instanceof Number ? ((Number) projectIdObj).longValue() : Long.parseLong(projectIdObj.toString());
        if (year < 2000 || year > 2100) {
            return ResponseEntity.badRequest().body(Map.of("error", "year must be between 2000 and 2100"));
        }
        try {
            AuditDto created = auditService.create(appId, year, dueAt, projectId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
