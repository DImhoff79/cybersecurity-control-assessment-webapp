package com.cyberassessment.service;

import com.cyberassessment.dto.ReportSummaryDto;
import com.cyberassessment.dto.AuditYearSummaryDto;
import com.cyberassessment.dto.AuditorAuditItemDto;
import com.cyberassessment.dto.AuditorDashboardDto;
import com.cyberassessment.dto.AuditorEvidenceItemDto;
import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditControl;
import com.cyberassessment.entity.AuditEvidence;
import com.cyberassessment.entity.AuditStatus;
import com.cyberassessment.entity.EvidenceReviewStatus;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.AuditEvidenceRepository;
import com.cyberassessment.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ApplicationRepository applicationRepository;
    private final AuditRepository auditRepository;
    private final AuditEvidenceRepository auditEvidenceRepository;

    @Transactional(readOnly = true)
    public ReportSummaryDto getSummary() {
        List<Audit> audits = auditRepository.findAll();
        Instant now = Instant.now();
        long totalAudits = audits.size();
        long openAudits = audits.stream()
                .filter(a -> a.getStatus() == AuditStatus.DRAFT || a.getStatus() == AuditStatus.IN_PROGRESS)
                .count();
        long overdueAudits = audits.stream()
                .filter(a -> a.getDueAt() != null && a.getDueAt().isBefore(now))
                .filter(a -> a.getStatus() == AuditStatus.DRAFT || a.getStatus() == AuditStatus.IN_PROGRESS || a.getStatus() == AuditStatus.SUBMITTED)
                .count();
        long submittedAudits = audits.stream().filter(a -> a.getStatus() == AuditStatus.SUBMITTED).count();
        long attestedAudits = audits.stream().filter(a -> a.getStatus() == AuditStatus.ATTESTED).count();
        long completedAudits = audits.stream().filter(a -> a.getStatus() == AuditStatus.COMPLETE).count();
        return ReportSummaryDto.builder()
                .totalApplications(applicationRepository.count())
                .totalAudits(totalAudits)
                .openAudits(openAudits)
                .overdueAudits(overdueAudits)
                .submittedAudits(submittedAudits)
                .attestedAudits(attestedAudits)
                .completedAudits(completedAudits)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AuditYearSummaryDto> byYear() {
        Map<Integer, List<Audit>> grouped = auditRepository.findAll().stream()
                .collect(Collectors.groupingBy(Audit::getYear));
        return grouped.entrySet().stream()
                .map(entry -> {
                    Integer year = entry.getKey();
                    List<Audit> audits = entry.getValue();
                    return AuditYearSummaryDto.builder()
                            .year(year)
                            .total(audits.size())
                            .draft(audits.stream().filter(a -> a.getStatus() == AuditStatus.DRAFT).count())
                            .inProgress(audits.stream().filter(a -> a.getStatus() == AuditStatus.IN_PROGRESS).count())
                            .submitted(audits.stream().filter(a -> a.getStatus() == AuditStatus.SUBMITTED).count())
                            .attested(audits.stream().filter(a -> a.getStatus() == AuditStatus.ATTESTED).count())
                            .complete(audits.stream().filter(a -> a.getStatus() == AuditStatus.COMPLETE).count())
                            .build();
                })
                .sorted(Comparator.comparing(AuditYearSummaryDto::getYear).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public String auditsCsv() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
        StringBuilder sb = new StringBuilder();
        sb.append("audit_id,application,year,status,assigned_to,due_date,submitted_at,attested_at,completed_at\n");
        for (Audit a : auditRepository.findAll()) {
            sb.append(a.getId()).append(",")
                    .append(csv(a.getApplication().getName())).append(",")
                    .append(a.getYear()).append(",")
                    .append(a.getStatus()).append(",")
                    .append(csv(a.getAssignedTo() != null ? a.getAssignedTo().getEmail() : "")).append(",")
                    .append(csv(a.getDueAt() != null ? formatter.format(a.getDueAt()) : "")).append(",")
                    .append(csv(a.getCompletedAt() != null ? formatter.format(a.getCompletedAt()) : "")).append(",")
                    .append(csv(a.getAttestedAt() != null ? formatter.format(a.getAttestedAt()) : "")).append(",")
                    .append(csv(a.getCompletedAt() != null ? formatter.format(a.getCompletedAt()) : "")).append("\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public AuditorDashboardDto auditorDashboard() {
        List<Audit> audits = auditRepository.findAll();
        List<Audit> prioritized = audits.stream()
                .filter(a -> a.getStatus() == AuditStatus.SUBMITTED || a.getStatus() == AuditStatus.ATTESTED || a.getStatus() == AuditStatus.IN_PROGRESS)
                .sorted((a, b) -> {
                    Instant ad = a.getDueAt();
                    Instant bd = b.getDueAt();
                    if (ad == null && bd == null) return 0;
                    if (ad == null) return 1;
                    if (bd == null) return -1;
                    return ad.compareTo(bd);
                })
                .limit(50)
                .toList();

        List<AuditorAuditItemDto> auditsNeedingAttention = prioritized.stream().map(a -> AuditorAuditItemDto.builder()
                .auditId(a.getId())
                .applicationName(a.getApplication().getName())
                .year(a.getYear())
                .status(a.getStatus())
                .assignedToEmail(a.getAssignedTo() != null ? a.getAssignedTo().getEmail() : null)
                .dueAt(a.getDueAt())
                .frameworks(a.getAuditControls().stream()
                        .map(ac -> ac.getControl().getFramework().name())
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", ")))
                .pendingEvidenceCount(a.getAuditControls().stream()
                        .flatMap(ac -> ac.getEvidences().stream())
                        .filter(e -> e.getReviewStatus() == EvidenceReviewStatus.PENDING)
                        .count())
                .build()).toList();

        List<AuditEvidence> evidenceRows = auditEvidenceRepository.findByReviewStatusOrderByCreatedAtDesc(EvidenceReviewStatus.PENDING)
                .stream().limit(100).toList();
        List<AuditorEvidenceItemDto> evidenceQueue = evidenceRows.stream().map(e -> {
            AuditControl ac = e.getAuditControl();
            Audit a = ac.getAudit();
            return AuditorEvidenceItemDto.builder()
                    .evidenceId(e.getId())
                    .auditId(a.getId())
                    .auditControlId(ac.getId())
                    .applicationName(a.getApplication().getName())
                    .year(a.getYear())
                    .controlControlId(ac.getControl().getControlId())
                    .controlName(ac.getControl().getName())
                    .framework(ac.getControl().getFramework().name())
                    .title(e.getTitle())
                    .evidenceType(e.getEvidenceType())
                    .reviewStatus(e.getReviewStatus())
                    .uri(e.getUri())
                    .fileName(e.getFileName())
                    .notes(e.getNotes())
                    .createdAt(e.getCreatedAt())
                    .build();
        }).toList();

        return AuditorDashboardDto.builder()
                .summary(getSummary())
                .auditsNeedingAttention(auditsNeedingAttention)
                .evidenceQueue(evidenceQueue)
                .build();
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
