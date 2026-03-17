package com.cyberassessment.controller;

import com.cyberassessment.dto.RemediationActionDto;
import com.cyberassessment.dto.RemediationPlanDto;
import com.cyberassessment.entity.RemediationActionStatus;
import com.cyberassessment.entity.RemediationPlanStatus;
import com.cyberassessment.service.RemediationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/remediation-plans")
@RequiredArgsConstructor
public class RemediationController {

    private final RemediationService remediationService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_REMEDIATION_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<RemediationPlanDto> listPlans() {
        return remediationService.listPlans();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_REMEDIATION_MANAGEMENT')")
    public RemediationPlanDto createPlan(@RequestBody Map<String, Object> body) {
        Long riskId = body.containsKey("riskId") && body.get("riskId") != null
                ? ((Number) body.get("riskId")).longValue()
                : null;
        String title = body.containsKey("title") ? (String) body.get("title") : null;
        Instant targetCompleteAt = parseInstant(body.get("targetCompleteAt"));
        return remediationService.createPlan(riskId, title, targetCompleteAt);
    }

    @PutMapping("/{planId}")
    @PreAuthorize("hasAuthority('PERM_REMEDIATION_MANAGEMENT')")
    public RemediationPlanDto updatePlan(@PathVariable Long planId, @RequestBody Map<String, Object> body) {
        String title = body.containsKey("title") ? (String) body.get("title") : null;
        RemediationPlanStatus status = body.containsKey("status") && body.get("status") instanceof String s
                ? RemediationPlanStatus.valueOf(s)
                : null;
        Instant targetCompleteAt = parseInstant(body.get("targetCompleteAt"));
        return remediationService.updatePlan(planId, title, status, targetCompleteAt);
    }

    @GetMapping("/{planId}/actions")
    @PreAuthorize("hasAnyAuthority('PERM_REMEDIATION_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<RemediationActionDto> actions(@PathVariable Long planId) {
        return remediationService.actions(planId);
    }

    @PostMapping("/{planId}/actions")
    @PreAuthorize("hasAuthority('PERM_REMEDIATION_MANAGEMENT')")
    public RemediationActionDto createAction(@PathVariable Long planId, @RequestBody Map<String, Object> body) {
        String actionTitle = body.containsKey("actionTitle") ? (String) body.get("actionTitle") : null;
        String actionDetail = body.containsKey("actionDetail") ? (String) body.get("actionDetail") : null;
        Long ownerUserId = body.containsKey("ownerUserId") && body.get("ownerUserId") != null
                ? ((Number) body.get("ownerUserId")).longValue()
                : null;
        Integer sequenceNo = body.containsKey("sequenceNo") && body.get("sequenceNo") != null
                ? ((Number) body.get("sequenceNo")).intValue()
                : null;
        Instant dueAt = parseInstant(body.get("dueAt"));
        return remediationService.createAction(planId, actionTitle, actionDetail, ownerUserId, dueAt, sequenceNo);
    }

    @PutMapping("/actions/{actionId}")
    @PreAuthorize("hasAuthority('PERM_REMEDIATION_MANAGEMENT')")
    public RemediationActionDto updateAction(@PathVariable Long actionId, @RequestBody Map<String, Object> body) {
        RemediationActionStatus status = body.containsKey("status") && body.get("status") instanceof String s
                ? RemediationActionStatus.valueOf(s)
                : null;
        Instant completedAt = parseInstant(body.get("completedAt"));
        Instant dueAt = parseInstant(body.get("dueAt"));
        return remediationService.updateAction(actionId, status, completedAt, dueAt);
    }

    private Instant parseInstant(Object value) {
        if (value instanceof String s && !s.isBlank()) return Instant.parse(s);
        return null;
    }
}
