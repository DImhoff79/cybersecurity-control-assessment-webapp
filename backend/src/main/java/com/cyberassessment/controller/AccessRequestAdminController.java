package com.cyberassessment.controller;

import com.cyberassessment.dto.AccessRequestDto;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.service.AccessRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/access-requests")
@RequiredArgsConstructor
public class AccessRequestAdminController {
    private final AccessRequestService accessRequestService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccessRequestDto> listPending() {
        return accessRequestService.listPending();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            String roleStr = body != null && body.containsKey("role") ? String.valueOf(body.get("role")) : null;
            UserRole role = roleStr != null && !roleStr.isBlank() ? UserRole.valueOf(roleStr) : UserRole.APPLICATION_OWNER;
            String notes = body != null && body.containsKey("notes") ? (String) body.get("notes") : null;
            return ResponseEntity.ok(accessRequestService.approve(id, role, notes));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            String notes = body != null && body.containsKey("notes") ? (String) body.get("notes") : null;
            return ResponseEntity.ok(accessRequestService.reject(id, notes));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
