package com.cyberassessment.controller;

import com.cyberassessment.dto.UserDto;
import com.cyberassessment.entity.UserPermission;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public List<UserDto> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<UserDto> get(@PathVariable Long id) {
        UserDto dto = userService.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String email = body.containsKey("email") ? (String) body.get("email") : null;
        String password = body.containsKey("password") ? (String) body.get("password") : null;
        String displayName = body.containsKey("displayName") ? (String) body.get("displayName") : null;
        try {
            UserRole role = parseRole(body.containsKey("role") ? (String) body.get("role") : null);
            Set<UserPermission> permissions = parsePermissions(body.get("permissions"));
            UserDto created = userService.create(email, password, displayName, role, permissions);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String email = body.containsKey("email") ? (String) body.get("email") : null;
        String password = body.containsKey("password") ? (String) body.get("password") : null;
        String displayName = body.containsKey("displayName") ? (String) body.get("displayName") : null;
        try {
            UserRole role = body.containsKey("role") ? parseRole((String) body.get("role")) : null;
            Set<UserPermission> permissions = body.containsKey("permissions") ? parsePermissions(body.get("permissions")) : null;
            UserDto updated = userService.update(id, email, password, displayName, role, permissions);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete user with active assignments or ownership."));
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

    private Set<UserPermission> parsePermissions(Object rawPermissions) {
        if (rawPermissions == null) {
            return null;
        }
        if (!(rawPermissions instanceof List<?> list)) {
            throw new IllegalArgumentException("permissions must be an array of permission names");
        }
        Set<UserPermission> permissions = new HashSet<>();
        for (Object value : list) {
            if (!(value instanceof String raw)) {
                throw new IllegalArgumentException("permissions must only contain string values");
            }
            String normalized = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
            permissions.add(UserPermission.valueOf(normalized));
        }
        return permissions;
    }
}
