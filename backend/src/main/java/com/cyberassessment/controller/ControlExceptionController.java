package com.cyberassessment.controller;

import com.cyberassessment.dto.ControlExceptionCreateRequest;
import com.cyberassessment.dto.ControlExceptionDecisionRequest;
import com.cyberassessment.dto.ControlExceptionDto;
import com.cyberassessment.service.ControlExceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/control-exceptions")
@RequiredArgsConstructor
public class ControlExceptionController {

    private final ControlExceptionService controlExceptionService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public List<ControlExceptionDto> list(
            @RequestParam(required = false) Long auditId,
            @RequestParam(required = false) Long findingId) {
        return controlExceptionService.list(auditId, findingId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<ControlExceptionDto> request(@RequestBody ControlExceptionCreateRequest request) {
        return ResponseEntity.ok(controlExceptionService.request(request));
    }

    @PostMapping("/{exceptionId}/approve")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<ControlExceptionDto> approve(@PathVariable Long exceptionId,
                                                        @RequestBody(required = false) ControlExceptionDecisionRequest request) {
        return ResponseEntity.ok(controlExceptionService.approve(exceptionId, request));
    }

    @PostMapping("/{exceptionId}/reject")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<ControlExceptionDto> reject(@PathVariable Long exceptionId,
                                                       @RequestBody(required = false) ControlExceptionDecisionRequest request) {
        return ResponseEntity.ok(controlExceptionService.reject(exceptionId, request));
    }
}
