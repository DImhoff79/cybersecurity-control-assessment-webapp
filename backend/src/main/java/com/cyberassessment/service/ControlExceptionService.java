package com.cyberassessment.service;

import com.cyberassessment.dto.ControlExceptionCreateRequest;
import com.cyberassessment.dto.ControlExceptionDecisionRequest;
import com.cyberassessment.dto.ControlExceptionDto;
import com.cyberassessment.dto.ControlExceptionUpdateRequest;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.ApprovalDelegateRepository;
import com.cyberassessment.repository.AuditAssignmentRepository;
import com.cyberassessment.repository.AuditControlRepository;
import com.cyberassessment.repository.AuditRepository;
import com.cyberassessment.repository.ControlExceptionRepository;
import com.cyberassessment.repository.FindingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ControlExceptionService {

    private final ControlExceptionRepository controlExceptionRepository;
    private final AuditRepository auditRepository;
    private final AuditControlRepository auditControlRepository;
    private final FindingRepository findingRepository;
    private final CurrentUserService currentUserService;
    private final AuditActivityLogService auditActivityLogService;
    private final AuditService auditService;
    private final AuditAssignmentRepository auditAssignmentRepository;
    private final ApprovalDelegateRepository approvalDelegateRepository;

    @Transactional
    public List<ControlExceptionDto> list(Long auditId, Long findingId) {
        expireElapsedApprovals();
        List<ControlExceptionRequest> items;
        if (findingId != null && auditId != null) {
            items = controlExceptionRepository.findByAuditIdAndFinding_IdOrderByRequestedAtDesc(auditId, findingId);
        } else if (findingId != null) {
            items = controlExceptionRepository.findByFinding_IdOrderByRequestedAtDesc(findingId);
        } else if (auditId != null) {
            items = controlExceptionRepository.findByAuditIdOrderByRequestedAtDesc(auditId);
        } else {
            items = controlExceptionRepository.findAllByOrderByRequestedAtDesc();
        }
        return items.stream().map(ControlExceptionService::toDto).toList();
    }

    @Transactional
    public List<ControlExceptionDto> listForWorkspace() {
        expireElapsedApprovals();
        User current = currentUserService.getCurrentUserOrThrow();
        List<Long> auditIds = auditService.findAccessibleAuditIdsForCurrentUser();
        List<ControlExceptionRequest> byAudits = auditIds.isEmpty()
                ? List.of()
                : controlExceptionRepository.findByAudit_IdInOrderByRequestedAtDesc(auditIds);
        List<ControlExceptionRequest> requestedByMe =
                controlExceptionRepository.findByRequestedBy_IdOrderByRequestedAtDesc(current.getId());
        Map<Long, ControlExceptionRequest> merged = new LinkedHashMap<>();
        for (ControlExceptionRequest row : byAudits) {
            merged.put(row.getId(), row);
        }
        for (ControlExceptionRequest row : requestedByMe) {
            merged.putIfAbsent(row.getId(), row);
        }
        List<ControlExceptionRequest> rows = new ArrayList<>(merged.values());
        rows.sort(Comparator.comparing(ControlExceptionRequest::getRequestedAt).reversed());
        return rows.stream().map(ControlExceptionService::toDto).toList();
    }

    /**
     * Single exception for workspace UI, using the same visibility rules as {@link #listForWorkspace()}.
     */
    @Transactional(readOnly = true)
    public ControlExceptionDto findForWorkspace(Long exceptionId) {
        expireElapsedApprovals();
        return controlExceptionRepository.findById(exceptionId)
                .filter(this::canViewInWorkspace)
                .map(ControlExceptionService::toDto)
                .orElse(null);
    }

    private boolean canViewInWorkspace(ControlExceptionRequest row) {
        User current = currentUserService.getCurrentUserOrThrow();
        List<Long> auditIds = auditService.findAccessibleAuditIdsForCurrentUser();
        if (auditIds.contains(row.getAudit().getId())) {
            return true;
        }
        return row.getRequestedBy().getId().equals(current.getId());
    }

    @Transactional
    public ControlExceptionDto requestForWorkspace(ControlExceptionCreateRequest request) {
        auditService.assertCanAccessAudit(request.getAuditId());
        return request(request);
    }

    /**
     * Update a pending (REQUESTED) exception for workspace users: requester, application owner,
     * assignee, or collaborator. Auditors may only edit exceptions they requested.
     */
    @Transactional
    public ControlExceptionDto updateForWorkspace(Long exceptionId, ControlExceptionUpdateRequest request) {
        expireElapsedApprovals();
        ControlExceptionRequest row = controlExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new IllegalArgumentException("Control exception not found"));
        // Do not call assertCanAccessAudit here: ensureCanAccessAudit is stricter than who may edit a
        // pending request (e.g. the requesting auditor is not always assignee/collaborator/owner).
        if (!canEditPendingException(row)) {
            throw new IllegalArgumentException("Not authorized to edit this exception");
        }
        if (row.getStatus() != ControlExceptionStatus.REQUESTED) {
            throw new IllegalArgumentException("Only pending exception requests can be edited");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        Audit audit = row.getAudit();
        Finding finding = resolveFindingForException(request.getFindingId(), audit);
        AuditControl auditControl = resolveAuditControlForException(request.getAuditControlId(), audit, finding);
        row.setReason(request.getReason().trim());
        row.setCompensatingControl(request.getCompensatingControl());
        row.setExpiresAt(request.getExpiresAt());
        row.setFinding(finding);
        row.setAuditControl(auditControl);
        row = controlExceptionRepository.save(row);
        auditActivityLogService.log(audit, AuditActivityType.EXCEPTION_UPDATED, "Control exception request updated");
        return toDto(row);
    }

    private boolean canEditPendingException(ControlExceptionRequest row) {
        if (row.getStatus() != ControlExceptionStatus.REQUESTED) {
            return false;
        }
        User u = currentUserService.getCurrentUserOrThrow();
        if (currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            return true;
        }
        if (u.getRole() == UserRole.AUDITOR) {
            return row.getRequestedBy().getId().equals(u.getId());
        }
        Audit audit = row.getAudit();
        if (row.getRequestedBy().getId().equals(u.getId())) {
            return true;
        }
        if (audit.getApplication().getOwner() != null && audit.getApplication().getOwner().getId().equals(u.getId())) {
            return true;
        }
        if (audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(u.getId())) {
            return true;
        }
        return auditAssignmentRepository.existsByAuditIdAndUserIdAndActiveTrue(audit.getId(), u.getId());
    }

    @Transactional
    public ControlExceptionDto request(ControlExceptionCreateRequest request) {
        if (request.getAuditId() == null) {
            throw new IllegalArgumentException("auditId is required");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        Audit audit = auditRepository.findById(request.getAuditId())
                .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + request.getAuditId()));
        Finding finding = resolveFindingForException(request.getFindingId(), audit);
        AuditControl auditControl = resolveAuditControlForException(request.getAuditControlId(), audit, finding);

        ControlExceptionRequest row = ControlExceptionRequest.builder()
                .audit(audit)
                .auditControl(auditControl)
                .finding(finding)
                .requestedBy(currentUserService.getCurrentUserOrThrow())
                .reason(request.getReason().trim())
                .compensatingControl(request.getCompensatingControl())
                .expiresAt(request.getExpiresAt())
                .status(ControlExceptionStatus.REQUESTED)
                .build();
        row = controlExceptionRepository.save(row);
        auditActivityLogService.log(audit, AuditActivityType.EXCEPTION_REQUESTED, "Control exception requested");
        return toDto(row);
    }

    @Transactional
    public ControlExceptionDto approve(Long exceptionId, ControlExceptionDecisionRequest request) {
        ControlExceptionRequest row = controlExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new IllegalArgumentException("Control exception not found"));
        assertCanDecideControlException(row);
        if (row.getStatus() != ControlExceptionStatus.REQUESTED) {
            throw new IllegalArgumentException("Only requested exceptions can be approved");
        }
        row.setStatus(ControlExceptionStatus.APPROVED);
        row.setDecidedAt(Instant.now());
        row.setDecidedBy(currentUserService.getCurrentUserOrThrow());
        row.setDecisionNotes(request != null ? request.getDecisionNotes() : null);
        if (request != null && request.getExpiresAt() != null) {
            row.setExpiresAt(request.getExpiresAt());
        }
        row = controlExceptionRepository.save(row);
        auditActivityLogService.log(row.getAudit(), AuditActivityType.EXCEPTION_APPROVED, "Control exception approved");
        return toDto(row);
    }

    @Transactional
    public ControlExceptionDto reject(Long exceptionId, ControlExceptionDecisionRequest request) {
        ControlExceptionRequest row = controlExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new IllegalArgumentException("Control exception not found"));
        assertCanDecideControlException(row);
        if (row.getStatus() != ControlExceptionStatus.REQUESTED) {
            throw new IllegalArgumentException("Only requested exceptions can be rejected");
        }
        row.setStatus(ControlExceptionStatus.REJECTED);
        row.setDecidedAt(Instant.now());
        row.setDecidedBy(currentUserService.getCurrentUserOrThrow());
        row.setDecisionNotes(request != null ? request.getDecisionNotes() : null);
        row = controlExceptionRepository.save(row);
        auditActivityLogService.log(row.getAudit(), AuditActivityType.EXCEPTION_REJECTED, "Control exception rejected");
        return toDto(row);
    }

    private void expireElapsedApprovals() {
        Instant now = Instant.now();
        List<ControlExceptionRequest> expired = controlExceptionRepository
                .findByStatusAndExpiresAtBefore(ControlExceptionStatus.APPROVED, now);
        if (expired.isEmpty()) {
            return;
        }
        for (ControlExceptionRequest row : expired) {
            row.setStatus(ControlExceptionStatus.EXPIRED);
            row.setDecidedAt(now);
            auditActivityLogService.log(row.getAudit(), AuditActivityType.EXCEPTION_EXPIRED, "Control exception expired");
        }
        controlExceptionRepository.saveAll(expired);
    }

    private void assertCanDecideControlException(ControlExceptionRequest row) {
        if (!canDecideControlException(row)) {
            throw new IllegalArgumentException("Not authorized to approve or reject this exception");
        }
    }

    private boolean canDecideControlException(ControlExceptionRequest row) {
        User u = currentUserService.getCurrentUserOrThrow();
        if (currentUserService.isAdmin()) {
            return true;
        }
        if (currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            return true;
        }
        if (approvalDelegateRepository.existsByUser_Id(u.getId())) {
            return true;
        }
        Audit audit = row.getAudit();
        if (u.getRole() == UserRole.AUDITOR) {
            return auditorParticipatesInAudit(u, audit);
        }
        return false;
    }

    private boolean auditorParticipatesInAudit(User u, Audit audit) {
        if (audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(u.getId())) {
            return true;
        }
        return auditAssignmentRepository.existsByAuditIdAndUserIdAndActiveTrue(audit.getId(), u.getId());
    }

    private Finding resolveFindingForException(Long findingId, Audit audit) {
        if (findingId == null) {
            return null;
        }
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new IllegalArgumentException("Finding not found: " + findingId));
        if (!finding.getAudit().getId().equals(audit.getId())) {
            throw new IllegalArgumentException("Finding does not belong to the selected audit");
        }
        return finding;
    }

    private AuditControl resolveAuditControlForException(Long requestedAuditControlId, Audit audit, Finding finding) {
        if (finding != null && finding.getAuditControl() != null) {
            AuditControl fromFinding = finding.getAuditControl();
            if (!fromFinding.getAudit().getId().equals(audit.getId())) {
                throw new IllegalArgumentException("Finding control is inconsistent with audit");
            }
            if (requestedAuditControlId != null && !fromFinding.getId().equals(requestedAuditControlId)) {
                throw new IllegalArgumentException("Control selection must match the linked finding's control");
            }
            return fromFinding;
        }
        return resolveAuditControl(requestedAuditControlId, audit);
    }

    private AuditControl resolveAuditControl(Long auditControlId, Audit audit) {
        if (auditControlId == null) {
            return null;
        }
        AuditControl auditControl = auditControlRepository.findById(auditControlId)
                .orElseThrow(() -> new IllegalArgumentException("Audit control not found: " + auditControlId));
        if (!auditControl.getAudit().getId().equals(audit.getId())) {
            throw new IllegalArgumentException("Audit control does not belong to audit");
        }
        return auditControl;
    }

    public static ControlExceptionDto toDto(ControlExceptionRequest row) {
        AuditControl auditControl = row.getAuditControl();
        Finding finding = row.getFinding();
        User requestedBy = row.getRequestedBy();
        User decidedBy = row.getDecidedBy();
        return ControlExceptionDto.builder()
                .id(row.getId())
                .auditId(row.getAudit().getId())
                .auditYear(row.getAudit().getYear())
                .applicationName(row.getAudit().getApplication() != null ? row.getAudit().getApplication().getName() : null)
                .auditControlId(auditControl != null ? auditControl.getId() : null)
                .findingId(finding != null ? finding.getId() : null)
                .findingTitle(finding != null ? finding.getTitle() : null)
                .controlId(auditControl != null ? auditControl.getControl().getControlId() : null)
                .controlName(auditControl != null ? auditControl.getControl().getName() : null)
                .status(row.getStatus())
                .reason(row.getReason())
                .compensatingControl(row.getCompensatingControl())
                .decisionNotes(row.getDecisionNotes())
                .requestedByUserId(requestedBy != null ? requestedBy.getId() : null)
                .requestedByEmail(requestedBy != null ? requestedBy.getEmail() : null)
                .requestedByDisplayName(requestedBy != null ? requestedBy.getDisplayName() : null)
                .decidedByUserId(decidedBy != null ? decidedBy.getId() : null)
                .decidedByEmail(decidedBy != null ? decidedBy.getEmail() : null)
                .decidedByDisplayName(decidedBy != null ? decidedBy.getDisplayName() : null)
                .requestedAt(row.getRequestedAt())
                .decidedAt(row.getDecidedAt())
                .expiresAt(row.getExpiresAt())
                .slaState(computeSlaState(row))
                .build();
    }

    private static String computeSlaState(ControlExceptionRequest row) {
        Instant now = Instant.now();
        if (row.getStatus() == ControlExceptionStatus.EXPIRED) {
            return "BREACHED";
        }
        if (row.getStatus() == ControlExceptionStatus.REQUESTED) {
            return "PENDING_DECISION";
        }
        if (row.getExpiresAt() == null) {
            return "NO_EXPIRY";
        }
        if (row.getExpiresAt().isBefore(now)) {
            return "BREACHED";
        }
        if (row.getExpiresAt().isBefore(now.plusSeconds(7 * 24 * 60 * 60L))) {
            return "AT_RISK";
        }
        return "ON_TRACK";
    }
}
