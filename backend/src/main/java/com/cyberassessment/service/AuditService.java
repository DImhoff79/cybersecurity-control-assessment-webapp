package com.cyberassessment.service;

import com.cyberassessment.dto.*;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.*;
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
    private final AuditControlAssignmentRepository auditControlAssignmentRepository;
    private final AuditControlAnswerRepository auditControlAnswerRepository;
    private final QuestionControlMappingRepository questionControlMappingRepository;
    private final QuestionRepository questionRepository;
    private final AuditQuestionnaireSnapshotRepository auditQuestionnaireSnapshotRepository;
    private final AuditQuestionnaireItemRepository auditQuestionnaireItemRepository;
    private final AuditAssignmentRepository auditAssignmentRepository;
    private final QuestionnaireTemplateService questionnaireTemplateService;
    private final QuestionnaireTemplateItemRepository questionnaireTemplateItemRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final AuditActivityLogService auditActivityLogService;
    private final JavaMailSender mailSender;

    public AuditService(AuditRepository auditRepository, ApplicationRepository applicationRepository,
                        ControlRepository controlRepository, AuditControlRepository auditControlRepository,
                        AuditControlAssignmentRepository auditControlAssignmentRepository,
                        AuditControlAnswerRepository auditControlAnswerRepository,
                        QuestionControlMappingRepository questionControlMappingRepository,
                        QuestionRepository questionRepository,
                        AuditQuestionnaireSnapshotRepository auditQuestionnaireSnapshotRepository,
                        AuditQuestionnaireItemRepository auditQuestionnaireItemRepository,
                        AuditAssignmentRepository auditAssignmentRepository,
                        QuestionnaireTemplateService questionnaireTemplateService,
                        QuestionnaireTemplateItemRepository questionnaireTemplateItemRepository,
                        CurrentUserService currentUserService, UserRepository userRepository,
                        AuditActivityLogService auditActivityLogService,
                        @Autowired(required = false) JavaMailSender mailSender) {
        this.auditRepository = auditRepository;
        this.applicationRepository = applicationRepository;
        this.controlRepository = controlRepository;
        this.auditControlRepository = auditControlRepository;
        this.auditControlAssignmentRepository = auditControlAssignmentRepository;
        this.auditControlAnswerRepository = auditControlAnswerRepository;
        this.questionControlMappingRepository = questionControlMappingRepository;
        this.questionRepository = questionRepository;
        this.auditQuestionnaireSnapshotRepository = auditQuestionnaireSnapshotRepository;
        this.auditQuestionnaireItemRepository = auditQuestionnaireItemRepository;
        this.auditAssignmentRepository = auditAssignmentRepository;
        this.questionnaireTemplateService = questionnaireTemplateService;
        this.questionnaireTemplateItemRepository = questionnaireTemplateItemRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.auditActivityLogService = auditActivityLogService;
        this.mailSender = mailSender;
    }

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public static AuditDto toDto(Audit a) {
        if (a == null) return null;
        User assigned = a.getAssignedTo();
        User attestedBy = a.getAttestedBy();
        List<AuditAssignmentDto> assignments = a.getAssignments() != null
                ? a.getAssignments().stream().filter(aa -> Boolean.TRUE.equals(aa.getActive()))
                .map(aa -> AuditAssignmentDto.builder()
                        .id(aa.getId())
                        .auditId(a.getId())
                        .userId(aa.getUser().getId())
                        .userEmail(aa.getUser().getEmail())
                        .userDisplayName(aa.getUser().getDisplayName())
                        .assignmentRole(aa.getAssignmentRole())
                        .active(aa.getActive())
                        .assignedAt(aa.getAssignedAt())
                        .build())
                .toList()
                : List.of();
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
                .dueAt(a.getDueAt())
                .reminderSentAt(a.getReminderSentAt())
                .escalatedAt(a.getEscalatedAt())
                .attestedAt(a.getAttestedAt())
                .attestedByUserId(attestedBy != null ? attestedBy.getId() : null)
                .attestedByEmail(attestedBy != null ? attestedBy.getEmail() : null)
                .attestationStatement(a.getAttestationStatement())
                .assignments(assignments)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AuditDto> findByApplicationId(Long applicationId) {
        Application app = applicationRepository.findById(applicationId).orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        return auditRepository.findByApplicationOrderByYearDesc(app).stream().map(AuditService::toDto).collect(Collectors.toList());
    }

    @Transactional
    public AuditDto create(Long applicationId, Integer year) {
        return create(applicationId, year, null);
    }

    @Transactional
    public AuditDto create(Long applicationId, Integer year, Instant dueAt) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can create audits");
        }
        Application app = applicationRepository.findById(applicationId).orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        if (auditRepository.findByApplicationIdAndYear(applicationId, year).isPresent()) {
            throw new IllegalArgumentException("Audit already exists for this application and year");
        }
        Audit audit = Audit.builder()
                .application(app)
                .year(year)
                .status(AuditStatus.DRAFT)
                .dueAt(dueAt)
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
        createQuestionnaireSnapshot(audit);
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_CREATED, "Created audit for " + app.getName() + " (" + year + ")");
        return toDto(audit);
    }

    @Transactional(readOnly = true)
    public AuditDto findById(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElse(null);
        if (audit == null) {
            return null;
        }
        ensureCanAccessAudit(audit);
        return toDto(audit);
    }

    @Transactional
    public AuditDto update(Long auditId, AuditStatus status) {
        return update(auditId, status, null);
    }

    @Transactional
    public AuditDto update(Long auditId, AuditStatus status, Instant dueAt) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can update audits");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        if (status != null) audit.setStatus(status);
        if (dueAt != null) audit.setDueAt(dueAt);
        if (status == AuditStatus.IN_PROGRESS && audit.getStartedAt() == null) audit.setStartedAt(Instant.now());
        if (status == AuditStatus.COMPLETE) audit.setCompletedAt(Instant.now());
        audit = auditRepository.save(audit);
        if (status == AuditStatus.COMPLETE) {
            auditActivityLogService.log(audit, AuditActivityType.AUDIT_COMPLETED, "Audit marked complete");
        }
        return toDto(audit);
    }

    @Transactional
    public void delete(Long auditId) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can delete audits");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_DELETED, "Audit deleted");
        auditRepository.delete(audit);
    }

    @Transactional
    public AuditDto assign(Long auditId, Long userId) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can assign audits");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        User user = userId != null ? userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)) : null;
        audit.setAssignedTo(user);
        audit.setAssignedAt(user != null ? Instant.now() : null);
        if (user != null) {
            syncPrimaryAssignment(audit, user);
        } else {
            deactivatePrimaryAssignments(audit.getId());
        }
        audit = auditRepository.save(audit);
        if (user != null) {
            auditActivityLogService.log(audit, AuditActivityType.AUDIT_ASSIGNED, "Assigned to " + user.getEmail());
        }
        return toDto(audit);
    }

    @Transactional
    public AuditDto sendToOwner(Long auditId) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can send audits");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        audit.setSentAt(Instant.now());
        if (audit.getStatus() == AuditStatus.DRAFT) {
            audit.setStatus(AuditStatus.IN_PROGRESS);
            audit.setStartedAt(Instant.now());
        }
        audit = auditRepository.save(audit);
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_SENT, "Audit sent to owner");
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
        List<Audit> direct = auditRepository.findByAssignedTo(current);
        List<Audit> collaboratorAudits = auditAssignmentRepository.findByUserIdAndActiveTrue(current.getId())
                .stream()
                .map(AuditAssignment::getAudit)
                .toList();
        return java.util.stream.Stream.concat(direct.stream(), collaboratorAudits.stream())
                .collect(Collectors.toMap(Audit::getId, a -> a, (a, b) -> a))
                .values().stream()
                .map(AuditService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditQuestionItemDto> getQuestionsForAudit(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        ensureCanAccessAudit(audit);
        Set<Long> allowedControlIds = allowedControlIdsForCurrentUser(audit);
        AuditQuestionnaireSnapshot snapshot = ensureSnapshot(audit);
        List<AuditQuestionnaireItem> items = auditQuestionnaireItemRepository
                .findBySnapshotIdOrderByDisplayOrderAscControlControlIdAsc(snapshot.getId());
        List<AuditQuestionItemDto> result = new ArrayList<>();
        for (AuditQuestionnaireItem item : items) {
            if (!Boolean.TRUE.equals(item.getAskOwner())) {
                continue;
            }
            if (!allowedControlIds.contains(item.getControl().getId())) {
                continue;
            }
            AuditControl ac = item.getAuditControl();
            Optional<AuditControlAnswer> existing = ac.getAnswers().stream().filter(a -> a.getQuestion().getId().equals(item.getQuestion().getId())).findFirst();
            result.add(AuditQuestionItemDto.builder()
                    .questionId(item.getQuestion().getId())
                    .auditControlId(ac.getId())
                    .controlId(item.getControl().getId())
                    .controlControlId(item.getControl().getControlId())
                    .controlName(item.getControl().getName())
                    .questionText(item.getQuestionText())
                    .helpText(item.getHelpText())
                    .displayOrder(item.getDisplayOrder())
                    .existingAnswerText(existing.map(AuditControlAnswer::getAnswerText).orElse(null))
                    .build());
        }
        result.sort(Comparator.comparing(AuditQuestionItemDto::getDisplayOrder).thenComparing(AuditQuestionItemDto::getControlControlId));
        return result;
    }

    @Transactional(readOnly = true)
    public List<AuditControlAnswerDto> getAnswersForAudit(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        ensureCanAccessAudit(audit);
        Set<Long> allowedControlIds = allowedControlIdsForCurrentUser(audit);
        List<AuditControl> auditControls = auditControlRepository.findByAudit(audit);
        List<AuditControlAnswerDto> result = new ArrayList<>();
        for (AuditControl ac : auditControls) {
            if (!allowedControlIds.contains(ac.getControl().getId())) {
                continue;
            }
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
        Set<Long> allowedControlIds = allowedControlIdsForCurrentUser(audit);
        for (SubmitAnswersRequest.AnswerItem item : request.getAnswers()) {
            if (item.getAuditControlId() == null || item.getQuestionId() == null) continue;
            AuditControl ac = auditControlRepository.findById(item.getAuditControlId()).orElse(null);
            if (ac == null || !ac.getAudit().getId().equals(auditId)) continue;
            if (!allowedControlIds.contains(ac.getControl().getId())) continue;
            boolean allowed = auditQuestionnaireItemRepository
                    .findByAuditControlIdAndAskOwnerTrue(ac.getId())
                    .stream()
                    .anyMatch(i -> i.getQuestion().getId().equals(item.getQuestionId()));
            if (!allowed) {
                continue;
            }
            Optional<AuditControlAnswer> existing = auditControlAnswerRepository.findByAuditControlIdAndQuestionId(item.getAuditControlId(), item.getQuestionId());
            if (existing.isPresent()) {
                AuditControlAnswer a = existing.get();
                a.setAnswerText(item.getAnswerText());
                a.setAnsweredAt(Instant.now());
                auditControlAnswerRepository.save(a);
            } else {
                Question q = questionRepository.findById(item.getQuestionId()).orElse(null);
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
        if (audit.getStatus() == AuditStatus.DRAFT) {
            audit.setStatus(AuditStatus.IN_PROGRESS);
        }
        auditRepository.save(audit);
        auditActivityLogService.log(audit, AuditActivityType.ANSWER_SUBMITTED, "Submitted answers");
    }

    @Transactional
    public AuditDto submitAudit(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        ensureCanAccessAudit(audit);
        if (!currentUserService.isAdmin()) {
            User current = currentUserService.getCurrentUserOrThrow();
            if (audit.getAssignedTo() == null || !audit.getAssignedTo().getId().equals(current.getId())) {
                throw new IllegalArgumentException("Only the primary assignee can submit the full audit");
            }
        }
        if (!isAuditCompleteForOwner(audit)) {
            throw new IllegalArgumentException("Please answer all questions before submitting.");
        }
        audit.setStatus(AuditStatus.SUBMITTED);
        if (audit.getCompletedAt() == null) {
            audit.setCompletedAt(Instant.now());
        }
        audit = auditRepository.save(audit);
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_SUBMITTED, "Audit submitted by owner");
        return toDto(audit);
    }

    @Transactional
    public AuditDto attest(Long auditId, String statement) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can attest audits");
        }
        if (audit.getStatus() != AuditStatus.SUBMITTED && audit.getStatus() != AuditStatus.ATTESTED) {
            throw new IllegalArgumentException("Only submitted audits can be attested");
        }
        User actor = currentUserService.getCurrentUserOrThrow();
        audit.setStatus(AuditStatus.ATTESTED);
        audit.setAttestedAt(Instant.now());
        audit.setAttestedBy(actor);
        if (statement != null && !statement.isBlank()) {
            audit.setAttestationStatement(statement);
        }
        audit = auditRepository.save(audit);
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_ATTESTED, "Audit attested by " + actor.getEmail());
        return toDto(audit);
    }

    @Transactional
    public AuditDto sendReminder(Long auditId) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can send reminders");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        audit.setReminderSentAt(Instant.now());
        if (audit.getDueAt() != null && audit.getDueAt().isBefore(Instant.now())) {
            audit.setEscalatedAt(Instant.now());
        }
        audit = auditRepository.save(audit);
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_SENT, "Reminder sent");
        return toDto(audit);
    }

    @Transactional
    public List<AuditDto> bulkAssign(List<Long> auditIds, Long userId, boolean sendNow) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can bulk assign audits");
        }
        User user = userId != null ? userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)) : null;
        List<AuditDto> updated = new ArrayList<>();
        for (Long auditId : auditIds) {
            Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
            audit.setAssignedTo(user);
            audit.setAssignedAt(user != null ? Instant.now() : null);
            if (user != null) {
                syncPrimaryAssignment(audit, user);
            } else {
                deactivatePrimaryAssignments(audit.getId());
            }
            if (sendNow) {
                audit.setSentAt(Instant.now());
                if (audit.getStatus() == AuditStatus.DRAFT) {
                    audit.setStatus(AuditStatus.IN_PROGRESS);
                    audit.setStartedAt(Instant.now());
                }
            }
            audit = auditRepository.save(audit);
            auditActivityLogService.log(audit, AuditActivityType.AUDIT_ASSIGNED, "Bulk assigned to " + (user != null ? user.getEmail() : "none"));
            updated.add(toDto(audit));
        }
        return updated;
    }

    private boolean isAuditCompleteForOwner(Audit audit) {
        List<AuditControl> auditControls = auditControlRepository.findByAudit(audit);
        for (AuditControl ac : auditControls) {
            List<Question> ownerQuestions = auditQuestionnaireItemRepository
                    .findByAuditControlIdAndAskOwnerTrue(ac.getId())
                    .stream()
                    .map(AuditQuestionnaireItem::getQuestion)
                    .toList();

            if (!ownerQuestions.isEmpty()) {
                for (Question q : ownerQuestions) {
                    Optional<AuditControlAnswer> answer = auditControlAnswerRepository
                            .findByAuditControlIdAndQuestionId(ac.getId(), q.getId());
                    if (answer.isEmpty() || answer.get().getAnswerText() == null || answer.get().getAnswerText().isBlank()) {
                        return false;
                    }
                }
            } else if (!(ac.getStatus() == ControlAssessmentStatus.PASS || ac.getStatus() == ControlAssessmentStatus.FAIL || ac.getStatus() == ControlAssessmentStatus.NA)) {
                return false;
            }
        }
        return true;
    }

    private void ensureCanAccessAudit(Audit audit) {
        User current = currentUserService.getCurrentUserOrThrow();
        if (currentUserService.isAdmin()) return;
        boolean direct = audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(current.getId());
        boolean collaborator = auditAssignmentRepository.existsByAuditIdAndUserIdAndActiveTrue(audit.getId(), current.getId());
        if (!direct && !collaborator) {
            throw new IllegalArgumentException("You do not have access to this audit");
        }
    }

    private Set<Long> allowedControlIdsForCurrentUser(Audit audit) {
        if (currentUserService.isAdmin()) {
            return auditControlRepository.findByAudit(audit).stream().map(ac -> ac.getControl().getId()).collect(Collectors.toSet());
        }
        User current = currentUserService.getCurrentUserOrThrow();
        boolean isPrimary = audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(current.getId());
        if (isPrimary) {
            return auditControlRepository.findByAudit(audit).stream().map(ac -> ac.getControl().getId()).collect(Collectors.toSet());
        }
        Set<Long> controlIds = new HashSet<>();
        for (AuditControl ac : auditControlRepository.findByAudit(audit)) {
            if (auditControlAssignmentRepository.existsByAuditControlIdAndUserIdAndActiveTrue(ac.getId(), current.getId())) {
                controlIds.add(ac.getControl().getId());
            }
        }
        return controlIds;
    }

    @Transactional(readOnly = true)
    public List<AuditAssignmentDto> listAssignments(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        ensureCanAccessAudit(audit);
        return auditAssignmentRepository.findByAuditIdAndActiveTrue(auditId).stream()
                .map(this::toAssignmentDto)
                .toList();
    }

    @Transactional
    public List<AuditAssignmentDto> addAssignment(Long auditId, Long userId, AuditAssignmentRole role) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can manage assignments");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (role == AuditAssignmentRole.PRIMARY) {
            deactivatePrimaryAssignments(auditId);
            audit.setAssignedTo(user);
            audit.setAssignedAt(Instant.now());
            auditRepository.save(audit);
        }
        AuditAssignment assignment = auditAssignmentRepository.findByAuditIdAndUserIdAndAssignmentRole(auditId, userId, role)
                .orElse(AuditAssignment.builder()
                        .audit(audit)
                        .user(user)
                        .assignmentRole(role)
                        .build());
        assignment.setActive(true);
        assignment = auditAssignmentRepository.save(assignment);
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_ASSIGNED, "Added " + role + " assignee: " + user.getEmail());
        return auditAssignmentRepository.findByAuditIdAndActiveTrue(auditId).stream().map(this::toAssignmentDto).toList();
    }

    @Transactional
    public List<AuditAssignmentDto> removeAssignment(Long auditId, Long assignmentId) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can manage assignments");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        AuditAssignment assignment = auditAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        if (!assignment.getAudit().getId().equals(auditId)) {
            throw new IllegalArgumentException("Assignment does not belong to audit");
        }
        assignment.setActive(false);
        auditAssignmentRepository.save(assignment);
        if (assignment.getAssignmentRole() == AuditAssignmentRole.PRIMARY && audit.getAssignedTo() != null
                && audit.getAssignedTo().getId().equals(assignment.getUser().getId())) {
            audit.setAssignedTo(null);
            audit.setAssignedAt(null);
            auditRepository.save(audit);
        }
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_ASSIGNED, "Removed " + assignment.getAssignmentRole() + " assignee: " + assignment.getUser().getEmail());
        return auditAssignmentRepository.findByAuditIdAndActiveTrue(auditId).stream().map(this::toAssignmentDto).toList();
    }

    @Transactional(readOnly = true)
    public void assertCanAccessAudit(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        ensureCanAccessAudit(audit);
    }

    private AuditQuestionnaireSnapshot ensureSnapshot(Audit audit) {
        return auditQuestionnaireSnapshotRepository.findByAuditId(audit.getId())
                .orElseGet(() -> createQuestionnaireSnapshot(audit));
    }

    private AuditQuestionnaireSnapshot createQuestionnaireSnapshot(Audit audit) {
        AuditQuestionnaireSnapshot snapshot = AuditQuestionnaireSnapshot.builder()
                .audit(audit)
                .createdBy(currentUserService.getCurrentUser().orElse(null))
                .build();
        snapshot = auditQuestionnaireSnapshotRepository.save(snapshot);
        List<AuditControl> auditControls = auditControlRepository.findByAudit(audit);
        QuestionnaireTemplate publishedTemplate = questionnaireTemplateService.findLatestPublishedEntity();
        if (publishedTemplate != null) {
            List<QuestionnaireTemplateItem> templateItems = questionnaireTemplateItemRepository
                    .findByTemplateIdOrderByDisplayOrderAscControlControlIdAsc(publishedTemplate.getId());
            for (AuditControl ac : auditControls) {
                List<QuestionnaireTemplateItem> controlItems = templateItems.stream()
                        .filter(i -> i.getControl().getId().equals(ac.getControl().getId()))
                        .toList();
                for (QuestionnaireTemplateItem i : controlItems) {
                    AuditQuestionnaireItem item = AuditQuestionnaireItem.builder()
                            .snapshot(snapshot)
                            .auditControl(ac)
                            .question(i.getQuestion())
                            .control(ac.getControl())
                            .questionText(i.getQuestionText())
                            .helpText(i.getHelpText())
                            .displayOrder(i.getDisplayOrder())
                            .askOwner(Boolean.TRUE.equals(i.getAskOwner()))
                            .build();
                    auditQuestionnaireItemRepository.save(item);
                }
            }
            return snapshot;
        }

        for (AuditControl ac : auditControls) {
            List<QuestionControlMapping> mappings = questionControlMappingRepository.findByControl_IdOrderByQuestionDisplayOrderAsc(ac.getControl().getId()).stream()
                    .sorted(Comparator.comparingInt(m -> m.getQuestion().getDisplayOrder()))
                    .toList();
            for (QuestionControlMapping mapping : mappings) {
                Question q = mapping.getQuestion();
                AuditQuestionnaireItem item = AuditQuestionnaireItem.builder()
                        .snapshot(snapshot)
                        .auditControl(ac)
                        .question(q)
                        .control(ac.getControl())
                        .questionText(q.getQuestionText())
                        .helpText(q.getHelpText())
                        .displayOrder(q.getDisplayOrder())
                        .askOwner(Boolean.TRUE.equals(q.getAskOwner()))
                        .build();
                auditQuestionnaireItemRepository.save(item);
            }
        }
        return snapshot;
    }

    private AuditAssignmentDto toAssignmentDto(AuditAssignment aa) {
        return AuditAssignmentDto.builder()
                .id(aa.getId())
                .auditId(aa.getAudit().getId())
                .userId(aa.getUser().getId())
                .userEmail(aa.getUser().getEmail())
                .userDisplayName(aa.getUser().getDisplayName())
                .assignmentRole(aa.getAssignmentRole())
                .active(aa.getActive())
                .assignedAt(aa.getAssignedAt())
                .build();
    }

    private void syncPrimaryAssignment(Audit audit, User user) {
        deactivatePrimaryAssignments(audit.getId());
        AuditAssignment primary = auditAssignmentRepository.findByAuditIdAndUserIdAndAssignmentRole(audit.getId(), user.getId(), AuditAssignmentRole.PRIMARY)
                .orElse(AuditAssignment.builder().audit(audit).user(user).assignmentRole(AuditAssignmentRole.PRIMARY).build());
        primary.setActive(true);
        auditAssignmentRepository.save(primary);
    }

    private void deactivatePrimaryAssignments(Long auditId) {
        List<AuditAssignment> primaries = auditAssignmentRepository.findByAuditIdAndAssignmentRole(auditId, AuditAssignmentRole.PRIMARY);
        for (AuditAssignment a : primaries) {
            a.setActive(false);
        }
        auditAssignmentRepository.saveAll(primaries);
    }
}
