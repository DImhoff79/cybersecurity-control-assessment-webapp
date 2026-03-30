package com.cyberassessment.controller;

import com.cyberassessment.dto.OwnerAnswerOptionProfileDto;
import com.cyberassessment.service.OwnerAnswerOptionProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner-answer-option-profiles")
@RequiredArgsConstructor
public class OwnerAnswerOptionProfileController {

    private final OwnerAnswerOptionProfileService ownerAnswerOptionProfileService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public List<OwnerAnswerOptionProfileDto> list() {
        return ownerAnswerOptionProfileService.listAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<OwnerAnswerOptionProfileDto> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ownerAnswerOptionProfileService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody OwnerAnswerOptionProfileDto body) {
        try {
            return ResponseEntity.ok(ownerAnswerOptionProfileService.update(id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Bad request"));
        }
    }
}
