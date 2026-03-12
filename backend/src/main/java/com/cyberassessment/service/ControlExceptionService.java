package com.cyberassessment.service;

import com.cyberassessment.dto.ControlExceptionCreateRequest;
import com.cyberassessment.dto.ControlExceptionDecisionRequest;
import com.cyberassessment.dto.ControlExceptionDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.AuditControlRepository;
import com.cyberassessment.repository.AuditRepository;
import com.cyberassessment.repository.ControlExceptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ControlExceptionService {

    private final ControlExceptionRepository controlExceptionRepository;
    private final AuditRepository auditRepository;
    private final AuditControlRepository auditControlRepository;
    private final CurrentUserService currentUserService;
    private final AuditActivityLogService auditActivityLogService;

    @Transactional
    public List<ControlExceptionDto> list(Long auditId) {
        expireElapsedApprovals();
        List<ControlExceptionRequest> items = auditId != null
                ? controlExceptionRepository.findByAuditIdOrderByRequestedAtDesc(auditId)
                : controlExceptionRepository.findAllByOrderByRequestedAtDesc();
        return items.stream().map(ControlExceptionService::toDto).toList();
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
        AuditControl auditControl = resolveAuditControl(request.getAuditControlId(), audit);

        ControlExceptionRequest row = ControlExceptionRequest.builder()
                .audit(audit)
                .auditControl(auditControl)
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
        User requestedBy = row.getRequestedBy();
        User decidedBy = row.getDecidedBy();
        return ControlExceptionDto.builder()
                .id(row.getId())
                .auditId(row.getAudit().getId())
                .auditYear(row.getAudit().getYear())
                .applicationName(row.getAudit().getApplication() != null ? row.getAudit().getApplication().getName() : null)
                .auditControlId(auditControl != null ? auditControl.getId() : null)
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
