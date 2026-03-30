package com.cyberassessment.service;

import com.cyberassessment.config.AuditSeedProperties;
import com.cyberassessment.dto.*;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.*;
import com.cyberassessment.util.RegulatoryScopeMatcher;
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
    private final QuestionRepository questionRepository;
    private final AuditQuestionnaireSnapshotRepository auditQuestionnaireSnapshotRepository;
    private final AuditQuestionnaireItemRepository auditQuestionnaireItemRepository;
    private final AuditAssignmentRepository auditAssignmentRepository;
    private final FindingRepository findingRepository;
    private final AuditApprovalStepRepository auditApprovalStepRepository;
    private final AuditApprovalService auditApprovalService;
    private final AuditProjectRepository auditProjectRepository;
    private final QuestionnaireTemplateService questionnaireTemplateService;
    private final QuestionnaireTemplateItemRepository questionnaireTemplateItemRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final AuditActivityLogService auditActivityLogService;
    private final JavaMailSender mailSender;
    private final AuditSeedProperties auditSeedProperties;
    private final ApplicationSecurityReviewService applicationSecurityReviewService;
    private final OwnerAnswerOptionProfileService ownerAnswerOptionProfileService;

    public AuditService(AuditRepository auditRepository, ApplicationRepository applicationRepository,
                        ControlRepository controlRepository, AuditControlRepository auditControlRepository,
                        AuditControlAnswerRepository auditControlAnswerRepository,
                        QuestionControlMappingRepository questionControlMappingRepository,
                        QuestionRepository questionRepository,
                        AuditQuestionnaireSnapshotRepository auditQuestionnaireSnapshotRepository,
                        AuditQuestionnaireItemRepository auditQuestionnaireItemRepository,
                        AuditAssignmentRepository auditAssignmentRepository,
                        FindingRepository findingRepository,
                        AuditApprovalStepRepository auditApprovalStepRepository,
                        AuditApprovalService auditApprovalService,
                        AuditProjectRepository auditProjectRepository,
                        QuestionnaireTemplateService questionnaireTemplateService,
                        QuestionnaireTemplateItemRepository questionnaireTemplateItemRepository,
                        CurrentUserService currentUserService, UserRepository userRepository,
                        AuditActivityLogService auditActivityLogService,
                        AuditSeedProperties auditSeedProperties,
                        ApplicationSecurityReviewService applicationSecurityReviewService,
                        OwnerAnswerOptionProfileService ownerAnswerOptionProfileService,
                        @Autowired(required = false) JavaMailSender mailSender) {
        this.auditRepository = auditRepository;
        this.applicationRepository = applicationRepository;
        this.controlRepository = controlRepository;
        this.auditControlRepository = auditControlRepository;
        this.auditControlAnswerRepository = auditControlAnswerRepository;
        this.questionControlMappingRepository = questionControlMappingRepository;
        this.questionRepository = questionRepository;
        this.auditQuestionnaireSnapshotRepository = auditQuestionnaireSnapshotRepository;
        this.auditQuestionnaireItemRepository = auditQuestionnaireItemRepository;
        this.auditAssignmentRepository = auditAssignmentRepository;
        this.findingRepository = findingRepository;
        this.auditApprovalStepRepository = auditApprovalStepRepository;
        this.auditApprovalService = auditApprovalService;
        this.auditProjectRepository = auditProjectRepository;
        this.questionnaireTemplateService = questionnaireTemplateService;
        this.questionnaireTemplateItemRepository = questionnaireTemplateItemRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.auditActivityLogService = auditActivityLogService;
        this.auditSeedProperties = auditSeedProperties;
        this.applicationSecurityReviewService = applicationSecurityReviewService;
        this.ownerAnswerOptionProfileService = ownerAnswerOptionProfileService;
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
                .projectId(a.getAuditProject() != null ? a.getAuditProject().getId() : null)
                .projectName(a.getAuditProject() != null ? a.getAuditProject().getName() : null)
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
        return create(applicationId, year, dueAt, null);
    }

    @Transactional
    public AuditDto create(Long applicationId, Integer year, Instant dueAt, Long auditProjectId) {
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: AUDIT_MANAGEMENT");
        }
        Application app = applicationRepository.findById(applicationId).orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        AuditProject project = null;
        if (auditProjectId != null) {
            project = auditProjectRepository.findById(auditProjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Audit project not found: " + auditProjectId));
            boolean appInScope = project.getApplications().stream().anyMatch(a -> Objects.equals(a.getId(), applicationId));
            if (!appInScope) {
                throw new IllegalArgumentException("Application is not in scope for this audit project");
            }
            if (project.getYear() != null && !Objects.equals(project.getYear(), year)) {
                throw new IllegalArgumentException("Audit year must match audit project year");
            }
        }
        if (auditRepository.findByApplicationIdAndYear(applicationId, year).isPresent()) {
            throw new IllegalArgumentException("Audit already exists for this application and year");
        }
        Audit audit = Audit.builder()
                .application(app)
                .auditProject(project)
                .year(year)
                .status(AuditStatus.DRAFT)
                .dueAt(dueAt)
                .build();
        audit = auditRepository.save(audit);
        seedAuditControlsAndSnapshot(audit);
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_CREATED, "Created audit for " + app.getName() + " (" + year + ")");
        return toDto(audit);
    }

    /**
     * Application owner starts an assessment without an audit project (intake / new application).
     */
    @Transactional
    public AuditDto createOwnerIntakeAudit(Long applicationId, Integer year) {
        User me = currentUserService.getCurrentUser().orElseThrow(() -> new IllegalArgumentException("Not authenticated"));
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        if (app.getOwner() == null || !Objects.equals(app.getOwner().getId(), me.getId())) {
            throw new IllegalArgumentException("Only the application owner can start this assessment");
        }
        if (auditRepository.findByApplicationIdAndYear(applicationId, year).isPresent()) {
            throw new IllegalArgumentException("An assessment already exists for this application and year");
        }
        Audit audit = Audit.builder()
                .application(app)
                .auditProject(null)
                .year(year)
                .status(AuditStatus.DRAFT)
                .build();
        audit = auditRepository.save(audit);
        seedAuditControlsAndSnapshot(audit);
        syncPrimaryAssignment(audit, me);
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_CREATED,
                "Owner intake assessment for " + app.getName() + " (" + year + ")");
        return toDto(audit);
    }

    private void seedAuditControlsAndSnapshot(Audit audit) {
        Application app = audit.getApplication();
        List<Control> enabledControls = controlRepository.findAllEnabledWithRegulatoryScopes();
        Optional<ControlFramework> frameworkOnly = auditSeedProperties.getNewAuditFrameworkFilter();
        if (frameworkOnly.isPresent()) {
            enabledControls = enabledControls.stream()
                    .filter(c -> c.getFramework() == frameworkOnly.get())
                    .toList();
        }
        for (Control c : enabledControls) {
            if (auditSeedProperties.isRegulatoryScopeFilterEnabled()
                    && !RegulatoryScopeMatcher.controlAppliesToApplication(c, app)) {
                continue;
            }
            AuditControl ac = AuditControl.builder()
                    .audit(audit)
                    .control(c)
                    .status(ControlAssessmentStatus.NOT_STARTED)
                    .build();
            auditControlRepository.save(ac);
        }
        createQuestionnaireSnapshot(audit);
    }

    @Transactional(readOnly = true)
    public AuditDto findById(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElse(null);
        if (audit == null) {
            return null;
        }
        ensureCanAccessAudit(audit);
        return enrichAuditDto(audit);
    }

    private AuditDto enrichAuditDto(Audit audit) {
        AuditDto dto = AuditService.toDto(audit);
        enrichPendingAuditor(audit.getId(), dto);
        if (audit.getApplication() != null && audit.getApplication().getId() != null) {
            applicationSecurityReviewService.findSummary(audit.getApplication().getId())
                    .ifPresent(dto::setSecurityArchitectureReview);
        }
        return dto;
    }

    private void enrichPendingAuditor(Long auditId, AuditDto dto) {
        List<AuditApprovalStep> pending = auditApprovalStepRepository
                .findByAuditIdAndStepStatusOrderByStepOrderAsc(auditId, AuditApprovalStepStatus.PENDING);
        User u = null;
        if (!pending.isEmpty()) {
            u = pending.get(0).getAssignedTo();
        } else if (dto.getStatus() == AuditStatus.PENDING_APPROVAL) {
            u = resolveReviewerAuditorForPendingApproval(auditId);
        }
        if (u == null) {
            return;
        }
        dto.setPendingAuditorUserId(u.getId());
        dto.setPendingAuditorEmail(u.getEmail());
        dto.setPendingAuditorFirstName(u.getFirstName());
        dto.setPendingAuditorLastName(u.getLastName());
        dto.setPendingAuditorDisplayName(u.getDisplayName());
    }

    /**
     * When an audit is awaiting approval but has no approval-step rows (legacy data or repair),
     * use the first active REVIEWER assignment with role AUDITOR — same rule as workflow seeding.
     */
    private User resolveReviewerAuditorForPendingApproval(Long auditId) {
        List<AuditAssignment> reviewers = auditAssignmentRepository
                .findByAuditIdAndAssignmentRoleAndActiveTrueOrderByAssignedAtAsc(auditId, AuditAssignmentRole.REVIEWER);
        for (AuditAssignment ar : reviewers) {
            User candidate = ar.getUser();
            if (candidate != null && candidate.getRole() == UserRole.AUDITOR) {
                return candidate;
            }
        }
        return null;
    }

    @Transactional
    public AuditDto update(Long auditId, AuditStatus status) {
        return update(auditId, status, null);
    }

    @Transactional
    public AuditDto update(Long auditId, AuditStatus status, Instant dueAt) {
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: AUDIT_MANAGEMENT");
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
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: AUDIT_MANAGEMENT");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_DELETED, "Audit deleted");
        auditRepository.delete(audit);
    }

    @Transactional
    public AuditDto assign(Long auditId, Long userId) {
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: AUDIT_MANAGEMENT");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        User user = userId != null ? userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)) : null;
        requireAuditorAssignee(user);
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
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: AUDIT_MANAGEMENT");
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
        List<AuditDto> dtos;
        if (currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            dtos = auditRepository.findAll().stream().map(AuditService::toDto).collect(Collectors.toList());
        } else {
            List<Audit> direct = auditRepository.findByAssignedTo(current);
            List<Audit> collaboratorAudits = auditAssignmentRepository.findByUserIdAndActiveTrue(current.getId())
                    .stream()
                    .map(AuditAssignment::getAudit)
                    .toList();
            List<Audit> ownedAppAudits = auditRepository.findByApplicationOwnerId(current.getId());
            List<Audit> approvalAudits = auditApprovalStepRepository.findDistinctAuditsByAssignedToUserId(current.getId());
            List<Audit> findingOwnerAudits = findingRepository.findDistinctAuditsByOwnerUserId(current.getId());
            dtos = java.util.stream.Stream.of(direct.stream(), collaboratorAudits.stream(), ownedAppAudits.stream(), approvalAudits.stream(), findingOwnerAudits.stream())
                    .flatMap(s -> s)
                    .collect(Collectors.toMap(Audit::getId, a -> a, (a, b) -> a))
                    .values().stream()
                    .map(AuditService::toDto)
                    .toList();
        }
        attachSecurityReviews(dtos);
        return dtos;
    }

    private void attachSecurityReviews(List<AuditDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        Set<Long> appIds = dtos.stream()
                .map(AuditDto::getApplicationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ApplicationSecurityReviewSummaryDto> byApp = applicationSecurityReviewService.findSummariesByApplicationIds(appIds);
        for (AuditDto d : dtos) {
            if (d.getApplicationId() != null) {
                ApplicationSecurityReviewSummaryDto s = byApp.get(d.getApplicationId());
                if (s != null) {
                    d.setSecurityArchitectureReview(s);
                }
            }
        }
    }

    /**
     * Audit IDs the current user may access for workspace features (exceptions, etc.).
     */
    @Transactional(readOnly = true)
    public List<Long> findAccessibleAuditIdsForCurrentUser() {
        Optional<User> currentOpt = currentUserService.getCurrentUser();
        if (currentOpt.isEmpty()) {
            return List.of();
        }
        User current = currentOpt.get();
        if (currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            return auditRepository.findAll().stream().map(Audit::getId).toList();
        }
        List<Audit> direct = auditRepository.findByAssignedTo(current);
        List<Audit> collaboratorAudits = auditAssignmentRepository.findByUserIdAndActiveTrue(current.getId())
                .stream()
                .map(AuditAssignment::getAudit)
                .toList();
        List<Audit> ownedAppAudits = auditRepository.findByApplicationOwnerId(current.getId());
        List<Audit> approvalAudits = auditApprovalStepRepository.findDistinctAuditsByAssignedToUserId(current.getId());
        List<Audit> findingOwnerAudits = findingRepository.findDistinctAuditsByOwnerUserId(current.getId());
        return java.util.stream.Stream.of(direct.stream(), collaboratorAudits.stream(), ownedAppAudits.stream(), approvalAudits.stream(), findingOwnerAudits.stream())
                .flatMap(s -> s)
                .collect(Collectors.toMap(Audit::getId, a -> a, (a, b) -> a))
                .values().stream()
                .map(Audit::getId)
                .toList();
    }

    /** Owner response UI only: questions with {@code ask_owner = true} in the library (auditor-only mappings are excluded). */
    @Transactional(readOnly = true)
    public List<AuditQuestionItemDto> getQuestionsForAudit(Long auditId) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        ensureCanAccessAudit(audit);
        Set<Long> allowedControlIds = allowedControlIdsForCurrentUser(audit);
        List<AuditQuestionItemDto> result = new ArrayList<>();
        for (AuditControl ac : auditControlRepository.findByAudit(audit)) {
            Control c = ac.getControl();
            if (!allowedControlIds.contains(c.getId())) {
                continue;
            }
            for (Question q : allMappedQuestionsForAuditControl(ac)) {
                if (!Boolean.TRUE.equals(q.getAskOwner())) {
                    continue;
                }
                Optional<AuditControlAnswer> existing = ac.getAnswers().stream()
                        .filter(a -> a.getQuestion().getId().equals(q.getId()))
                        .findFirst();
                ResolvedOwnerAnswerUi ownerUi = ownerAnswerOptionProfileService.resolveForQuestion(q);
                result.add(AuditQuestionItemDto.builder()
                        .questionId(q.getId())
                        .auditControlId(ac.getId())
                        .controlId(c.getId())
                        .controlControlId(c.getControlId())
                        .controlName(c.getName())
                        .questionText(q.getQuestionText())
                        .helpText(q.getHelpText())
                        .displayOrder(q.getDisplayOrder())
                        .askOwner(true)
                        .existingAnswerText(existing.map(AuditControlAnswer::getAnswerText).orElse(null))
                        .ownerAnswerOptionProfileId(ownerUi.getProfileId())
                        .ownerAnswerOptionProfileCode(ownerUi.getCode())
                        .ownerResponseFieldLabel(ownerUi.getFieldLabel())
                        .ownerResponseFieldHint(ownerUi.getFieldHint())
                        .ownerResponseOptions(ownerUi.getOptions())
                        .build());
            }
        }
        result.sort(Comparator
                .comparing(AuditQuestionItemDto::getControlControlId, Comparator.nullsLast(String::compareTo))
                .thenComparing(AuditQuestionItemDto::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(AuditQuestionItemDto::getQuestionId, Comparator.nullsLast(Long::compareTo)));
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
        if (audit.getStatus() == AuditStatus.PENDING_APPROVAL
                || audit.getStatus() == AuditStatus.AUDITOR_APPROVED
                || audit.getStatus() == AuditStatus.ATTESTED
                || audit.getStatus() == AuditStatus.COMPLETE) {
            throw new IllegalArgumentException("Answers cannot be edited in the current audit status");
        }
        Set<Long> allowedControlIds = allowedControlIdsForCurrentUser(audit);
        for (SubmitAnswersRequest.AnswerItem item : request.getAnswers()) {
            if (item.getAuditControlId() == null || item.getQuestionId() == null) continue;
            AuditControl ac = auditControlRepository.findById(item.getAuditControlId()).orElse(null);
            if (ac == null || !ac.getAudit().getId().equals(auditId)) continue;
            if (!allowedControlIds.contains(ac.getControl().getId())) continue;
            if (!questionControlMappingRepository.existsByControl_IdAndQuestion_Id(ac.getControl().getId(), item.getQuestionId())) {
                continue;
            }
            Question q = questionRepository.findById(item.getQuestionId()).orElse(null);
            if (q == null || !Boolean.TRUE.equals(q.getAskOwner())) {
                continue;
            }
            Optional<AuditControlAnswer> existing = auditControlAnswerRepository.findByAuditControlIdAndQuestionId(item.getAuditControlId(), item.getQuestionId());
            if (existing.isPresent()) {
                AuditControlAnswer a = existing.get();
                a.setAnswerText(item.getAnswerText());
                a.setAnsweredAt(Instant.now());
                auditControlAnswerRepository.save(a);
            } else {
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
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            User current = currentUserService.getCurrentUserOrThrow();
            User appOwner = audit.getApplication().getOwner();
            if (appOwner == null || !appOwner.getId().equals(current.getId())) {
                throw new IllegalArgumentException("Only the application owner can submit the full audit");
            }
        }
        if (!isAuditCompleteForOwner(audit)) {
            throw new IllegalArgumentException("Please answer all questions before submitting.");
        }
        if (audit.getStatus() != AuditStatus.DRAFT
                && audit.getStatus() != AuditStatus.IN_PROGRESS
                && audit.getStatus() != AuditStatus.REVISIONS_REQUESTED) {
            throw new IllegalArgumentException("This audit cannot be submitted in its current status");
        }
        auditApprovalService.prepareWorkflowForSubmit(audit);
        audit.setStatus(AuditStatus.PENDING_APPROVAL);
        if (audit.getCompletedAt() == null) {
            audit.setCompletedAt(Instant.now());
        }
        audit = auditRepository.save(audit);
        auditActivityLogService.log(audit, AuditActivityType.AUDIT_SUBMITTED, "Audit submitted by owner");
        return enrichAuditDto(audit);
    }

    @Transactional
    public AuditDto attest(Long auditId, String statement) {
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: AUDIT_MANAGEMENT");
        }
        if (audit.getStatus() != AuditStatus.AUDITOR_APPROVED && audit.getStatus() != AuditStatus.ATTESTED) {
            throw new IllegalArgumentException("Only auditor-approved audits can be attested");
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
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: AUDIT_MANAGEMENT");
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
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: AUDIT_MANAGEMENT");
        }
        User user = userId != null ? userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)) : null;
        requireAuditorAssignee(user);
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

    /** Primary assignee and collaborators must be {@link UserRole#AUDITOR} users. */
    private void requireAuditorAssignee(User user) {
        if (user == null) {
            return;
        }
        if (user.getRole() != UserRole.AUDITOR) {
            throw new IllegalArgumentException("Only users with the Auditor role can be assigned to an audit");
        }
    }

    private boolean isAuditCompleteForOwner(Audit audit) {
        List<AuditControl> auditControls = auditControlRepository.findByAudit(audit);
        for (AuditControl ac : auditControls) {
            List<Long> ownerQuestionIds = ownerQuestionsForAuditControl(ac).stream().map(Question::getId).toList();

            if (!ownerQuestionIds.isEmpty()) {
                for (Long qid : ownerQuestionIds) {
                    Optional<AuditControlAnswer> answer = auditControlAnswerRepository
                            .findByAuditControlIdAndQuestionId(ac.getId(), qid);
                    if (answer.isEmpty() || answer.get().getAnswerText() == null || answer.get().getAnswerText().isBlank()) {
                        return false;
                    }
                }
            } else if (questionControlMappingRepository.countByControl_Id(ac.getControl().getId()) == 0) {
                if (!(ac.getStatus() == ControlAssessmentStatus.PASS || ac.getStatus() == ControlAssessmentStatus.FAIL || ac.getStatus() == ControlAssessmentStatus.NA)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * All questions linked to the control via {@code question_control_mappings}, in the same order as
     * {@link com.cyberassessment.service.QuestionService#findByControlId(Long)} and the control catalog.
     */
    private List<Question> allMappedQuestionsForAuditControl(AuditControl ac) {
        Map<Long, Question> byId = new LinkedHashMap<>();
        for (QuestionControlMapping m : questionControlMappingRepository.findByControl_IdOrderByQuestionDisplayOrderAsc(ac.getControl().getId())) {
            byId.putIfAbsent(m.getQuestion().getId(), m.getQuestion());
        }
        return new ArrayList<>(byId.values());
    }

    /**
     * Owner-answerable subset for completion checks (matches {@link #getQuestionsForAudit} and {@link #submitAnswers}).
     */
    private List<Question> ownerQuestionsForAuditControl(AuditControl ac) {
        return allMappedQuestionsForAuditControl(ac).stream()
                .filter(q -> Boolean.TRUE.equals(q.getAskOwner()))
                .toList();
    }

    private void ensureCanAccessAudit(Audit audit) {
        User current = currentUserService.getCurrentUserOrThrow();
        if (currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) return;
        boolean direct = audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(current.getId());
        boolean collaborator = auditAssignmentRepository.existsByAuditIdAndUserIdAndActiveTrue(audit.getId(), current.getId());
        boolean appOwner = audit.getApplication().getOwner() != null
                && audit.getApplication().getOwner().getId().equals(current.getId());
        boolean approvalAssignee = auditApprovalStepRepository.existsByAuditIdAndAssignedTo_Id(audit.getId(), current.getId());
        boolean findingOwner = findingRepository.existsByAudit_IdAndOwner_Id(audit.getId(), current.getId());
        if (!direct && !collaborator && !appOwner && !approvalAssignee && !findingOwner) {
            throw new IllegalArgumentException("You do not have access to this audit");
        }
    }

    private Set<Long> allowedControlIdsForCurrentUser(Audit audit) {
        if (currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            return auditControlRepository.findByAudit(audit).stream().map(ac -> ac.getControl().getId()).collect(Collectors.toSet());
        }
        User current = currentUserService.getCurrentUserOrThrow();
        boolean isPrimary = audit.getAssignedTo() != null && audit.getAssignedTo().getId().equals(current.getId());
        boolean isAppOwner = audit.getApplication().getOwner() != null
                && audit.getApplication().getOwner().getId().equals(current.getId());
        if (isPrimary || isAppOwner) {
            return auditControlRepository.findByAudit(audit).stream().map(ac -> ac.getControl().getId()).collect(Collectors.toSet());
        }
        if (auditAssignmentRepository.existsByAuditIdAndUserIdAndActiveTrue(audit.getId(), current.getId())) {
            return auditControlRepository.findByAudit(audit).stream().map(ac -> ac.getControl().getId()).collect(Collectors.toSet());
        }
        if (auditApprovalStepRepository.existsByAuditIdAndAssignedTo_Id(audit.getId(), current.getId())) {
            return auditControlRepository.findByAudit(audit).stream().map(ac -> ac.getControl().getId()).collect(Collectors.toSet());
        }
        if (findingRepository.existsByAudit_IdAndOwner_Id(audit.getId(), current.getId())) {
            return findingRepository.findByAudit_IdAndOwner_Id(audit.getId(), current.getId()).stream()
                    .filter(f -> f.getAuditControl() != null)
                    .map(f -> f.getAuditControl().getControl().getId())
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
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
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: AUDIT_MANAGEMENT");
        }
        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        requireAuditorAssignee(user);
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
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: AUDIT_MANAGEMENT");
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
        List<QuestionnaireTemplateItem> templateItems = publishedTemplate != null
                ? questionnaireTemplateItemRepository.findByTemplateIdOrderByDisplayOrderAscControlControlIdAsc(publishedTemplate.getId())
                : List.of();
        for (AuditControl ac : auditControls) {
            List<QuestionnaireTemplateItem> controlItems = templateItems.stream()
                    .filter(i -> i.getControl().getId().equals(ac.getControl().getId()))
                    .toList();
            if (!controlItems.isEmpty()) {
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
            } else {
                appendSnapshotItemsFromControlMappings(snapshot, ac);
            }
        }
        return snapshot;
    }

    private void appendSnapshotItemsFromControlMappings(AuditQuestionnaireSnapshot snapshot, AuditControl ac) {
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
