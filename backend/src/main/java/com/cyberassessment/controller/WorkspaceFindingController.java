package com.cyberassessment.controller;

import com.cyberassessment.dto.FindingDto;
import com.cyberassessment.service.AuditService;
import com.cyberassessment.service.FindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Findings for audits the current user may access (self-service / workspace).
 */
@RestController
@RequestMapping("/api/workspace/findings")
@RequiredArgsConstructor
public class WorkspaceFindingController {

    private final FindingService findingService;
    private final AuditService auditService;

    @GetMapping
    public List<FindingDto> listByAudit(@RequestParam Long auditId) {
        auditService.assertCanAccessAudit(auditId);
        return findingService.list(auditId);
    }
}
