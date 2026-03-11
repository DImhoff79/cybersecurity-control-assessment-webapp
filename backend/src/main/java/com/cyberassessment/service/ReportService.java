package com.cyberassessment.service;

import com.cyberassessment.dto.ReportSummaryDto;
import com.cyberassessment.dto.AuditYearSummaryDto;
import com.cyberassessment.dto.AuditTrendPointDto;
import com.cyberassessment.dto.AuditProjectSummaryDto;
import com.cyberassessment.dto.AuditorAuditItemDto;
import com.cyberassessment.dto.AuditorDashboardDto;
import com.cyberassessment.dto.AuditorEvidenceItemDto;
import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditControl;
import com.cyberassessment.entity.AuditEvidence;
import com.cyberassessment.entity.AuditProjectStatus;
import com.cyberassessment.entity.AuditStatus;
import com.cyberassessment.entity.EvidenceReviewStatus;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.AuditEvidenceRepository;
import com.cyberassessment.repository.AuditProjectRepository;
import com.cyberassessment.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ApplicationRepository applicationRepository;
    private final AuditRepository auditRepository;
    private final AuditProjectRepository auditProjectRepository;
    private final AuditEvidenceRepository auditEvidenceRepository;

    @Transactional(readOnly = true)
    public ReportSummaryDto getSummary() {
        Instant now = Instant.now();
        long totalAudits = auditRepository.count();
        long openAudits = auditRepository.countByStatusIn(List.of(AuditStatus.DRAFT, AuditStatus.IN_PROGRESS));
        long overdueAudits = auditRepository.countByDueAtBeforeAndStatusIn(now, List.of(AuditStatus.DRAFT, AuditStatus.IN_PROGRESS, AuditStatus.SUBMITTED));
        long submittedAudits = auditRepository.countByStatus(AuditStatus.SUBMITTED);
        long attestedAudits = auditRepository.countByStatus(AuditStatus.ATTESTED);
        long completedAudits = auditRepository.countByStatus(AuditStatus.COMPLETE);
        long totalProjects = auditProjectRepository.count();
        long activeProjects = auditProjectRepository.countByStatus(AuditProjectStatus.ACTIVE);
        return ReportSummaryDto.builder()
                .totalApplications(applicationRepository.count())
                .totalAuditProjects(totalProjects)
                .activeAuditProjects(activeProjects)
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
    public List<AuditTrendPointDto> trends() {
        List<Audit> audits = auditRepository.findAll();
        Instant now = Instant.now();
        Map<Integer, List<Audit>> grouped = audits.stream().collect(Collectors.groupingBy(Audit::getYear));
        return grouped.entrySet().stream()
                .map(entry -> {
                    Integer year = entry.getKey();
                    List<Audit> rows = entry.getValue();
                    long total = rows.size();
                    long draft = rows.stream().filter(a -> a.getStatus() == AuditStatus.DRAFT).count();
                    long inProgress = rows.stream().filter(a -> a.getStatus() == AuditStatus.IN_PROGRESS).count();
                    long submitted = rows.stream().filter(a -> a.getStatus() == AuditStatus.SUBMITTED).count();
                    long attested = rows.stream().filter(a -> a.getStatus() == AuditStatus.ATTESTED).count();
                    long complete = rows.stream().filter(a -> a.getStatus() == AuditStatus.COMPLETE).count();
                    long open = draft + inProgress + submitted + attested;
                    long overdue = rows.stream()
                            .filter(a -> a.getDueAt() != null && a.getDueAt().isBefore(now))
                            .filter(a -> a.getStatus() != AuditStatus.COMPLETE)
                            .count();
                    return AuditTrendPointDto.builder()
                            .year(year)
                            .total(total)
                            .open(open)
                            .overdue(overdue)
                            .submitted(submitted)
                            .attested(attested)
                            .complete(complete)
                            .completionRatePct(pct(complete, total))
                            .overdueRatePct(pct(overdue, total))
                            .build();
                })
                .sorted(Comparator.comparing(AuditTrendPointDto::getYear))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditProjectSummaryDto> byProject() {
        return auditProjectRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> {
                    List<Audit> audits = p.getAudits();
                    long total = audits.size();
                    long complete = audits.stream().filter(a -> a.getStatus() == AuditStatus.COMPLETE).count();
                    long submitted = audits.stream().filter(a -> a.getStatus() == AuditStatus.SUBMITTED).count();
                    long attested = audits.stream().filter(a -> a.getStatus() == AuditStatus.ATTESTED).count();
                    long open = audits.stream()
                            .filter(a -> a.getStatus() == AuditStatus.DRAFT || a.getStatus() == AuditStatus.IN_PROGRESS)
                            .count();
                    return AuditProjectSummaryDto.builder()
                            .projectId(p.getId())
                            .projectName(p.getName())
                            .frameworkTag(p.getFrameworkTag())
                            .year(p.getYear())
                            .status(p.getStatus())
                            .scopedApplications(p.getApplications().size())
                            .totalAudits(total)
                            .openAudits(open)
                            .submittedAudits(submitted)
                            .attestedAudits(attested)
                            .completeAudits(complete)
                            .createdAt(p.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public String auditsCsv() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
        StringBuilder sb = new StringBuilder();
        sb.append("audit_id,project,application,year,status,assigned_to,due_date,submitted_at,attested_at,completed_at\n");
        int pageNo = 0;
        while (true) {
            Page<Audit> page = auditRepository.findAllPaged(PageRequest.of(pageNo, 500));
            for (Audit a : page.getContent()) {
                sb.append(a.getId()).append(",")
                        .append(csv(a.getAuditProject() != null ? a.getAuditProject().getName() : "")).append(",")
                        .append(csv(a.getApplication().getName())).append(",")
                        .append(a.getYear()).append(",")
                        .append(a.getStatus()).append(",")
                        .append(csv(a.getAssignedTo() != null ? a.getAssignedTo().getEmail() : "")).append(",")
                        .append(csv(a.getDueAt() != null ? formatter.format(a.getDueAt()) : "")).append(",")
                        .append(csv(a.getCompletedAt() != null ? formatter.format(a.getCompletedAt()) : "")).append(",")
                        .append(csv(a.getAttestedAt() != null ? formatter.format(a.getAttestedAt()) : "")).append(",")
                        .append(csv(a.getCompletedAt() != null ? formatter.format(a.getCompletedAt()) : "")).append("\n");
            }
            if (!page.hasNext()) {
                break;
            }
            pageNo += 1;
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public AuditorDashboardDto auditorDashboard() {
        List<Audit> prioritized = auditRepository.findByStatusInOrderByDueAtAsc(
                List.of(AuditStatus.SUBMITTED, AuditStatus.ATTESTED, AuditStatus.IN_PROGRESS),
                PageRequest.of(0, 50)
        ).getContent();

        List<AuditorAuditItemDto> auditsNeedingAttention = prioritized.stream().map(a -> AuditorAuditItemDto.builder()
                .auditId(a.getId())
                .projectId(a.getAuditProject() != null ? a.getAuditProject().getId() : null)
                .projectName(a.getAuditProject() != null ? a.getAuditProject().getName() : null)
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
                    .projectId(a.getAuditProject() != null ? a.getAuditProject().getId() : null)
                    .projectName(a.getAuditProject() != null ? a.getAuditProject().getName() : null)
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

    @Transactional(readOnly = true)
    public byte[] boardPackPdf() {
        ReportSummaryDto summary = getSummary();
        List<AuditTrendPointDto> trends = trends();
        List<Audit> topOverdue = auditRepository.findTop12ByDueAtBeforeAndStatusNotOrderByDueAtAsc(Instant.now(), AuditStatus.COMPLETE);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            float y = 760;

            y = writeLine(content, y, 14, "Cybersecurity Audit Program Board Pack");
            y = writeLine(content, y, 10, "Generated: " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            y -= 8;
            y = writeLine(content, y, 12, "Program Summary");
            y = writeLine(content, y, 10, "Applications: " + summary.getTotalApplications());
            y = writeLine(content, y, 10, "Total Audits: " + summary.getTotalAudits());
            y = writeLine(content, y, 10, "Open Audits: " + summary.getOpenAudits());
            y = writeLine(content, y, 10, "Overdue Audits: " + summary.getOverdueAudits());
            y = writeLine(content, y, 10, "Submitted: " + summary.getSubmittedAudits() + ", Attested: " + summary.getAttestedAudits() + ", Completed: " + summary.getCompletedAudits());
            y -= 8;

            y = writeLine(content, y, 12, "Yearly Trends");
            y = writeLine(content, y, 10, "Year | Total | Open | Overdue | Completion % | Overdue %");
            for (AuditTrendPointDto t : trends) {
                String line = t.getYear() + " | " + t.getTotal() + " | " + t.getOpen() + " | " + t.getOverdue()
                        + " | " + formatPct(t.getCompletionRatePct()) + " | " + formatPct(t.getOverdueRatePct());
                y = writeLine(content, y, 10, line);
            }
            y -= 8;

            y = writeLine(content, y, 12, "Top Overdue Audits");
            if (topOverdue.isEmpty()) {
                y = writeLine(content, y, 10, "None");
            } else {
                for (Audit a : topOverdue) {
                    String line = "#" + a.getId() + " " + a.getApplication().getName() + " (" + a.getYear() + ") - "
                            + a.getStatus() + " due " + DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(a.getDueAt());
                    y = writeLine(content, y, 10, line);
                    if (y < 70) break;
                }
            }

            content.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to generate board pack PDF");
        }
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private double pct(long value, long total) {
        if (total <= 0) return 0.0;
        return BigDecimal.valueOf(value * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatPct(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private float writeLine(PDPageContentStream content, float y, int size, String text) throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), size);
        content.newLineAtOffset(40, y);
        content.showText(text);
        content.endText();
        return y - (size + 4);
    }
}
