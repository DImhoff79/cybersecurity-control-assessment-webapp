package com.cyberassessment.service;

import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditActivityType;
import com.cyberassessment.entity.AuditEvidence;
import com.cyberassessment.entity.AuditStatus;
import com.cyberassessment.entity.EvidenceReviewStatus;
import com.cyberassessment.repository.AuditEvidenceRepository;
import com.cyberassessment.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditAutomationService {

    private final AuditRepository auditRepository;
    private final AuditEvidenceRepository auditEvidenceRepository;
    private final AuditActivityLogService auditActivityLogService;
    private final AsyncMailService asyncMailService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.automation.enabled:true}")
    private boolean enabled;

    @Value("${app.automation.reminder-days-before-due:3}")
    private long reminderDaysBeforeDue;

    @Value("${app.evidence-policy.reminder-days-before-expiry:14}")
    private long reminderDaysBeforeEvidenceExpiry;

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void runDailyAuditAutomation() {
        if (!enabled) {
            return;
        }
        if (!tryAcquireExecutionLock()) {
            log.info("Skipping automation run; another node holds execution lock");
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
                auditActivityLogService.log(audit, AuditActivityType.AUDIT_SENT, "Automated reminder sent");
            }

            boolean overdue = audit.getDueAt().isBefore(now);
            if (overdue && audit.getEscalatedAt() == null) {
                audit.setEscalatedAt(now);
                auditActivityLogService.log(audit, AuditActivityType.AUDIT_SENT, "Automated escalation triggered for overdue audit");
            }
        }
        auditRepository.saveAll(audits);
        runDailyEvidenceRetentionAutomation(now);
    }

    private boolean tryAcquireExecutionLock() {
        jdbcTemplate.update(
                "CREATE TABLE IF NOT EXISTS automation_execution_locks (lock_name VARCHAR(64) PRIMARY KEY, lock_until TIMESTAMP NOT NULL, locked_at TIMESTAMP NOT NULL)"
        );
        Instant now = Instant.now();
        Instant lockUntil = now.plus(Duration.ofMinutes(10));
        int updated = jdbcTemplate.update(
                "UPDATE automation_execution_locks SET lock_until = ?, locked_at = ? WHERE lock_name = ? AND lock_until <= ?",
                Timestamp.from(lockUntil),
                Timestamp.from(now),
                "daily-audit-automation",
                Timestamp.from(now)
        );
        if (updated == 1) {
            return true;
        }
        try {
            int inserted = jdbcTemplate.update(
                    "INSERT INTO automation_execution_locks (lock_name, lock_until, locked_at) VALUES (?, ?, ?)",
                    "daily-audit-automation",
                    Timestamp.from(lockUntil),
                    Timestamp.from(now)
            );
            return inserted == 1;
        } catch (Exception ignored) {
            return false;
        }
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
            asyncMailService.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send automated reminder for audit {}", audit.getId(), e);
        }
    }

    private void runDailyEvidenceRetentionAutomation(Instant now) {
        Instant expiresBefore = now.plus(Duration.ofDays(reminderDaysBeforeEvidenceExpiry));
        List<AuditEvidence> candidates = auditEvidenceRepository.findByExpiresAtBeforeAndReviewStatusIn(
                expiresBefore,
                List.of(EvidenceReviewStatus.PENDING, EvidenceReviewStatus.REJECTED)
        );
        for (AuditEvidence evidence : candidates) {
            if (evidence.getExpiresAt() == null) continue;
            long daysRemaining = Duration.between(now, evidence.getExpiresAt()).toDays();
            if (daysRemaining < 0) continue;
            Audit audit = evidence.getAuditControl().getAudit();
            String details = "Evidence \"" + (evidence.getFileName() != null ? evidence.getFileName() : evidence.getTitle())
                    + "\" expires in " + daysRemaining + " day(s).";
            auditActivityLogService.log(audit, AuditActivityType.EVIDENCE_EXPIRING, details);
            sendEvidenceExpiryEmail(audit, details);
        }
    }

    private void sendEvidenceExpiryEmail(Audit audit, String details) {
        if (audit.getAssignedTo() == null) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(audit.getAssignedTo().getEmail());
            msg.setSubject("Evidence retention reminder: " + audit.getApplication().getName());
            msg.setText(details + "\n\nPlease review and refresh evidence in the assessment workspace.");
            asyncMailService.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send evidence expiry reminder for audit {}", audit.getId(), e);
        }
    }
}
