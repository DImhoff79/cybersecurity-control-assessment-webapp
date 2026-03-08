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
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditProjectDto> list() {
        return auditProjectService.list();
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> get(@PathVariable Long projectId) {
        try {
            return ResponseEntity.ok(auditProjectService.get(projectId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
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
}
