package com.cyberassessment.controller;

import com.cyberassessment.dto.UserDto;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> get(@PathVariable Long id) {
        UserDto dto = userService.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String displayName = body.get("displayName");
        String roleStr = body.get("role");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and password required"));
        }
        UserRole role = roleStr != null ? UserRole.valueOf(roleStr) : UserRole.APPLICATION_OWNER;
        try {
            UserDto created = userService.create(email, password, displayName, role);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String displayName = body.containsKey("displayName") ? (String) body.get("displayName") : null;
        UserRole role = body.containsKey("role") ? UserRole.valueOf((String) body.get("role")) : null;
        try {
            UserDto updated = userService.update(id, displayName, role);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
