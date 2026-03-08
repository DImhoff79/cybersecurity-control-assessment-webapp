package com.cyberassessment.service;

import com.cyberassessment.dto.AuditControlAnswerDto;
import com.cyberassessment.dto.AuditControlAssignmentDto;
import com.cyberassessment.dto.AuditControlDto;
import com.cyberassessment.dto.AuditEvidenceDto;
import com.cyberassessment.dto.MyTaskDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.AuditControlAssignmentRepository;
import com.cyberassessment.repository.AuditControlRepository;
import com.cyberassessment.repository.AuditRepository;
import com.cyberassessment.repository.AuditAssignmentRepository;
import com.cyberassessment.repository.UserRepository;
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
    private final AuditActivityLogService auditActivityLogService;
    private final AuditControlAssignmentRepository auditControlAssignmentRepository;
    private final AuditAssignmentRepository auditAssignmentRepository;
    private final UserRepository userRepository;

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
        List<AuditControlAssignmentDto> assignments = ac.getAssignments().stream()
                .filter(a -> Boolean.TRUE.equals(a.getActive()))
                .map(a -> AuditControlAssignmentDto.builder()
                        .id(a.getId())
                        .auditControlId(ac.getId())
                        .userId(a.getUser().getId())
                        .userEmail(a.getUser().getEmail())
                        .userDisplayName(a.getUser().getDisplayName())
                        .assignmentRole(a.getAssignmentRole())
                        .active(a.getActive())
                        .assignedAt(a.getAssignedAt())
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
                .evidences(evidences)
                .assignments(assignments)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AuditControlDto> findByAuditId(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        if (!currentUserService.isAdmin()) {
            User current = currentUserService.getCurrentUserOrThrow();
            boolean isPrimary = audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(current.getId());
            boolean isAuditCollaborator = auditAssignmentRepository.existsByAuditIdAndUserIdAndActiveTrue(audit.getId(), current.getId());
            if (!isPrimary && !isAuditCollaborator) {
                throw new IllegalArgumentException("You do not have access to this audit");
            }
            if (isPrimary) {
                return auditControlRepository.findByAudit(audit).stream().map(AuditControlService::toDto).collect(Collectors.toList());
            }
            return auditControlRepository.findByAudit(audit).stream()
                    .filter(ac -> auditControlAssignmentRepository.existsByAuditControlIdAndUserIdAndActiveTrue(ac.getId(), current.getId()))
                    .map(AuditControlService::toDto)
                    .collect(Collectors.toList());
        }
        return auditControlRepository.findByAudit(audit).stream().map(AuditControlService::toDto).collect(Collectors.toList());
    }

    @Transactional
    public AuditControlDto update(Long id, ControlAssessmentStatus status, String evidence, String notes) {
        AuditControl ac = auditControlRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("AuditControl not found: " + id));
        if (!currentUserService.isAdmin()) {
            User current = currentUserService.getCurrentUserOrThrow();
            boolean isPrimary = ac.getAudit().getAssignedTo() != null && ac.getAudit().getAssignedTo().getId().equals(current.getId());
            boolean isTaskAssignee = auditControlAssignmentRepository.existsByAuditControlIdAndUserIdAndActiveTrue(ac.getId(), current.getId());
            if (!isPrimary && !isTaskAssignee) {
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

    @Transactional(readOnly = true)
    public List<AuditControlAssignmentDto> listAssignments(Long auditControlId) {
        AuditControl ac = auditControlRepository.findById(auditControlId).orElseThrow(() -> new IllegalArgumentException("AuditControl not found: " + auditControlId));
        if (!currentUserService.isAdmin()) {
            User current = currentUserService.getCurrentUserOrThrow();
            boolean isPrimary = ac.getAudit().getAssignedTo() != null && ac.getAudit().getAssignedTo().getId().equals(current.getId());
            boolean isAssignee = auditControlAssignmentRepository.existsByAuditControlIdAndUserIdAndActiveTrue(auditControlId, current.getId());
            if (!isPrimary && !isAssignee) {
                throw new IllegalArgumentException("You do not have access to this audit control");
            }
        }
        return auditControlAssignmentRepository.findByAuditControlIdAndActiveTrue(auditControlId).stream()
                .map(this::toAssignmentDto)
                .toList();
    }

    @Transactional
    public List<AuditControlAssignmentDto> addAssignment(Long auditControlId, Long userId, AuditControlAssignmentRole role) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can manage task assignments");
        }
        AuditControl ac = auditControlRepository.findById(auditControlId).orElseThrow(() -> new IllegalArgumentException("AuditControl not found: " + auditControlId));
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        AuditControlAssignment assignment = auditControlAssignmentRepository
                .findByAuditControlIdAndUserIdAndAssignmentRole(auditControlId, userId, role)
                .orElse(AuditControlAssignment.builder()
                        .auditControl(ac)
                        .user(user)
                        .assignmentRole(role)
                        .build());
        assignment.setActive(true);
        auditControlAssignmentRepository.save(assignment);
        auditActivityLogService.log(ac.getAudit(), AuditActivityType.AUDIT_ASSIGNED, "Task delegated for control " + ac.getControl().getControlId() + " to " + user.getEmail());
        return listAssignments(auditControlId);
    }

    @Transactional
    public List<AuditControlAssignmentDto> removeAssignment(Long auditControlId, Long assignmentId) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can manage task assignments");
        }
        AuditControlAssignment assignment = auditControlAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Task assignment not found"));
        if (!assignment.getAuditControl().getId().equals(auditControlId)) {
            throw new IllegalArgumentException("Assignment does not belong to this control");
        }
        assignment.setActive(false);
        auditControlAssignmentRepository.save(assignment);
        auditActivityLogService.log(assignment.getAuditControl().getAudit(), AuditActivityType.AUDIT_ASSIGNED,
                "Task assignment removed for control " + assignment.getAuditControl().getControl().getControlId());
        return listAssignments(auditControlId);
    }

    private AuditControlAssignmentDto toAssignmentDto(AuditControlAssignment a) {
        return AuditControlAssignmentDto.builder()
                .id(a.getId())
                .auditControlId(a.getAuditControl().getId())
                .userId(a.getUser().getId())
                .userEmail(a.getUser().getEmail())
                .userDisplayName(a.getUser().getDisplayName())
                .assignmentRole(a.getAssignmentRole())
                .active(a.getActive())
                .assignedAt(a.getAssignedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<MyTaskDto> myTasks() {
        User current = currentUserService.getCurrentUserOrThrow();
        if (currentUserService.isAdmin()) {
            return List.of();
        }
        return auditControlAssignmentRepository.findByUserIdAndActiveTrue(current.getId()).stream()
                .map(a -> {
                    AuditControl ac = a.getAuditControl();
                    Audit audit = ac.getAudit();
                    return MyTaskDto.builder()
                            .assignmentId(a.getId())
                            .auditId(audit.getId())
                            .auditControlId(ac.getId())
                            .applicationName(audit.getApplication().getName())
                            .auditYear(audit.getYear())
                            .controlControlId(ac.getControl().getControlId())
                            .controlName(ac.getControl().getName())
                            .status(ac.getStatus())
                            .notes(ac.getNotes())
                            .assignmentRole(a.getAssignmentRole())
                            .dueAt(audit.getDueAt())
                            .build();
                })
                .sorted((a, b) -> {
                    if (a.getDueAt() == null && b.getDueAt() == null) return 0;
                    if (a.getDueAt() == null) return 1;
                    if (b.getDueAt() == null) return -1;
                    return a.getDueAt().compareTo(b.getDueAt());
                })
                .toList();
    }
}
