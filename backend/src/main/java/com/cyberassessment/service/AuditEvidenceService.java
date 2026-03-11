package com.cyberassessment.service;

import com.cyberassessment.dto.AuditEvidenceDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.AuditAssignmentRepository;
import com.cyberassessment.repository.AuditControlAssignmentRepository;
import com.cyberassessment.repository.AuditControlRepository;
import com.cyberassessment.repository.AuditEvidenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditEvidenceService {

    private final AuditEvidenceRepository auditEvidenceRepository;
    private final AuditControlRepository auditControlRepository;
    private final CurrentUserService currentUserService;
    private final AuditActivityLogService auditActivityLogService;
    private final AuditAssignmentRepository auditAssignmentRepository;
    private final AuditControlAssignmentRepository auditControlAssignmentRepository;

    @Value("${app.evidence-policy.min-description-length:8}")
    private int minDescriptionLength;

    @Value("${app.evidence-policy.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    @Value("${app.evidence-policy.allowed-content-types:application/pdf,text/plain,image/png,image/jpeg,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/msword,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel,text/csv}")
    private String allowedContentTypes;

    @Value("${app.evidence-policy.retention-days:365}")
    private long retentionDays;

    @Value("${app.evidence-storage-mode:db}")
    private String evidenceStorageMode;

    public static AuditEvidenceDto toDto(AuditEvidence e) {
        User reviewedBy = e.getReviewedBy();
        User createdBy = e.getCreatedBy();
        return AuditEvidenceDto.builder()
                .id(e.getId())
                .auditControlId(e.getAuditControl().getId())
                .evidenceType(e.getEvidenceType())
                .title(e.getTitle())
                .uri(e.getUri())
                .fileName(e.getFileName())
                .mimeType(e.getMimeType())
                .sizeBytes(e.getSizeBytes())
                .source(e.getSource())
                .owner(e.getOwner())
                .notes(e.getNotes())
                .collectedAt(e.getCollectedAt())
                .expiresAt(e.getExpiresAt())
                .reviewStatus(e.getReviewStatus())
                .reviewedByUserId(reviewedBy != null ? reviewedBy.getId() : null)
                .reviewedByEmail(reviewedBy != null ? reviewedBy.getEmail() : null)
                .reviewedAt(e.getReviewedAt())
                .createdByUserId(createdBy != null ? createdBy.getId() : null)
                .createdByEmail(createdBy != null ? createdBy.getEmail() : null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AuditEvidenceDto> listByAuditControl(Long auditControlId) {
        AuditControl ac = auditControlRepository.findById(auditControlId)
                .orElseThrow(() -> new IllegalArgumentException("AuditControl not found: " + auditControlId));
        ensureCanAccess(ac);
        return auditEvidenceRepository.findByAuditControlId(auditControlId).stream()
                .map(AuditEvidenceService::toDto)
                .toList();
    }

    @Transactional
    public AuditEvidenceDto create(Long auditControlId, EvidenceType evidenceType, String title, String uri, String source, String owner,
                                   String notes, Instant collectedAt, Instant expiresAt) {
        throw new IllegalArgumentException("Only file upload evidence is supported");
    }

    @Transactional
    @CacheEvict(cacheNames = {"reportSummary", "reportByYear", "reportTrends", "reportByProject"}, allEntries = true)
    public AuditEvidenceDto review(Long evidenceId, EvidenceReviewStatus status, String notes) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can review evidence");
        }
        AuditEvidence evidence = auditEvidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + evidenceId));
        if (status != null) {
            evidence.setReviewStatus(status);
            evidence.setReviewedBy(currentUserService.getCurrentUserOrThrow());
            evidence.setReviewedAt(Instant.now());
        }
        if (notes != null && !notes.isBlank()) {
            String combined = evidence.getNotes() == null || evidence.getNotes().isBlank()
                    ? notes
                    : evidence.getNotes() + "\n" + notes;
            evidence.setNotes(combined);
        }
        evidence = auditEvidenceRepository.save(evidence);
        auditActivityLogService.log(
                evidence.getAuditControl().getAudit(),
                AuditActivityType.EVIDENCE_REVIEWED,
                "Evidence " + evidence.getId() + " reviewed as " + evidence.getReviewStatus()
        );
        return toDto(evidence);
    }

    @Transactional
    @CacheEvict(cacheNames = {"reportSummary", "reportByYear", "reportTrends", "reportByProject"}, allEntries = true)
    public AuditEvidenceDto upload(Long auditControlId, String description, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (description.trim().length() < minDescriptionLength) {
            throw new IllegalArgumentException("description must be at least " + minDescriptionLength + " characters");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("file exceeds max size policy");
        }
        String contentType = file.getContentType();
        Set<String> allowed = parseAllowedContentTypes();
        if (contentType == null || !allowed.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("file type is not allowed by policy");
        }
        AuditControl ac = auditControlRepository.findById(auditControlId)
                .orElseThrow(() -> new IllegalArgumentException("AuditControl not found: " + auditControlId));
        ensureCanAccess(ac);
        try {
            String safeName = file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_") : "evidence.bin";
            byte[] fileContent = file.getBytes();
            String storageKey = Instant.now().toEpochMilli() + "_" + safeName;

            AuditEvidence evidence = AuditEvidence.builder()
                    .auditControl(ac)
                    .evidenceType(EvidenceType.DOCUMENT)
                    .title(safeName)
                    .notes(description.trim())
                    .fileName(safeName)
                    .storageKey(storageKey)
                    .source(evidenceStorageMode)
                    .mimeType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .fileContent(fileContent)
                    .collectedAt(Instant.now())
                    .expiresAt(Instant.now().plus(retentionDays, ChronoUnit.DAYS))
                    .reviewStatus(EvidenceReviewStatus.PENDING)
                    .createdBy(currentUserService.getCurrentUser().orElse(null))
                    .build();
            evidence = auditEvidenceRepository.save(evidence);
            evidence.setUri("/api/evidences/" + evidence.getId() + "/download");
            evidence = auditEvidenceRepository.save(evidence);
            auditActivityLogService.log(ac.getAudit(), AuditActivityType.EVIDENCE_ADDED, "Uploaded evidence file: " + safeName);
            return toDto(evidence);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to store evidence file");
        }
    }

    @Transactional(readOnly = true)
    public Resource loadEvidenceFile(Long evidenceId) {
        AuditEvidence evidence = auditEvidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + evidenceId));
        ensureCanAccess(evidence.getAuditControl());
        if (evidence.getFileContent() == null || evidence.getFileContent().length == 0) {
            throw new IllegalArgumentException("Evidence does not contain an uploaded file");
        }
        return new ByteArrayResource(evidence.getFileContent());
    }

    private void ensureCanAccess(AuditControl auditControl) {
        if (currentUserService.isAdmin()) {
            return;
        }
        User current = currentUserService.getCurrentUserOrThrow();
        Audit audit = auditControl.getAudit();
        boolean isPrimary = audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(current.getId());
        boolean isAuditCollaborator = auditAssignmentRepository.existsByAuditIdAndUserIdAndActiveTrue(audit.getId(), current.getId());
        boolean isTaskAssignee = auditControlAssignmentRepository.existsByAuditControlIdAndUserIdAndActiveTrue(auditControl.getId(), current.getId());
        if (!isPrimary && !isAuditCollaborator && !isTaskAssignee) {
            throw new IllegalArgumentException("You do not have access to this audit");
        }
    }

    private Set<String> parseAllowedContentTypes() {
        return Arrays.stream(allowedContentTypes.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
