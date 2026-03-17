package com.cyberassessment.service;

import com.cyberassessment.dto.RemediationActionDto;
import com.cyberassessment.dto.RemediationPlanDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.RemediationActionRepository;
import com.cyberassessment.repository.RemediationPlanRepository;
import com.cyberassessment.repository.RiskRegisterItemRepository;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RemediationService {

    private final RemediationPlanRepository remediationPlanRepository;
    private final RemediationActionRepository remediationActionRepository;
    private final RiskRegisterItemRepository riskRegisterItemRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public static RemediationPlanDto toDto(RemediationPlan row) {
        return RemediationPlanDto.builder()
                .id(row.getId())
                .riskId(row.getRisk() != null ? row.getRisk().getId() : null)
                .riskTitle(row.getRisk() != null ? row.getRisk().getTitle() : null)
                .title(row.getTitle())
                .proposedPlan(row.getProposedPlan())
                .timeframeText(row.getTimeframeText())
                .compensatingControls(row.getCompensatingControls())
                .planRationale(row.getPlanRationale())
                .status(row.getStatus())
                .approvalStatus(row.getApprovalStatus())
                .approvalNotes(row.getApprovalNotes())
                .approvedByUserId(row.getApprovedBy() != null ? row.getApprovedBy().getId() : null)
                .approvedByEmail(row.getApprovedBy() != null ? row.getApprovedBy().getEmail() : null)
                .approvedAt(row.getApprovedAt())
                .targetCompleteAt(row.getTargetCompleteAt())
                .createdByUserId(row.getCreatedBy() != null ? row.getCreatedBy().getId() : null)
                .createdByEmail(row.getCreatedBy() != null ? row.getCreatedBy().getEmail() : null)
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }

    public static RemediationActionDto toDto(RemediationAction row) {
        return RemediationActionDto.builder()
                .id(row.getId())
                .planId(row.getPlan().getId())
                .actionTitle(row.getActionTitle())
                .actionDetail(row.getActionDetail())
                .ownerUserId(row.getOwner() != null ? row.getOwner().getId() : null)
                .ownerEmail(row.getOwner() != null ? row.getOwner().getEmail() : null)
                .dueAt(row.getDueAt())
                .status(row.getStatus())
                .sequenceNo(row.getSequenceNo())
                .completedAt(row.getCompletedAt())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RemediationPlanDto> listPlans() {
        return remediationPlanRepository.findAll().stream()
                .sorted(Comparator.comparing(RemediationPlan::getUpdatedAt).reversed())
                .map(RemediationService::toDto)
                .toList();
    }

    @Transactional
    public RemediationPlanDto createPlan(
            Long riskId,
            String title,
            String proposedPlan,
            String timeframeText,
            String compensatingControls,
            String planRationale,
            Instant targetCompleteAt
    ) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Plan title is required");
        RiskRegisterItem risk = riskId == null ? null : riskRegisterItemRepository.findById(riskId)
                .orElseThrow(() -> new IllegalArgumentException("Risk not found: " + riskId));
        User actor = currentUserService.getCurrentUser().orElse(null);
        RemediationPlan row = remediationPlanRepository.save(RemediationPlan.builder()
                .risk(risk)
                .title(title.trim())
                .proposedPlan(trimToNull(proposedPlan))
                .timeframeText(trimToNull(timeframeText))
                .compensatingControls(trimToNull(compensatingControls))
                .planRationale(trimToNull(planRationale))
                .status(RemediationPlanStatus.DRAFT)
                .approvalStatus(RemediationPlanApprovalStatus.DRAFT)
                .targetCompleteAt(targetCompleteAt)
                .createdBy(actor)
                .build());
        return toDto(row);
    }

    @Transactional
    public RemediationPlanDto updatePlan(
            Long planId,
            String title,
            String proposedPlan,
            String timeframeText,
            String compensatingControls,
            String planRationale,
            RemediationPlanStatus status,
            Instant targetCompleteAt
    ) {
        RemediationPlan row = remediationPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Remediation plan not found: " + planId));
        boolean documentationChanged = false;
        if (title != null && !title.isBlank()) row.setTitle(title.trim());
        if (proposedPlan != null) {
            row.setProposedPlan(trimToNull(proposedPlan));
            documentationChanged = true;
        }
        if (timeframeText != null) {
            row.setTimeframeText(trimToNull(timeframeText));
            documentationChanged = true;
        }
        if (compensatingControls != null) {
            row.setCompensatingControls(trimToNull(compensatingControls));
            documentationChanged = true;
        }
        if (planRationale != null) {
            row.setPlanRationale(trimToNull(planRationale));
            documentationChanged = true;
        }
        if (status != null) {
            if (status != RemediationPlanStatus.DRAFT && row.getApprovalStatus() != RemediationPlanApprovalStatus.APPROVED) {
                throw new IllegalArgumentException("Plan must be approved by an admin before changing execution status");
            }
            row.setStatus(status);
        }
        if (targetCompleteAt != null) row.setTargetCompleteAt(targetCompleteAt);
        if (documentationChanged && row.getApprovalStatus() == RemediationPlanApprovalStatus.APPROVED) {
            row.setApprovalStatus(RemediationPlanApprovalStatus.DRAFT);
            row.setApprovalNotes("Plan content changed after approval; resubmission required");
            row.setApprovedBy(null);
            row.setApprovedAt(null);
            row.setStatus(RemediationPlanStatus.DRAFT);
        }
        return toDto(remediationPlanRepository.save(row));
    }

    @Transactional
    public RemediationPlanDto submitForApproval(Long planId) {
        RemediationPlan row = remediationPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Remediation plan not found: " + planId));
        if (isBlank(row.getProposedPlan()) || isBlank(row.getPlanRationale()) || isBlank(row.getTimeframeText())) {
            throw new IllegalArgumentException("Proposed plan, timeframe, and rationale are required before submission");
        }
        row.setApprovalStatus(RemediationPlanApprovalStatus.SUBMITTED);
        row.setApprovalNotes(null);
        row.setApprovedBy(null);
        row.setApprovedAt(null);
        return toDto(remediationPlanRepository.save(row));
    }

    @Transactional
    public RemediationPlanDto decideApproval(Long planId, boolean approved, String notes) {
        RemediationPlan row = remediationPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Remediation plan not found: " + planId));
        if (row.getApprovalStatus() != RemediationPlanApprovalStatus.SUBMITTED) {
            throw new IllegalArgumentException("Only submitted plans can be approved or rejected");
        }
        User actor = currentUserService.getCurrentUserOrThrow();
        row.setApprovalStatus(approved ? RemediationPlanApprovalStatus.APPROVED : RemediationPlanApprovalStatus.REJECTED);
        row.setApprovalNotes(trimToNull(notes));
        row.setApprovedBy(actor);
        row.setApprovedAt(Instant.now());
        if (!approved) {
            row.setStatus(RemediationPlanStatus.DRAFT);
        }
        return toDto(remediationPlanRepository.save(row));
    }

    @Transactional
    public RemediationActionDto createAction(
            Long planId,
            String actionTitle,
            String actionDetail,
            Long ownerUserId,
            Instant dueAt,
            Integer sequenceNo
    ) {
        if (actionTitle == null || actionTitle.isBlank()) throw new IllegalArgumentException("Action title is required");
        RemediationPlan plan = remediationPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Remediation plan not found: " + planId));
        if (plan.getApprovalStatus() != RemediationPlanApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Plan must be approved by an admin before actions can be created");
        }
        User owner = ownerUserId == null ? null : userRepository.findById(ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Owner user not found: " + ownerUserId));
        RemediationAction row = remediationActionRepository.save(RemediationAction.builder()
                .plan(plan)
                .actionTitle(actionTitle.trim())
                .actionDetail(actionDetail)
                .owner(owner)
                .dueAt(dueAt)
                .status(RemediationActionStatus.TODO)
                .sequenceNo(sequenceNo == null ? 1 : Math.max(1, sequenceNo))
                .build());
        return toDto(row);
    }

    @Transactional
    public RemediationActionDto updateAction(Long actionId, RemediationActionStatus status, Instant completedAt, Instant dueAt) {
        RemediationAction row = remediationActionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Remediation action not found: " + actionId));
        if (status != null) row.setStatus(status);
        if (completedAt != null) row.setCompletedAt(completedAt);
        if (dueAt != null) row.setDueAt(dueAt);
        if (status == RemediationActionStatus.DONE && row.getCompletedAt() == null) {
            row.setCompletedAt(Instant.now());
        }
        return toDto(remediationActionRepository.save(row));
    }

    @Transactional(readOnly = true)
    public List<RemediationActionDto> actions(Long planId) {
        return remediationActionRepository.findByPlanIdOrderBySequenceNoAscIdAsc(planId).stream()
                .map(RemediationService::toDto)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
