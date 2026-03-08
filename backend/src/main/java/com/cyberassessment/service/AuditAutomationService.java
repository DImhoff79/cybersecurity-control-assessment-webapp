package com.cyberassessment.service;

import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditStatus;
import com.cyberassessment.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditAutomationService {

    private final AuditRepository auditRepository;
    private final AuditActivityLogService auditActivityLogService;
    private final JavaMailSender mailSender;

    @Value("${app.automation.enabled:true}")
    private boolean enabled;

    @Value("${app.automation.reminder-days-before-due:3}")
    private long reminderDaysBeforeDue;

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void runDailyAuditAutomation() {
        if (!enabled) {
            return;
        }
        Instant now = Instant.now();
        List<Audit> audits = auditRepository.findByStatusIn(List.of(AuditStatus.DRAFT, AuditStatus.IN_PROGRESS, AuditStatus.SUBMITTED, AuditStatus.ATTESTED));
        for (Audit audit : audits) {
            if (audit.getDueAt() == null) {
                continue;
            }
            long daysUntilDue = Duration.between(now, audit.getDueAt()).toDays();
            boolean shouldRemind = daysUntilDue <= reminderDaysBeforeDue && daysUntilDue >= 0;
            boolean recentlyReminded = audit.getReminderSentAt() != null && Duration.between(audit.getReminderSentAt(), now).toHours() < 24;

            if (shouldRemind && !recentlyReminded) {
                sendReminderEmail(audit, daysUntilDue);
                audit.setReminderSentAt(now);
                auditActivityLogService.log(audit, com.cyberassessment.entity.AuditActivityType.AUDIT_SENT, "Automated reminder sent");
            }

            boolean overdue = audit.getDueAt().isBefore(now);
            if (overdue && audit.getEscalatedAt() == null) {
                audit.setEscalatedAt(now);
                auditActivityLogService.log(audit, com.cyberassessment.entity.AuditActivityType.AUDIT_SENT, "Automated escalation triggered for overdue audit");
            }
        }
        auditRepository.saveAll(audits);
    }

    private void sendReminderEmail(Audit audit, long daysUntilDue) {
        if (audit.getAssignedTo() == null) {
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(audit.getAssignedTo().getEmail());
            msg.setSubject("Reminder: Assessment due for " + audit.getApplication().getName());
            String dueText = daysUntilDue == 0 ? "today" : ("in " + daysUntilDue + " day(s)");
            msg.setText("Your assessment for " + audit.getApplication().getName() + " (" + audit.getYear() + ") is due " + dueText + ".");
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send automated reminder for audit {}", audit.getId(), e);
        }
    }
}
