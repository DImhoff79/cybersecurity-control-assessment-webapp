package com.cyberassessment.service;

import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.AuditApprovalStepDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.AuditApprovalStepRepository;
import com.cyberassessment.repository.AuditAssignmentRepository;
import com.cyberassessment.repository.AuditRepository;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuditApprovalService {

    private final AuditRepository auditRepository;
    private final AuditApprovalStepRepository auditApprovalStepRepository;
    private final AuditAssignmentRepository auditAssignmentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditActivityLogService auditActivityLogService;

    @Transactional(readOnly = true)
    public List<AuditApprovalStepDto> getWorkflow(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        ensureCanViewWorkflow(audit);
        return auditApprovalStepRepository.findByAuditIdOrderByStepOrderAsc(auditId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Called before setting status to PENDING_APPROVAL on submit. Seeds one step from REVIEWER assignment if empty.
     */
    @Transactional
    public void prepareWorkflowForSubmit(Audit audit) {
        if (audit.getStatus() == AuditStatus.REVISIONS_REQUESTED) {
            resetStepsForResubmission(audit.getId());
        }
        seedDefaultApprovalIfEmpty(audit);
    }

    @Transactional
    public List<AuditApprovalStepDto> replaceWorkflow(Long auditId, List<Long> assigneeUserIds) {
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: AUDIT_MANAGEMENT");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        if (audit.getStatus() == AuditStatus.COMPLETE) {
            throw new IllegalArgumentException("Cannot edit approval workflow for a completed audit");
        }
        if (assigneeUserIds == null || assigneeUserIds.isEmpty()) {
            throw new IllegalArgumentException("At least one auditor assignee is required");
        }
        List<Long> distinctOrder = new ArrayList<>();
        for (Long uid : assigneeUserIds) {
            if (uid == null) {
                throw new IllegalArgumentException("Invalid assignee user id");
            }
            if (!distinctOrder.contains(uid)) {
                distinctOrder.add(uid);
            }
        }
        for (Long userId : distinctOrder) {
            User u = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            if (u.getRole() != UserRole.AUDITOR) {
                throw new IllegalArgumentException("Approval assignees must have role AUDITOR: " + u.getEmail());
            }
        }
        auditApprovalStepRepository.deleteByAudit_Id(auditId);
        int order = 0;
        for (Long userId : distinctOrder) {
            User u = userRepository.findById(userId).orElseThrow();
            AuditApprovalStep step = AuditApprovalStep.builder()
                    .audit(audit)
                    .stepOrder(order++)
                    .assignedTo(u)
                    .stepStatus(AuditApprovalStepStatus.PENDING)
                    .build();
            auditApprovalStepRepository.save(step);
        }
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_APPROVAL_WORKFLOW_UPDATED, "Approval workflow updated");
        return getWorkflow(auditId);
    }

    @Transactional
    public AuditDto approve(Long auditId, String notes) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        if (audit.getStatus() != AuditStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Audit is not awaiting approval");
        }
        List<AuditApprovalStep> steps = auditApprovalStepRepository.findByAuditIdOrderByStepOrderAsc(auditId);
        if (steps.isEmpty()) {
            throw new IllegalStateException("No approval steps configured");
        }
        AuditApprovalStep current = steps.stream()
                .filter(s -> s.getStepStatus() == AuditApprovalStepStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending approval step"));
        User actor = currentUserService.getCurrentUserOrThrow();
        if (!canDecideStep(current, actor)) {
            throw new IllegalArgumentException("You are not authorized to approve this step");
        }
        current.setStepStatus(AuditApprovalStepStatus.APPROVED);
        current.setDecisionNotes(notes != null && !notes.isBlank() ? notes.trim() : null);
        current.setDecidedAt(Instant.now());
        auditApprovalStepRepository.save(current);

        List<AuditApprovalStep> refreshed = auditApprovalStepRepository.findByAuditIdOrderByStepOrderAsc(auditId);
        boolean anyPending = refreshed.stream().anyMatch(s -> s.getStepStatus() == AuditApprovalStepStatus.PENDING);
        if (!anyPending) {
            boolean allApproved = refreshed.stream().allMatch(s -> s.getStepStatus() == AuditApprovalStepStatus.APPROVED);
            if (allApproved) {
                audit.setStatus(AuditStatus.AUDITOR_APPROVED);
                auditRepository.save(audit);
            }
        }
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_APPROVAL_APPROVED, "Approval step approved by " + actor.getEmail());
        return AuditService.toDto(auditRepository.findById(auditId).orElseThrow());
    }

    @Transactional
    public AuditDto requestRevision(Long auditId, String notes) {
        if (notes == null || notes.isBlank()) {
            throw new IllegalArgumentException("Notes are required when returning an audit for revision");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        if (audit.getStatus() != AuditStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Audit is not awaiting approval");
        }
        List<AuditApprovalStep> steps = auditApprovalStepRepository.findByAuditIdOrderByStepOrderAsc(auditId);
        AuditApprovalStep current = steps.stream()
                .filter(s -> s.getStepStatus() == AuditApprovalStepStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending approval step"));
        User actor = currentUserService.getCurrentUserOrThrow();
        if (!canDecideStep(current, actor)) {
            throw new IllegalArgumentException("You are not authorized to return this audit for revision");
        }
        for (AuditApprovalStep s : steps) {
            s.setStepStatus(AuditApprovalStepStatus.PENDING);
            s.setDecisionNotes(null);
            s.setDecidedAt(null);
        }
        auditApprovalStepRepository.saveAll(steps);

        audit.setStatus(AuditStatus.REVISIONS_REQUESTED);
        auditRepository.save(audit);
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_APPROVAL_RETURNED, "Returned for revision: " + notes.trim());
        return AuditService.toDto(audit);
    }

    private void seedDefaultApprovalIfEmpty(Audit audit) {
        if (auditApprovalStepRepository.countByAuditId(audit.getId()) > 0) {
            return;
        }
        List<AuditAssignment> reviewers = auditAssignmentRepository
                .findByAuditIdAndAssignmentRoleAndActiveTrueOrderByAssignedAtAsc(audit.getId(), AuditAssignmentRole.REVIEWER);
        for (AuditAssignment ar : reviewers) {
            User u = ar.getUser();
            if (u.getRole() == UserRole.AUDITOR) {
                AuditApprovalStep step = AuditApprovalStep.builder()
                        .audit(audit)
                        .stepOrder(0)
                        .assignedTo(u)
                        .stepStatus(AuditApprovalStepStatus.PENDING)
                        .build();
                auditApprovalStepRepository.save(step);
                return;
            }
        }
        throw new IllegalArgumentException(
                "Configure an approval workflow (or assign an active REVIEWER with role AUDITOR) before submitting.");
    }

    private void resetStepsForResubmission(Long auditId) {
        List<AuditApprovalStep> steps = auditApprovalStepRepository.findByAuditIdOrderByStepOrderAsc(auditId);
        for (AuditApprovalStep s : steps) {
            s.setStepStatus(AuditApprovalStepStatus.PENDING);
            s.setDecisionNotes(null);
            s.setDecidedAt(null);
        }
        auditApprovalStepRepository.saveAll(steps);
    }

    private boolean canDecideStep(AuditApprovalStep step, User actor) {
        if (currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            return true;
        }
        return step.getAssignedTo() != null && Objects.equals(step.getAssignedTo().getId(), actor.getId());
    }

    private void ensureCanViewWorkflow(Audit audit) {
        User current = currentUserService.getCurrentUserOrThrow();
        if (currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            return;
        }
        boolean direct = audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(current.getId());
        boolean collaborator = auditAssignmentRepository.existsByAuditIdAndUserIdAndActiveTrue(audit.getId(), current.getId());
        boolean appOwner = audit.getApplication().getOwner() != null
                && audit.getApplication().getOwner().getId().equals(current.getId());
        boolean approvalAssignee = auditApprovalStepRepository.existsByAuditIdAndAssignedTo_Id(audit.getId(), current.getId());
        if (!direct && !collaborator && !appOwner && !approvalAssignee) {
            throw new IllegalArgumentException("You do not have access to this audit");
        }
    }

    private AuditApprovalStepDto toDto(AuditApprovalStep s) {
        User u = s.getAssignedTo();
        return AuditApprovalStepDto.builder()
                .id(s.getId())
                .stepOrder(s.getStepOrder())
                .assignedUserId(u != null ? u.getId() : null)
                .assignedUserEmail(u != null ? u.getEmail() : null)
                .assignedUserDisplayName(u != null ? u.getDisplayName() : null)
                .stepStatus(s.getStepStatus())
                .decisionNotes(s.getDecisionNotes())
                .decidedAt(s.getDecidedAt())
                .build();
    }
}
