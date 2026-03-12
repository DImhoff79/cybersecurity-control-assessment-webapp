package com.cyberassessment.service;

import com.cyberassessment.dto.ReportScheduleDto;
import com.cyberassessment.dto.ReportScheduleUpsertRequest;
import com.cyberassessment.entity.ReportSchedule;
import com.cyberassessment.entity.ReportScheduleFrequency;
import com.cyberassessment.entity.ReportScheduleType;
import com.cyberassessment.entity.User;
import com.cyberassessment.repository.ReportScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportScheduleService {
    private final ReportScheduleRepository reportScheduleRepository;
    private final CurrentUserService currentUserService;
    private final ReportService reportService;
    private final AsyncMailService asyncMailService;

    @Transactional(readOnly = true)
    public List<ReportScheduleDto> list() {
        return reportScheduleRepository.findAll().stream()
                .map(ReportScheduleService::toDto)
                .toList();
    }

    @Transactional
    public ReportScheduleDto create(ReportScheduleUpsertRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (request.getReportType() == null) {
            throw new IllegalArgumentException("reportType is required");
        }
        if (request.getFrequency() == null) {
            throw new IllegalArgumentException("frequency is required");
        }
        if (request.getRecipientEmails() == null || request.getRecipientEmails().isBlank()) {
            throw new IllegalArgumentException("recipientEmails is required");
        }
        User current = currentUserService.getCurrentUserOrThrow();
        ReportSchedule schedule = ReportSchedule.builder()
                .name(request.getName().trim())
                .reportType(request.getReportType())
                .frequency(request.getFrequency())
                .recipientEmails(request.getRecipientEmails().trim())
                .categoryFilter(request.getCategoryFilter())
                .searchFilter(request.getSearchFilter())
                .projectIdFilter(request.getProjectIdFilter())
                .noProjectOnly(Boolean.TRUE.equals(request.getNoProjectOnly()))
                .nextRunAt(request.getFirstRunAt() != null ? request.getFirstRunAt() : Instant.now().plus(1, ChronoUnit.DAYS))
                .active(request.getActive() == null || request.getActive())
                .createdBy(current)
                .build();
        return toDto(reportScheduleRepository.save(schedule));
    }

    @Transactional
    public void delete(Long id) {
        reportScheduleRepository.deleteById(id);
    }

    @Transactional
    public void runDueSchedules(Instant now) {
        List<ReportSchedule> due = reportScheduleRepository.findByActiveTrueAndNextRunAtBefore(now.plusSeconds(1));
        for (ReportSchedule schedule : due) {
            try {
                String payload = switch (schedule.getReportType()) {
                    case AUDITS_CSV -> reportService.auditsCsv();
                    case RECENT_ACTIVITY_CSV -> reportService.recentActivityCsv(
                            schedule.getCategoryFilter(),
                            schedule.getSearchFilter(),
                            schedule.getProjectIdFilter(),
                            schedule.isNoProjectOnly()
                    );
                };
                sendReportMail(schedule, payload);
                schedule.setLastRunStatus("SUCCESS");
                schedule.setLastRunMessage("Delivered");
            } catch (Exception ex) {
                log.warn("Failed running report schedule {}", schedule.getId(), ex);
                schedule.setLastRunStatus("FAILED");
                schedule.setLastRunMessage(ex.getMessage() != null ? ex.getMessage() : "Failed");
            }
            schedule.setLastRunAt(now);
            schedule.setNextRunAt(nextRun(schedule.getFrequency(), now));
        }
        if (!due.isEmpty()) {
            reportScheduleRepository.saveAll(due);
        }
    }

    private void sendReportMail(ReportSchedule schedule, String payload) {
        String[] recipients = Arrays.stream(schedule.getRecipientEmails().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
        if (recipients.length == 0) return;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(recipients);
        msg.setSubject("Scheduled report: " + schedule.getName());
        msg.setText("Schedule: " + schedule.getName()
                + "\nType: " + schedule.getReportType()
                + "\nFrequency: " + schedule.getFrequency()
                + "\n\nCSV payload:\n" + payload);
        asyncMailService.send(msg);
    }

    private Instant nextRun(ReportScheduleFrequency frequency, Instant now) {
        return switch (frequency) {
            case DAILY -> now.plus(1, ChronoUnit.DAYS);
            case WEEKLY -> now.plus(7, ChronoUnit.DAYS);
            case MONTHLY -> now.plus(30, ChronoUnit.DAYS);
        };
    }

    public static ReportScheduleDto toDto(ReportSchedule schedule) {
        return ReportScheduleDto.builder()
                .id(schedule.getId())
                .name(schedule.getName())
                .reportType(schedule.getReportType())
                .frequency(schedule.getFrequency())
                .recipientEmails(schedule.getRecipientEmails())
                .categoryFilter(schedule.getCategoryFilter())
                .searchFilter(schedule.getSearchFilter())
                .projectIdFilter(schedule.getProjectIdFilter())
                .noProjectOnly(schedule.isNoProjectOnly())
                .nextRunAt(schedule.getNextRunAt())
                .active(schedule.isActive())
                .createdByUserId(schedule.getCreatedBy() != null ? schedule.getCreatedBy().getId() : null)
                .createdByEmail(schedule.getCreatedBy() != null ? schedule.getCreatedBy().getEmail() : null)
                .lastRunAt(schedule.getLastRunAt())
                .lastRunStatus(schedule.getLastRunStatus())
                .lastRunMessage(schedule.getLastRunMessage())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
