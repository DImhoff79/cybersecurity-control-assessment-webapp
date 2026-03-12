package com.cyberassessment.service;

import com.cyberassessment.dto.FindingDto;
import com.cyberassessment.dto.FindingUpsertRequest;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.AuditControlRepository;
import com.cyberassessment.repository.AuditRepository;
import com.cyberassessment.repository.FindingRepository;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FindingService {

    private final FindingRepository findingRepository;
    private final AuditRepository auditRepository;
    private final AuditControlRepository auditControlRepository;
    private final UserRepository userRepository;
    private final AuditActivityLogService auditActivityLogService;

    @Transactional(readOnly = true)
    public List<FindingDto> list(Long auditId) {
        List<Finding> findings = auditId != null
                ? findingRepository.findByAuditIdOrderByDueAtAscCreatedAtDesc(auditId)
                : findingRepository.findAllByOrderByDueAtAscCreatedAtDesc();
        return findings.stream().map(FindingService::toDto).toList();
    }

    @Transactional
    public FindingDto create(FindingUpsertRequest request) {
        if (request.getAuditId() == null) {
            throw new IllegalArgumentException("auditId is required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (request.getSeverity() == null) {
            throw new IllegalArgumentException("severity is required");
        }
        Audit audit = auditRepository.findById(request.getAuditId())
                .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + request.getAuditId()));
        AuditControl auditControl = resolveAuditControl(request.getAuditControlId(), audit);
        User owner = resolveOwner(request.getOwnerUserId());
        Finding finding = Finding.builder()
                .audit(audit)
                .auditControl(auditControl)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .status(request.getStatus() != null ? request.getStatus() : FindingStatus.OPEN)
                .owner(owner)
                .dueAt(request.getDueAt())
                .resolvedAt(request.getStatus() == FindingStatus.RESOLVED ? Instant.now() : null)
                .build();
        finding = findingRepository.save(finding);
        auditActivityLogService.log(audit, AuditActivityType.FINDING_CREATED, "Finding created: " + finding.getTitle());
        return toDto(finding);
    }

    @Transactional
    public FindingDto update(Long findingId, FindingUpsertRequest request) {
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new IllegalArgumentException("Finding not found: " + findingId));

        Long auditId = request.getAuditId() != null ? request.getAuditId() : finding.getAudit().getId();
        Audit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        finding.setAudit(audit);

        if (request.getAuditControlId() != null || finding.getAuditControl() != null) {
            finding.setAuditControl(resolveAuditControl(request.getAuditControlId(), audit));
        }
        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("title cannot be empty");
            }
            finding.setTitle(title);
        }
        if (request.getDescription() != null) {
            finding.setDescription(request.getDescription());
        }
        if (request.getSeverity() != null) {
            finding.setSeverity(request.getSeverity());
        }
        if (request.getOwnerUserId() != null || finding.getOwner() != null) {
            finding.setOwner(resolveOwner(request.getOwnerUserId()));
        }
        finding.setDueAt(request.getDueAt());
        if (request.getStatus() != null) {
            finding.setStatus(request.getStatus());
            if (request.getStatus() == FindingStatus.RESOLVED && finding.getResolvedAt() == null) {
                finding.setResolvedAt(Instant.now());
            } else if (request.getStatus() != FindingStatus.RESOLVED) {
                finding.setResolvedAt(null);
            }
        }
        finding = findingRepository.save(finding);

        AuditActivityType activityType = finding.getStatus() == FindingStatus.RESOLVED
                ? AuditActivityType.FINDING_RESOLVED
                : AuditActivityType.FINDING_UPDATED;
        auditActivityLogService.log(finding.getAudit(), activityType, "Finding updated: " + finding.getTitle());
        return toDto(finding);
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

    private User resolveOwner(Long ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        return userRepository.findById(ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Owner user not found: " + ownerUserId));
    }

    public static FindingDto toDto(Finding finding) {
        Audit audit = finding.getAudit();
        AuditControl auditControl = finding.getAuditControl();
        User owner = finding.getOwner();
        return FindingDto.builder()
                .id(finding.getId())
                .auditId(audit.getId())
                .auditYear(audit.getYear())
                .applicationName(audit.getApplication() != null ? audit.getApplication().getName() : null)
                .auditControlId(auditControl != null ? auditControl.getId() : null)
                .controlId(auditControl != null ? auditControl.getControl().getControlId() : null)
                .controlName(auditControl != null ? auditControl.getControl().getName() : null)
                .title(finding.getTitle())
                .description(finding.getDescription())
                .severity(finding.getSeverity())
                .status(finding.getStatus())
                .ownerUserId(owner != null ? owner.getId() : null)
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerDisplayName(owner != null ? owner.getDisplayName() : null)
                .dueAt(finding.getDueAt())
                .resolvedAt(finding.getResolvedAt())
                .reminderSentAt(finding.getReminderSentAt())
                .escalatedAt(finding.getEscalatedAt())
                .slaState(computeSlaState(finding))
                .createdAt(finding.getCreatedAt())
                .updatedAt(finding.getUpdatedAt())
                .build();
    }

    private static String computeSlaState(Finding finding) {
        if (finding.getStatus() == FindingStatus.RESOLVED) {
            return "RESOLVED";
        }
        if (finding.getDueAt() == null) {
            return "NO_DUE_DATE";
        }
        Instant now = Instant.now();
        if (finding.getDueAt().isBefore(now)) {
            return "BREACHED";
        }
        Instant atRiskThreshold = now.plusSeconds(3 * 24 * 60 * 60L);
        if (finding.getDueAt().isBefore(atRiskThreshold)) {
            return "AT_RISK";
        }
        return "ON_TRACK";
    }
}
