package com.cyberassessment.controller;

import com.cyberassessment.dto.AuditProjectDto;
import com.cyberassessment.service.AuditProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-projects")
@RequiredArgsConstructor
public class AuditProjectController {
    private final AuditProjectService auditProjectService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public List<AuditProjectDto> list() {
        return auditProjectService.list();
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<?> get(@PathVariable Long projectId) {
        try {
            return ResponseEntity.ok(auditProjectService.get(projectId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('AUDIT_MANAGER')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            String name = body.containsKey("name") ? String.valueOf(body.get("name")) : null;
            String frameworkTag = body.containsKey("frameworkTag") ? (String) body.get("frameworkTag") : null;
            Integer year = body.containsKey("year") && body.get("year") != null
                    ? Integer.parseInt(String.valueOf(body.get("year")))
                    : null;
            String notes = body.containsKey("notes") ? (String) body.get("notes") : null;
            Instant startsAt = body.containsKey("startsAt") && body.get("startsAt") != null
                    ? Instant.parse(String.valueOf(body.get("startsAt")))
                    : null;
            Instant dueAt = body.containsKey("dueAt") && body.get("dueAt") != null
                    ? Instant.parse(String.valueOf(body.get("dueAt")))
                    : null;
            @SuppressWarnings("unchecked")
            List<Number> appIdsRaw = body.containsKey("applicationIds") ? (List<Number>) body.get("applicationIds") : List.of();
            List<Long> appIds = appIdsRaw.stream().map(Number::longValue).toList();
            AuditProjectDto created = auditProjectService.create(name, frameworkTag, year, notes, startsAt, dueAt, appIds);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("hasRole('AUDIT_MANAGER')")
    public ResponseEntity<?> update(@PathVariable Long projectId, @RequestBody Map<String, Object> body) {
        try {
            String name = body.containsKey("name") ? (String) body.get("name") : null;
            String frameworkTag = body.containsKey("frameworkTag") ? (String) body.get("frameworkTag") : null;
            Integer year = body.containsKey("year") && body.get("year") != null
                    ? Integer.parseInt(String.valueOf(body.get("year")))
                    : null;
            String notes = body.containsKey("notes") ? (String) body.get("notes") : null;
            Instant startsAt = body.containsKey("startsAt") && body.get("startsAt") != null
                    ? Instant.parse(String.valueOf(body.get("startsAt")))
                    : null;
            Instant dueAt = body.containsKey("dueAt") && body.get("dueAt") != null
                    ? Instant.parse(String.valueOf(body.get("dueAt")))
                    : null;
            @SuppressWarnings("unchecked")
            List<Number> appIdsRaw = body.containsKey("applicationIds") ? (List<Number>) body.get("applicationIds") : null;
            List<Long> appIds = appIdsRaw != null ? appIdsRaw.stream().map(Number::longValue).toList() : null;
            AuditProjectDto updated = auditProjectService.update(projectId, name, frameworkTag, year, notes, startsAt, dueAt, appIds);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasRole('AUDIT_MANAGER')")
    public ResponseEntity<?> delete(@PathVariable Long projectId) {
        try {
            auditProjectService.delete(projectId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
