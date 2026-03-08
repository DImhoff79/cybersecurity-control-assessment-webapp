package com.cyberassessment.service;

import com.cyberassessment.dto.AuditEvidenceDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.AuditAssignmentRepository;
import com.cyberassessment.repository.AuditControlAssignmentRepository;
import com.cyberassessment.repository.AuditControlRepository;
import com.cyberassessment.repository.AuditEvidenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditEvidenceService {

    private final AuditEvidenceRepository auditEvidenceRepository;
    private final AuditControlRepository auditControlRepository;
    private final CurrentUserService currentUserService;
    private final AuditActivityLogService auditActivityLogService;
    private final AuditAssignmentRepository auditAssignmentRepository;
    private final AuditControlAssignmentRepository auditControlAssignmentRepository;

    @Value("${app.evidence-storage-path:./data/evidence}")
    private String evidenceStoragePath;

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
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        AuditControl ac = auditControlRepository.findById(auditControlId)
                .orElseThrow(() -> new IllegalArgumentException("AuditControl not found: " + auditControlId));
        ensureCanAccess(ac);
        AuditEvidence evidence = AuditEvidence.builder()
                .auditControl(ac)
                .evidenceType(evidenceType != null ? evidenceType : EvidenceType.OTHER)
                .title(title)
                .uri(uri)
                .source(source)
                .owner(owner)
                .notes(notes)
                .collectedAt(collectedAt)
                .expiresAt(expiresAt)
                .reviewStatus(EvidenceReviewStatus.PENDING)
                .createdBy(currentUserService.getCurrentUser().orElse(null))
                .build();
        evidence = auditEvidenceRepository.save(evidence);
        auditActivityLogService.log(ac.getAudit(), AuditActivityType.EVIDENCE_ADDED, "Added evidence: " + title);
        return toDto(evidence);
    }

    @Transactional
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
    public AuditEvidenceDto upload(Long auditControlId, EvidenceType evidenceType, String title, String notes, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        AuditControl ac = auditControlRepository.findById(auditControlId)
                .orElseThrow(() -> new IllegalArgumentException("AuditControl not found: " + auditControlId));
        ensureCanAccess(ac);
        try {
            Path basePath = Path.of(evidenceStoragePath).toAbsolutePath().normalize();
            Files.createDirectories(basePath);
            String safeName = file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_") : "evidence.bin";
            String storedName = Instant.now().toEpochMilli() + "_" + safeName;
            Path storedPath = basePath.resolve(storedName);
            Files.copy(file.getInputStream(), storedPath, StandardCopyOption.REPLACE_EXISTING);

            AuditEvidence evidence = AuditEvidence.builder()
                    .auditControl(ac)
                    .evidenceType(evidenceType != null ? evidenceType : EvidenceType.DOCUMENT)
                    .title(title != null && !title.isBlank() ? title : safeName)
                    .notes(notes)
                    .fileName(safeName)
                    .filePath(storedPath.toString())
                    .mimeType(file.getContentType())
                    .sizeBytes(file.getSize())
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
        if (evidence.getFilePath() == null || evidence.getFilePath().isBlank()) {
            throw new IllegalArgumentException("Evidence does not contain an uploaded file");
        }
        return new FileSystemResource(evidence.getFilePath());
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
}
