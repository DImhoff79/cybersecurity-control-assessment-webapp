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
    private final AuditEvidenceRepository auditEvidenceRepository;
    private final AuditActivityLogService auditActivityLogService;
    private final JavaMailSender mailSender;

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
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send evidence expiry reminder for audit {}", audit.getId(), e);
        }
    }
}
