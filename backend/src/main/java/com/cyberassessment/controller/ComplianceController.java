package com.cyberassessment.controller;

import com.cyberassessment.dto.*;
import com.cyberassessment.service.ComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceService complianceService;

    @GetMapping("/regulations")
    @PreAuthorize("hasAnyAuthority('PERM_AUDIT_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<RegulationDto> regulations() {
        return complianceService.listRegulations();
    }

    @PostMapping("/regulations")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public RegulationDto createRegulation(@RequestBody Map<String, Object> body) {
        String code = body.containsKey("code") ? (String) body.get("code") : null;
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        String version = body.containsKey("version") ? (String) body.get("version") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        Boolean active = body.containsKey("active") ? (Boolean) body.get("active") : null;
        return complianceService.createRegulation(code, name, version, description, active);
    }

    @GetMapping("/requirements")
    @PreAuthorize("hasAnyAuthority('PERM_AUDIT_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<ComplianceRequirementDto> requirements(@RequestParam(required = false) Long regulationId) {
        return complianceService.listRequirements(regulationId);
    }

    @PostMapping("/requirements")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ComplianceRequirementDto createRequirement(@RequestBody Map<String, Object> body) {
        if (!body.containsKey("regulationId") || body.get("regulationId") == null) {
            throw new IllegalArgumentException("regulationId is required");
        }
        Long regulationId = ((Number) body.get("regulationId")).longValue();
        String requirementCode = body.containsKey("requirementCode") ? (String) body.get("requirementCode") : null;
        String title = body.containsKey("title") ? (String) body.get("title") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        Boolean active = body.containsKey("active") ? (Boolean) body.get("active") : null;
        return complianceService.createRequirement(regulationId, requirementCode, title, description, active);
    }

    @GetMapping("/requirement-control-mappings")
    @PreAuthorize("hasAnyAuthority('PERM_AUDIT_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<RequirementControlMappingDto> listRequirementControlMappings(@RequestParam(required = false) Long requirementId) {
        return complianceService.listRequirementControlMappings(requirementId);
    }

    @PostMapping("/requirement-control-mappings")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public RequirementControlMappingDto createRequirementControlMapping(@RequestBody Map<String, Object> body) {
        if (!body.containsKey("requirementId") || !body.containsKey("controlId")) {
            throw new IllegalArgumentException("requirementId and controlId are required");
        }
        Long requirementId = ((Number) body.get("requirementId")).longValue();
        Long controlId = ((Number) body.get("controlId")).longValue();
        Integer coveragePct = body.containsKey("coveragePct") && body.get("coveragePct") != null
                ? ((Number) body.get("coveragePct")).intValue()
                : null;
        String notes = body.containsKey("notes") ? (String) body.get("notes") : null;
        return complianceService.mapRequirementToControl(requirementId, controlId, coveragePct, notes);
    }

    @GetMapping("/policy-requirement-mappings")
    @PreAuthorize("hasAnyAuthority('PERM_AUDIT_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<PolicyRequirementMappingDto> listPolicyRequirementMappings(@RequestParam(required = false) Long policyId) {
        return complianceService.listPolicyRequirementMappings(policyId);
    }

    @PostMapping("/policy-requirement-mappings")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public PolicyRequirementMappingDto createPolicyRequirementMapping(@RequestBody Map<String, Object> body) {
        if (!body.containsKey("policyId") || !body.containsKey("requirementId")) {
            throw new IllegalArgumentException("policyId and requirementId are required");
        }
        Long policyId = ((Number) body.get("policyId")).longValue();
        Long requirementId = ((Number) body.get("requirementId")).longValue();
        String notes = body.containsKey("notes") ? (String) body.get("notes") : null;
        return complianceService.mapPolicyToRequirement(policyId, requirementId, notes);
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyAuthority('PERM_AUDIT_MANAGEMENT','PERM_REPORT_VIEW')")
    public ComplianceKpiDto kpis() {
        return complianceService.complianceKpis();
    }
}
