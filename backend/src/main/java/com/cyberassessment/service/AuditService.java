package com.cyberassessment.service;

import com.cyberassessment.dto.*;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditRepository auditRepository;
    private final ApplicationRepository applicationRepository;
    private final ControlRepository controlRepository;
    private final AuditControlRepository auditControlRepository;
    private final AuditControlAnswerRepository auditControlAnswerRepository;
    private final QuestionControlMappingRepository questionControlMappingRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    public AuditService(AuditRepository auditRepository, ApplicationRepository applicationRepository,
                        ControlRepository controlRepository, AuditControlRepository auditControlRepository,
                        AuditControlAnswerRepository auditControlAnswerRepository,
                        QuestionControlMappingRepository questionControlMappingRepository,
                        CurrentUserService currentUserService, UserRepository userRepository,
                        @Autowired(required = false) JavaMailSender mailSender) {
        this.auditRepository = auditRepository;
        this.applicationRepository = applicationRepository;
        this.controlRepository = controlRepository;
        this.auditControlRepository = auditControlRepository;
        this.auditControlAnswerRepository = auditControlAnswerRepository;
        this.questionControlMappingRepository = questionControlMappingRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public static AuditDto toDto(Audit a) {
        if (a == null) return null;
        User assigned = a.getAssignedTo();
        return AuditDto.builder()
                .id(a.getId())
                .applicationId(a.getApplication().getId())
                .applicationName(a.getApplication().getName())
                .year(a.getYear())
                .status(a.getStatus())
                .startedAt(a.getStartedAt())
                .completedAt(a.getCompletedAt())
                .assignedToUserId(assigned != null ? assigned.getId() : null)
                .assignedToEmail(assigned != null ? assigned.getEmail() : null)
                .assignedToDisplayName(assigned != null ? assigned.getDisplayName() : null)
                .assignedAt(a.getAssignedAt())
                .sentAt(a.getSentAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AuditDto> findByApplicationId(Long applicationId) {
        Application app = applicationRepository.findById(applicationId).orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        return auditRepository.findByApplicationOrderByYearDesc(app).stream().map(AuditService::toDto).collect(Collectors.toList());
    }

    @Transactional
    public AuditDto create(Long applicationId, Integer year) {
        Application app = applicationRepository.findById(applicationId).orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        if (auditRepository.findByApplicationIdAndYear(applicationId, year).isPresent()) {
            throw new IllegalArgumentException("Audit already exists for this application and year");
        }
        Audit audit = Audit.builder()
                .application(app)
                .year(year)
                .status(AuditStatus.DRAFT)
                .build();
        audit = auditRepository.save(audit);
        List<Control> enabledControls = controlRepository.findByEnabled(true);
        for (Control c : enabledControls) {
            AuditControl ac = AuditControl.builder()
                    .audit(audit)
                    .control(c)
                    .status(ControlAssessmentStatus.NOT_STARTED)
                    .build();
            auditControlRepository.save(ac);
        }
        return toDto(audit);
    }

    @Transactional(readOnly = true)
    public AuditDto findById(Long auditId) {
        return auditRepository.findById(auditId).map(AuditService::toDto).orElse(null);
    }

    @Transactional
    public AuditDto update(Long auditId, AuditStatus status) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        if (status != null) audit.setStatus(status);
        if (status == AuditStatus.IN_PROGRESS && audit.getStartedAt() == null) audit.setStartedAt(Instant.now());
        if (status == AuditStatus.COMPLETE) audit.setCompletedAt(Instant.now());
        audit = auditRepository.save(audit);
        return toDto(audit);
    }

    @Transactional
    public void delete(Long auditId) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can delete audits");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        auditRepository.delete(audit);
    }

    @Transactional
    public AuditDto assign(Long auditId, Long userId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        User user = userId != null ? userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)) : null;
        audit.setAssignedTo(user);
        audit.setAssignedAt(user != null ? Instant.now() : null);
        audit = auditRepository.save(audit);
        return toDto(audit);
    }

    @Transactional
    public AuditDto sendToOwner(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        audit.setSentAt(Instant.now());
        if (audit.getStatus() == AuditStatus.DRAFT) {
            audit.setStatus(AuditStatus.IN_PROGRESS);
            audit.setStartedAt(Instant.now());
        }
        audit = auditRepository.save(audit);
        User assigned = audit.getAssignedTo();
        if (assigned != null && mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(assigned.getEmail());
                msg.setSubject("Cybersecurity Assessment: " + audit.getApplication().getName() + " (" + audit.getYear() + ")");
                msg.setText("You have been assigned to complete the cybersecurity control assessment for " + audit.getApplication().getName() + " for year " + audit.getYear() + ".\n\nPlease log in and complete the assessment at: " + frontendBaseUrl + "/my-audits");
                mailSender.send(msg);
            } catch (Exception ignored) {}
        }
        return toDto(audit);
    }

    @Transactional(readOnly = true)
    public List<AuditDto> findMyAudits() {
        User current = currentUserService.getCurrentUserOrThrow();
        if (currentUserService.isAdmin()) {
            return auditRepository.findAll().stream().map(AuditService::toDto).collect(Collectors.toList());
        }
        return auditRepository.findByAssignedTo(current).stream().map(AuditService::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditQuestionItemDto> getQuestionsForAudit(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        ensureCanAccessAudit(audit);
        List<AuditControl> auditControls = auditControlRepository.findByAudit(audit);
        List<AuditQuestionItemDto> result = new ArrayList<>();
        for (AuditControl ac : auditControls) {
            Control c = ac.getControl();
            List<Question> questions = questionControlMappingRepository.findByControl_IdOrderByQuestionDisplayOrderAsc(c.getId()).stream()
                    .map(QuestionControlMapping::getQuestion)
                    .filter(q -> Boolean.TRUE.equals(q.getAskOwner()))
                    .collect(Collectors.toList());
            questions.sort(Comparator.comparingInt(Question::getDisplayOrder));
            for (Question q : questions) {
                Optional<AuditControlAnswer> existing = ac.getAnswers().stream().filter(a -> a.getQuestion().getId().equals(q.getId())).findFirst();
                result.add(AuditQuestionItemDto.builder()
                        .questionId(q.getId())
                        .auditControlId(ac.getId())
                        .controlId(c.getId())
                        .controlControlId(c.getControlId())
                        .controlName(c.getName())
                        .questionText(q.getQuestionText())
                        .helpText(q.getHelpText())
                        .displayOrder(q.getDisplayOrder())
                        .existingAnswerText(existing.map(AuditControlAnswer::getAnswerText).orElse(null))
                        .build());
            }
        }
        result.sort(Comparator.comparing(AuditQuestionItemDto::getDisplayOrder).thenComparing(AuditQuestionItemDto::getControlControlId));
        return result;
    }

    @Transactional(readOnly = true)
    public List<AuditControlAnswerDto> getAnswersForAudit(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        ensureCanAccessAudit(audit);
        List<AuditControl> auditControls = auditControlRepository.findByAudit(audit);
        List<AuditControlAnswerDto> result = new ArrayList<>();
        for (AuditControl ac : auditControls) {
            for (AuditControlAnswer a : ac.getAnswers()) {
                result.add(AuditControlAnswerDto.builder()
                        .id(a.getId())
                        .auditControlId(ac.getId())
                        .questionId(a.getQuestion().getId())
                        .questionText(a.getQuestion().getQuestionText())
                        .answerText(a.getAnswerText())
                        .answeredAt(a.getAnsweredAt())
                        .build());
            }
        }
        return result;
    }

    @Transactional
    public void submitAnswers(Long auditId, SubmitAnswersRequest request) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        ensureCanAccessAudit(audit);
        for (SubmitAnswersRequest.AnswerItem item : request.getAnswers()) {
            if (item.getAuditControlId() == null || item.getQuestionId() == null) continue;
            AuditControl ac = auditControlRepository.findById(item.getAuditControlId()).orElse(null);
            if (ac == null || !ac.getAudit().getId().equals(auditId)) continue;
            Optional<AuditControlAnswer> existing = auditControlAnswerRepository.findByAuditControlIdAndQuestionId(item.getAuditControlId(), item.getQuestionId());
            if (existing.isPresent()) {
                AuditControlAnswer a = existing.get();
                a.setAnswerText(item.getAnswerText());
                a.setAnsweredAt(Instant.now());
                auditControlAnswerRepository.save(a);
            } else {
                Question q = questionControlMappingRepository.findByControl_IdOrderByQuestionDisplayOrderAsc(ac.getControl().getId()).stream()
                        .map(QuestionControlMapping::getQuestion)
                        .filter(qu -> qu.getId().equals(item.getQuestionId()))
                        .findFirst()
                        .orElse(null);
                if (q == null) continue;
                AuditControlAnswer a = AuditControlAnswer.builder()
                        .auditControl(ac)
                        .question(q)
                        .answerText(item.getAnswerText())
                        .answeredAt(Instant.now())
                        .build();
                auditControlAnswerRepository.save(a);
            }
        }
        audit.setStatus(AuditStatus.IN_PROGRESS);
        auditRepository.save(audit);
    }

    private void ensureCanAccessAudit(Audit audit) {
        User current = currentUserService.getCurrentUserOrThrow();
        if (currentUserService.isAdmin()) return;
        if (audit.getAssignedTo() == null || !audit.getAssignedTo().getId().equals(current.getId())) {
            throw new IllegalArgumentException("You do not have access to this audit");
        }
    }
}
