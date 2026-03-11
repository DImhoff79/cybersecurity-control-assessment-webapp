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
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public List<AccessRequestDto> listPending() {
        return accessRequestService.listPending();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            String roleStr = body != null && body.containsKey("role") ? String.valueOf(body.get("role")) : null;
            UserRole role = parseRole(roleStr);
            String notes = body != null && body.containsKey("notes") ? (String) body.get("notes") : null;
            return ResponseEntity.ok(accessRequestService.approve(id, role, notes));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            String notes = body != null && body.containsKey("notes") ? (String) body.get("notes") : null;
            return ResponseEntity.ok(accessRequestService.reject(id, notes));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private UserRole parseRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return UserRole.APPLICATION_OWNER;
        }
        String normalized = rawRole.trim().toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');
        if ("APPLICATION_OWNERS".equals(normalized)) {
            normalized = "APPLICATION_OWNER";
        }
        return UserRole.valueOf(normalized);
    }
}
