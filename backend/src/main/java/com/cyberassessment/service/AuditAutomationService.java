package com.cyberassessment.service;

import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditActivityType;
import com.cyberassessment.entity.AuditEvidence;
import com.cyberassessment.entity.Finding;
import com.cyberassessment.entity.FindingStatus;
import com.cyberassessment.entity.AuditStatus;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.AuditEvidenceRepository;
import com.cyberassessment.repository.AuditRepository;
import com.cyberassessment.repository.FindingRepository;
import com.cyberassessment.repository.NotificationRepository;
import com.cyberassessment.repository.UserRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditAutomationService {

    private final AuditRepository auditRepository;
    private final AuditEvidenceRepository auditEvidenceRepository;
    private final FindingRepository findingRepository;
    private final UserRepository userRepository;
    private final AuditActivityLogService auditActivityLogService;
    private final AsyncMailService asyncMailService;
    private final JdbcTemplate jdbcTemplate;
    private final AuditEvidenceService auditEvidenceService;
    private final ReportScheduleService reportScheduleService;
    private final NotificationRepository notificationRepository;

    @Value("${app.automation.enabled:true}")
    private boolean enabled;

    @Value("${app.automation.reminder-days-before-due:3}")
    private long reminderDaysBeforeDue;

    @Value("${app.evidence-policy.reminder-days-before-expiry:14}")
    private long reminderDaysBeforeEvidenceExpiry;

    @Value("${app.remediation.reminder-min-hours-between:24}")
    private long remediationReminderMinHoursBetween;

    @Value("${app.remediation.escalation-days-overdue:7}")
    private long remediationEscalationDaysOverdue;

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
        runDailyRemediationEscalation(now);
        runDailyEvidenceRetentionAutomation(now);
        auditEvidenceService.applyLifecycleAutomation(now);
        reportScheduleService.runDueSchedules(now);
        runDailyNotificationDigest(now);
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
        List<AuditEvidence> candidates = auditEvidenceRepository.findByExpiresAtBefore(expiresBefore);
        for (AuditEvidence evidence : candidates) {
            if (evidence.getExpiresAt() == null) continue;
            long daysRemaining = Duration.between(now, evidence.getExpiresAt()).toDays();
            Audit audit = evidence.getAuditControl().getAudit();
            String evidenceName = evidence.getFileName() != null ? evidence.getFileName() : evidence.getTitle();
            if (daysRemaining < 0) {
                String staleDetails = "Evidence \"" + evidenceName + "\" is stale (expired "
                        + Math.abs(daysRemaining) + " day(s) ago).";
                auditActivityLogService.log(audit, AuditActivityType.EVIDENCE_STALE, staleDetails);
                sendEvidenceExpiryEmail(audit, staleDetails, true);
            } else {
                String expiringDetails = "Evidence \"" + evidenceName + "\" expires in " + daysRemaining + " day(s).";
                auditActivityLogService.log(audit, AuditActivityType.EVIDENCE_EXPIRING, expiringDetails);
                sendEvidenceExpiryEmail(audit, expiringDetails, false);
            }
        }
    }

    private void runDailyRemediationEscalation(Instant now) {
        List<Finding> overdue = findingRepository.findByStatusInAndDueAtBefore(
                List.of(FindingStatus.OPEN, FindingStatus.IN_PROGRESS),
                now
        );
        if (overdue.isEmpty()) {
            return;
        }
        for (Finding finding : overdue) {
            boolean reminderDue = finding.getReminderSentAt() == null
                    || Duration.between(finding.getReminderSentAt(), now).toHours() >= remediationReminderMinHoursBetween;
            if (reminderDue) {
                sendFindingReminder(finding, now);
                finding.setReminderSentAt(now);
                auditActivityLogService.log(
                        finding.getAudit(),
                        AuditActivityType.FINDING_REMINDER_SENT,
                        "Overdue finding reminder sent: " + finding.getTitle()
                );
            }

            long daysOverdue = Duration.between(finding.getDueAt(), now).toDays();
            boolean escalationDue = daysOverdue >= remediationEscalationDaysOverdue
                    && (finding.getEscalatedAt() == null
                    || Duration.between(finding.getEscalatedAt(), now).toHours() >= remediationReminderMinHoursBetween);
            if (escalationDue) {
                sendFindingEscalation(finding, daysOverdue);
                finding.setEscalatedAt(now);
                auditActivityLogService.log(
                        finding.getAudit(),
                        AuditActivityType.FINDING_ESCALATED,
                        "Overdue finding escalated: " + finding.getTitle()
                );
            }
        }
        findingRepository.saveAll(overdue);
    }

    private void sendFindingReminder(Finding finding, Instant now) {
        if (finding.getOwner() == null || finding.getOwner().getEmail() == null || finding.getOwner().getEmail().isBlank()) {
            return;
        }
        long daysOverdue = Duration.between(finding.getDueAt(), now).toDays();
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(finding.getOwner().getEmail());
            msg.setSubject("Overdue remediation finding: " + finding.getTitle());
            msg.setText("The finding \"" + finding.getTitle() + "\" for application "
                    + finding.getAudit().getApplication().getName()
                    + " is overdue by " + Math.max(1, daysOverdue) + " day(s)."
                    + "\n\nPlease update remediation status and target closure date.");
            asyncMailService.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send finding reminder for finding {}", finding.getId(), e);
        }
    }

    private void sendFindingEscalation(Finding finding, long daysOverdue) {
        Set<String> recipients = new LinkedHashSet<>();
        if (finding.getOwner() != null && finding.getOwner().getEmail() != null && !finding.getOwner().getEmail().isBlank()) {
            recipients.add(finding.getOwner().getEmail());
        }
        userRepository.findByRole(UserRole.ADMIN).stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .forEach(recipients::add);
        userRepository.findByRole(UserRole.AUDIT_MANAGER).stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .forEach(recipients::add);

        if (recipients.isEmpty()) {
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(recipients.toArray(new String[0]));
            msg.setSubject("Escalation: overdue remediation finding");
            msg.setText("Finding \"" + finding.getTitle() + "\" for application "
                    + finding.getAudit().getApplication().getName()
                    + " is overdue by " + Math.max(1, daysOverdue) + " day(s)."
                    + "\nStatus: " + finding.getStatus()
                    + "\nOwner: " + (finding.getOwner() != null ? finding.getOwner().getEmail() : "unassigned")
                    + "\n\nPlease review and drive remediation closure.");
            asyncMailService.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send finding escalation for finding {}", finding.getId(), e);
        }
    }

    private void sendEvidenceExpiryEmail(Audit audit, String details, boolean stale) {
        if (audit.getAssignedTo() == null) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(audit.getAssignedTo().getEmail());
            msg.setSubject((stale ? "Evidence is stale: " : "Evidence retention reminder: ") + audit.getApplication().getName());
            msg.setText(details + "\n\nPlease review and refresh evidence in the assessment workspace.");
            asyncMailService.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send evidence expiry reminder for audit {}", audit.getId(), e);
        }
    }

    private void runDailyNotificationDigest(Instant now) {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            List<com.cyberassessment.entity.Notification> unread = notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(user.getId())
                    .stream()
                    .filter(n -> n.getDigestSentAt() == null)
                    .limit(20)
                    .toList();
            if (unread.isEmpty()) {
                continue;
            }
            try {
                StringBuilder body = new StringBuilder("You have " + unread.size() + " unread assessment notifications:\n\n");
                unread.forEach(n -> body.append("- [").append(n.getCategory()).append("] ")
                        .append(n.getTitle()).append(": ").append(n.getMessage()).append("\n"));
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(user.getEmail());
                msg.setSubject("Daily assessment digest");
                msg.setText(body.toString());
                asyncMailService.send(msg);
                unread.forEach(n -> n.setDigestSentAt(now));
                notificationRepository.saveAll(unread);
            } catch (Exception e) {
                log.warn("Failed sending digest for user {}", user.getId(), e);
            }
        }
    }
}
