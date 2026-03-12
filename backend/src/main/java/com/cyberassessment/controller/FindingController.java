package com.cyberassessment.controller;

import com.cyberassessment.dto.FindingDto;
import com.cyberassessment.dto.FindingUpsertRequest;
import com.cyberassessment.service.FindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/findings")
@RequiredArgsConstructor
public class FindingController {

    private final FindingService findingService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public List<FindingDto> list(@RequestParam(required = false) Long auditId) {
        return findingService.list(auditId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<FindingDto> create(@RequestBody FindingUpsertRequest request) {
        return ResponseEntity.ok(findingService.create(request));
    }

    @PutMapping("/{findingId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<FindingDto> update(@PathVariable Long findingId, @RequestBody FindingUpsertRequest request) {
        return ResponseEntity.ok(findingService.update(findingId, request));
    }
}
