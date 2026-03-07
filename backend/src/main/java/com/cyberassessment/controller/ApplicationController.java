package com.cyberassessment.controller;

import com.cyberassessment.dto.ApplicationDto;
import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.service.ApplicationService;
import com.cyberassessment.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApplicationDto> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        Long ownerId = body.get("ownerId") != null ? ((Number) body.get("ownerId")).longValue() : null;
        ApplicationDto created = applicationService.create(name, description, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationDto> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        Long ownerId = body.get("ownerId") != null ? ((Number) body.get("ownerId")).longValue() : null;
        ApplicationDto updated = applicationService.update(id, name, description, ownerId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
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
    public ResponseEntity<?> createAudit(@PathVariable Long appId, @RequestBody Map<String, Object> body) {
        Object yearObj = body.get("year");
        if (yearObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "year is required"));
        }
        int year = yearObj instanceof Number ? ((Number) yearObj).intValue() : Integer.parseInt(yearObj.toString());
        if (year < 2000 || year > 2100) {
            return ResponseEntity.badRequest().body(Map.of("error", "year must be between 2000 and 2100"));
        }
        try {
            AuditDto created = auditService.create(appId, year);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
