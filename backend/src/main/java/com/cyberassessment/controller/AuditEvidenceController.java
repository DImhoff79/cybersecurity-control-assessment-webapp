package com.cyberassessment.controller;

import com.cyberassessment.dto.AuditEvidenceDto;
import com.cyberassessment.entity.EvidenceReviewStatus;
import com.cyberassessment.service.AuditEvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuditEvidenceController {

    private final AuditEvidenceService auditEvidenceService;

    @GetMapping("/audit-controls/{auditControlId}/evidences")
    public List<AuditEvidenceDto> listByAuditControl(@PathVariable Long auditControlId) {
        return auditEvidenceService.listByAuditControl(auditControlId);
    }

    @PostMapping("/audit-controls/{auditControlId}/evidences")
    public ResponseEntity<AuditEvidenceDto> create(@PathVariable Long auditControlId) {
        return ResponseEntity.badRequest().build();
    }

    @PostMapping(value = "/audit-controls/{auditControlId}/evidences/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuditEvidenceDto> upload(
            @PathVariable Long auditControlId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("file") MultipartFile file
    ) {
        AuditEvidenceDto created = auditEvidenceService.upload(auditControlId, description, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/evidences/{evidenceId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditEvidenceDto> review(@PathVariable Long evidenceId, @RequestBody Map<String, Object> body) {
        EvidenceReviewStatus status = body.containsKey("reviewStatus")
                ? EvidenceReviewStatus.valueOf((String) body.get("reviewStatus")) : null;
        String notes = body.containsKey("notes") ? (String) body.get("notes") : null;
        AuditEvidenceDto updated = auditEvidenceService.review(evidenceId, status, notes);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/evidences/{evidenceId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long evidenceId) {
        Resource resource = auditEvidenceService.loadEvidenceFile(evidenceId);
        String fileName = resource.getFilename() != null ? resource.getFilename() : "evidence.bin";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
