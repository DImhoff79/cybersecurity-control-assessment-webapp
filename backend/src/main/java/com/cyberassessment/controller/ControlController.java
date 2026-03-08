package com.cyberassessment.controller;

import com.cyberassessment.dto.ControlDto;
import com.cyberassessment.entity.ControlFramework;
import com.cyberassessment.service.ControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/controls")
@RequiredArgsConstructor
public class ControlController {

    private final ControlService controlService;

    @GetMapping
    public List<ControlDto> list(
            @RequestParam(required = false) ControlFramework framework,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false, defaultValue = "false") boolean includeQuestions) {
        return controlService.findAll(framework, enabled, includeQuestions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ControlDto> get(@PathVariable Long id, @RequestParam(required = false, defaultValue = "true") boolean includeQuestions) {
        ControlDto dto = controlService.findById(id, includeQuestions);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ControlDto> patch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        Boolean enabled = body.containsKey("enabled") ? (Boolean) body.get("enabled") : null;
        try {
            ControlDto updated = controlService.patch(id, name, description, enabled);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ControlDto> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        Boolean enabled = body.containsKey("enabled") ? (Boolean) body.get("enabled") : null;
        try {
            ControlDto updated = controlService.update(id, name, description, enabled);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ControlDto> create(@RequestBody Map<String, Object> body) {
        String controlId = body.containsKey("controlId") ? (String) body.get("controlId") : null;
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        String frameworkValue = body.containsKey("framework") ? (String) body.get("framework") : null;
        Boolean enabled = body.containsKey("enabled") ? (Boolean) body.get("enabled") : null;
        String category = body.containsKey("category") ? (String) body.get("category") : null;
        try {
            ControlFramework framework = frameworkValue != null ? ControlFramework.valueOf(frameworkValue) : null;
            ControlDto created = controlService.create(controlId, name, description, framework, enabled, category);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            controlService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
