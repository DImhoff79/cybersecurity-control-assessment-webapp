package com.cyberassessment.controller;

import com.cyberassessment.dto.PolicyAcknowledgementDto;
import com.cyberassessment.dto.PolicyDto;
import com.cyberassessment.dto.PolicyRevisionEventDto;
import com.cyberassessment.dto.PolicyVersionDto;
import com.cyberassessment.entity.NistCsfFunction;
import com.cyberassessment.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_AUDIT_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<PolicyDto> list() {
        return policyService.list();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<PolicyDto> create(@RequestBody Map<String, Object> body) {
        String code = body.containsKey("code") ? (String) body.get("code") : null;
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        Long ownerUserId = body.containsKey("ownerUserId") && body.get("ownerUserId") != null
                ? ((Number) body.get("ownerUserId")).longValue()
                : null;
        String initialVersionTitle = body.containsKey("initialVersionTitle") ? (String) body.get("initialVersionTitle") : null;
        String initialBodyMarkdown = body.containsKey("initialBodyMarkdown") ? (String) body.get("initialBodyMarkdown") : null;
        return ResponseEntity.ok(policyService.create(
                code,
                name,
                description,
                ownerUserId,
                initialVersionTitle,
                initialBodyMarkdown,
                parseStringList(body.get("csfFunctions"))
        ));
    }

    @PutMapping("/{policyId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public PolicyDto update(@PathVariable Long policyId, @RequestBody Map<String, Object> body) {
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        Long ownerUserId = body.containsKey("ownerUserId") && body.get("ownerUserId") != null
                ? ((Number) body.get("ownerUserId")).longValue()
                : null;
        Instant nextReviewAt = null;
        if (body.containsKey("nextReviewAt") && body.get("nextReviewAt") instanceof String s && !s.isBlank()) {
            nextReviewAt = Instant.parse(s);
        }
        return policyService.update(policyId, name, description, ownerUserId, nextReviewAt, parseStringList(body.get("csfFunctions")));
    }

    @PostMapping("/{policyId}/versions")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public PolicyVersionDto createVersion(@PathVariable Long policyId, @RequestBody Map<String, Object> body) {
        String title = body.containsKey("title") ? (String) body.get("title") : null;
        String bodyMarkdown = body.containsKey("bodyMarkdown") ? (String) body.get("bodyMarkdown") : null;
        return policyService.createVersion(policyId, title, bodyMarkdown);
    }

    @PutMapping("/{policyId}/versions/{versionId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public PolicyVersionDto updateVersion(
            @PathVariable Long policyId,
            @PathVariable Long versionId,
            @RequestBody Map<String, Object> body
    ) {
        String title = body.containsKey("title") ? (String) body.get("title") : null;
        String bodyMarkdown = body.containsKey("bodyMarkdown") ? (String) body.get("bodyMarkdown") : null;
        return policyService.updateVersion(policyId, versionId, title, bodyMarkdown);
    }

    @PostMapping("/{policyId}/publish")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public PolicyDto publish(@PathVariable Long policyId, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("policyVersionId") || body.get("policyVersionId") == null) {
            throw new IllegalArgumentException("policyVersionId is required");
        }
        Long policyVersionId = ((Number) body.get("policyVersionId")).longValue();
        Instant dueAt = null;
        if (body.containsKey("dueAt") && body.get("dueAt") instanceof String s && !s.isBlank()) {
            dueAt = Instant.parse(s);
        }
        return policyService.publish(policyId, policyVersionId, dueAt);
    }

    @GetMapping("/acknowledgements")
    @PreAuthorize("hasAnyAuthority('PERM_AUDIT_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<PolicyAcknowledgementDto> acknowledgements(@RequestParam(required = false) Long policyId) {
        return policyService.listAcknowledgements(policyId);
    }

    @GetMapping("/my-acknowledgements")
    public List<PolicyAcknowledgementDto> myAcknowledgements() {
        return policyService.myAcknowledgements();
    }

    @PostMapping("/acknowledgements/{acknowledgementId}/acknowledge")
    public PolicyAcknowledgementDto acknowledge(@PathVariable Long acknowledgementId) {
        return policyService.acknowledge(acknowledgementId);
    }

    @GetMapping("/{policyId}/csf-mappings")
    @PreAuthorize("hasAnyAuthority('PERM_AUDIT_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<NistCsfFunction> getCsfMappings(@PathVariable Long policyId) {
        return policyService.getCsfMappings(policyId);
    }

    @PutMapping("/{policyId}/csf-mappings")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public List<NistCsfFunction> updateCsfMappings(@PathVariable Long policyId, @RequestBody Map<String, Object> body) {
        return policyService.updateCsfMappings(policyId, parseStringList(body.get("csfFunctions")));
    }

    @GetMapping("/{policyId}/revision-history")
    @PreAuthorize("hasAnyAuthority('PERM_AUDIT_MANAGEMENT','PERM_REPORT_VIEW')")
    public List<PolicyRevisionEventDto> revisionHistory(@PathVariable Long policyId) {
        return policyService.getRevisionHistory(policyId);
    }

    private List<String> parseStringList(Object value) {
        if (!(value instanceof List<?> raw)) return null;
        List<String> parsed = new ArrayList<>();
        for (Object item : raw) {
            if (item != null) parsed.add(item.toString());
        }
        return parsed;
    }
}
