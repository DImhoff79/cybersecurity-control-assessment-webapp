package com.cyberassessment.controller;

import com.cyberassessment.dto.RiskControlLinkDto;
import com.cyberassessment.dto.RiskRegisterItemDto;
import com.cyberassessment.entity.RiskStatus;
import com.cyberassessment.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risks")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_RISK_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<RiskRegisterItemDto> list() {
        return riskService.list();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_RISK_MANAGEMENT')")
    public RiskRegisterItemDto create(@RequestBody Map<String, Object> body) {
        String title = body.containsKey("title") ? (String) body.get("title") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        String businessImpact = body.containsKey("businessImpact") ? (String) body.get("businessImpact") : null;
        Integer likelihoodScore = body.containsKey("likelihoodScore") ? ((Number) body.get("likelihoodScore")).intValue() : null;
        Integer impactScore = body.containsKey("impactScore") ? ((Number) body.get("impactScore")).intValue() : null;
        Long ownerUserId = body.containsKey("ownerUserId") && body.get("ownerUserId") != null
                ? ((Number) body.get("ownerUserId")).longValue() : null;
        Long applicationId = body.containsKey("applicationId") && body.get("applicationId") != null
                ? ((Number) body.get("applicationId")).longValue() : null;
        Instant targetCloseAt = parseInstant(body.get("targetCloseAt"));
        return riskService.create(title, description, businessImpact, likelihoodScore, impactScore, ownerUserId, applicationId, targetCloseAt);
    }

    @PutMapping("/{riskId}")
    @PreAuthorize("hasAuthority('PERM_RISK_MANAGEMENT')")
    public RiskRegisterItemDto update(@PathVariable Long riskId, @RequestBody Map<String, Object> body) {
        String title = body.containsKey("title") ? (String) body.get("title") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        String businessImpact = body.containsKey("businessImpact") ? (String) body.get("businessImpact") : null;
        Integer likelihoodScore = body.containsKey("likelihoodScore") && body.get("likelihoodScore") != null
                ? ((Number) body.get("likelihoodScore")).intValue() : null;
        Integer impactScore = body.containsKey("impactScore") && body.get("impactScore") != null
                ? ((Number) body.get("impactScore")).intValue() : null;
        Long ownerUserId = body.containsKey("ownerUserId") && body.get("ownerUserId") != null
                ? ((Number) body.get("ownerUserId")).longValue() : null;
        RiskStatus status = body.containsKey("status") && body.get("status") instanceof String s ? RiskStatus.valueOf(s) : null;
        Integer residualRiskScore = body.containsKey("residualRiskScore") && body.get("residualRiskScore") != null
                ? ((Number) body.get("residualRiskScore")).intValue() : null;
        Instant targetCloseAt = parseInstant(body.get("targetCloseAt"));
        return riskService.update(riskId, title, description, businessImpact, likelihoodScore, impactScore, ownerUserId, status, residualRiskScore, targetCloseAt);
    }

    @GetMapping("/{riskId}/controls")
    @PreAuthorize("hasAnyAuthority('PERM_RISK_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<RiskControlLinkDto> controls(@PathVariable Long riskId) {
        return riskService.controls(riskId);
    }

    @PostMapping("/{riskId}/links/controls")
    @PreAuthorize("hasAuthority('PERM_RISK_MANAGEMENT')")
    public RiskControlLinkDto linkControl(@PathVariable Long riskId, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("controlId") || body.get("controlId") == null) {
            throw new IllegalArgumentException("controlId is required");
        }
        Long controlId = ((Number) body.get("controlId")).longValue();
        String notes = body.containsKey("notes") ? (String) body.get("notes") : null;
        return riskService.linkControl(riskId, controlId, notes);
    }

    @PostMapping("/{riskId}/links/findings")
    @PreAuthorize("hasAuthority('PERM_RISK_MANAGEMENT')")
    public void linkFinding(@PathVariable Long riskId, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("findingId") || body.get("findingId") == null) {
            throw new IllegalArgumentException("findingId is required");
        }
        Long findingId = ((Number) body.get("findingId")).longValue();
        riskService.linkFinding(riskId, findingId);
    }

    @PostMapping("/{riskId}/links/exceptions")
    @PreAuthorize("hasAuthority('PERM_RISK_MANAGEMENT')")
    public void linkException(@PathVariable Long riskId, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("exceptionId") || body.get("exceptionId") == null) {
            throw new IllegalArgumentException("exceptionId is required");
        }
        Long exceptionId = ((Number) body.get("exceptionId")).longValue();
        riskService.linkException(riskId, exceptionId);
    }

    private Instant parseInstant(Object value) {
        if (value instanceof String s && !s.isBlank()) return Instant.parse(s);
        return null;
    }
}
