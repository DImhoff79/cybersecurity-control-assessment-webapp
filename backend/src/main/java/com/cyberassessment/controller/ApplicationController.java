package com.cyberassessment.controller;

import com.cyberassessment.dto.ApplicationDto;
import com.cyberassessment.dto.AuditDto;
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

    @GetMapping
    public List<ApplicationDto> list() {
        return applicationService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDto> get(@PathVariable Long id) {
        ApplicationDto dto = applicationService.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
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
