package com.cyberassessment.service;

import com.cyberassessment.dto.AuditControlAnswerDto;
import com.cyberassessment.dto.AuditControlDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.AuditControlRepository;
import com.cyberassessment.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
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
                .build();
    }

    @Transactional(readOnly = true)
    public List<AuditControlDto> findByAuditId(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        if (!currentUserService.isAdmin()) {
            User current = currentUserService.getCurrentUserOrThrow();
            if (audit.getAssignedTo() == null || !audit.getAssignedTo().getId().equals(current.getId())) {
                throw new IllegalArgumentException("You do not have access to this audit");
            }
        }
        return auditControlRepository.findByAudit(audit).stream().map(AuditControlService::toDto).collect(Collectors.toList());
    }

    @Transactional
    public AuditControlDto update(Long id, ControlAssessmentStatus status, String evidence, String notes) {
        AuditControl ac = auditControlRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("AuditControl not found: " + id));
        if (status != null) ac.setStatus(status);
        if (evidence != null) ac.setEvidence(evidence);
        if (notes != null) ac.setNotes(notes);
        if (status != null && (status == ControlAssessmentStatus.PASS || status == ControlAssessmentStatus.FAIL || status == ControlAssessmentStatus.NA)) {
            ac.setAssessedAt(java.time.Instant.now());
        }
        ac = auditControlRepository.save(ac);
        return toDto(ac);
    }
}
