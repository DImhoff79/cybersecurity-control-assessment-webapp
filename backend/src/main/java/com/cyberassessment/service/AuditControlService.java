package com.cyberassessment.service;

import com.cyberassessment.dto.AuditControlAnswerDto;
import com.cyberassessment.dto.AuditControlDto;
import com.cyberassessment.dto.AuditEvidenceDto;
import com.cyberassessment.dto.BulkAuditControlUpdateItemDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.AuditControlRepository;
import com.cyberassessment.repository.AuditRepository;
import com.cyberassessment.repository.AuditAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditControlService {

    private final AuditControlRepository auditControlRepository;
    private final AuditRepository auditRepository;
    private final CurrentUserService currentUserService;
    private final AuditActivityLogService auditActivityLogService;
    private final AuditAssignmentRepository auditAssignmentRepository;

    public static AuditControlDto toDto(AuditControl ac) {
        if (ac == null) return null;
        Control c = ac.getControl();
        List<AuditControlAnswerDto> answers = ac.getAnswers().stream()
                .map(a -> AuditControlAnswerDto.builder()
                        .id(a.getId())
                        .auditControlId(ac.getId())
                        .questionId(a.getQuestion().getId())
                        .questionText(a.getQuestion().getQuestionText())
                        .answerText(a.getAnswerText())
                        .answeredAt(a.getAnsweredAt())
                        .build())
                .collect(Collectors.toList());
        List<AuditEvidenceDto> evidences = ac.getEvidences().stream()
                .map(AuditEvidenceService::toDto)
                .collect(Collectors.toList());
        return AuditControlDto.builder()
                .id(ac.getId())
                .auditId(ac.getAudit().getId())
                .controlId(c.getId())
                .controlControlId(c.getControlId())
                .controlName(c.getName())
                .status(ac.getStatus())
                .evidence(ac.getEvidence())
                .notes(ac.getNotes())
                .assessedAt(ac.getAssessedAt())
                .answers(answers)
                .evidences(evidences)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AuditControlDto> findByAuditId(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            User current = currentUserService.getCurrentUserOrThrow();
            boolean isPrimary = audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(current.getId());
            boolean isAuditCollaborator = auditAssignmentRepository.existsByAuditIdAndUserIdAndActiveTrue(audit.getId(), current.getId());
            boolean isAppOwner = audit.getApplication().getOwner() != null
                    && audit.getApplication().getOwner().getId().equals(current.getId());
            if (!isPrimary && !isAuditCollaborator && !isAppOwner) {
                throw new IllegalArgumentException("You do not have access to this audit");
            }
        }
        return auditControlRepository.findByAudit(audit).stream().map(AuditControlService::toDto).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(cacheNames = {"reportSummary", "reportByYear", "reportTrends", "reportByProject"}, allEntries = true)
    public AuditControlDto update(Long id, ControlAssessmentStatus status, String evidence, String notes) {
        AuditControl ac = auditControlRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("AuditControl not found: " + id));
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            User current = currentUserService.getCurrentUserOrThrow();
            Audit audit = ac.getAudit();
            boolean isPrimary = audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(current.getId());
            boolean isAuditCollaborator = auditAssignmentRepository.existsByAuditIdAndUserIdAndActiveTrue(audit.getId(), current.getId());
            boolean isAppOwner = audit.getApplication().getOwner() != null
                    && audit.getApplication().getOwner().getId().equals(current.getId());
            if (!isPrimary && !isAuditCollaborator && !isAppOwner) {
                throw new IllegalArgumentException("You do not have access to this audit");
            }
        }
        if (status != null) ac.setStatus(status);
        if (evidence != null) ac.setEvidence(evidence);
        if (notes != null) ac.setNotes(notes);
        if (status != null && (status == ControlAssessmentStatus.PASS || status == ControlAssessmentStatus.FAIL || status == ControlAssessmentStatus.NA)) {
            ac.setAssessedAt(java.time.Instant.now());
        }
        ac = auditControlRepository.save(ac);
        auditActivityLogService.log(ac.getAudit(), AuditActivityType.CONTROL_UPDATED, "Updated control " + ac.getControl().getControlId());
        return toDto(ac);
    }

    @Transactional
    @CacheEvict(cacheNames = {"reportSummary", "reportByYear", "reportTrends", "reportByProject"}, allEntries = true)
    public void bulkUpdate(Long auditId, List<BulkAuditControlUpdateItemDto> updates) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        if (!currentUserService.isAdmin()) {
            User current = currentUserService.getCurrentUserOrThrow();
            boolean isPrimary = audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(current.getId());
            boolean isAuditCollaborator = auditAssignmentRepository.existsByAuditIdAndUserIdAndActiveTrue(audit.getId(), current.getId());
            boolean isAppOwner = audit.getApplication().getOwner() != null
                    && audit.getApplication().getOwner().getId().equals(current.getId());
            if (!isPrimary && !isAuditCollaborator && !isAppOwner) {
                throw new IllegalArgumentException("You do not have access to this audit");
            }
        }
        List<AuditControl> changed = new java.util.ArrayList<>();
        for (BulkAuditControlUpdateItemDto item : updates) {
            if (item.getAuditControlId() == null) {
                continue;
            }
            AuditControl ac = auditControlRepository.findById(item.getAuditControlId())
                    .orElseThrow(() -> new IllegalArgumentException("AuditControl not found: " + item.getAuditControlId()));
            if (!ac.getAudit().getId().equals(auditId)) {
                throw new IllegalArgumentException("Audit control does not belong to audit");
            }
            if (item.getStatus() != null) {
                ac.setStatus(item.getStatus());
                if (item.getStatus() == ControlAssessmentStatus.PASS || item.getStatus() == ControlAssessmentStatus.FAIL || item.getStatus() == ControlAssessmentStatus.NA) {
                    ac.setAssessedAt(java.time.Instant.now());
                }
            }
            if (item.getNotes() != null) {
                ac.setNotes(item.getNotes());
            }
            changed.add(ac);
        }
        if (!changed.isEmpty()) {
            auditControlRepository.saveAll(changed);
            auditActivityLogService.log(audit, AuditActivityType.CONTROL_UPDATED, "Bulk-updated " + changed.size() + " controls");
        }
    }
}
