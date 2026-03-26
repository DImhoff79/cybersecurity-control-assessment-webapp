package com.cyberassessment.controller;

import com.cyberassessment.dto.ApprovalDelegateDto;
import com.cyberassessment.service.ApprovalDelegateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/approval-delegates")
@RequiredArgsConstructor
public class ApprovalDelegateController {

    private final ApprovalDelegateService approvalDelegateService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public List<ApprovalDelegateDto> list() {
        return approvalDelegateService.list();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<ApprovalDelegateDto> add(@RequestBody Map<String, Object> body) {
        Object uid = body.get("userId");
        if (uid == null) {
            return ResponseEntity.badRequest().build();
        }
        long userId = uid instanceof Number ? ((Number) uid).longValue() : Long.parseLong(uid.toString());
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(approvalDelegateService.add(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        try {
            approvalDelegateService.remove(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
