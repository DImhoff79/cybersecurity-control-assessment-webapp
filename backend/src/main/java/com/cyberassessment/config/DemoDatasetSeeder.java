package com.cyberassessment.config;

import com.cyberassessment.dto.ApplicationDto;
import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.ControlExceptionCreateRequest;
import com.cyberassessment.dto.FindingDto;
import com.cyberassessment.dto.FindingUpsertRequest;
import com.cyberassessment.dto.RemediationPlanDto;
import com.cyberassessment.dto.RiskRegisterItemDto;
import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.ApplicationCriticality;
import com.cyberassessment.entity.ApplicationLifecycleStatus;
import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditStatus;
import com.cyberassessment.entity.DataClassification;
import com.cyberassessment.entity.Finding;
import com.cyberassessment.entity.FindingSeverity;
import com.cyberassessment.entity.FindingStatus;
import com.cyberassessment.entity.RiskStatus;
import com.cyberassessment.entity.User;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.AuditControlRepository;
import com.cyberassessment.repository.AuditProjectRepository;
import com.cyberassessment.repository.AuditRepository;
import com.cyberassessment.repository.ComplianceRequirementRepository;
import com.cyberassessment.repository.ControlExceptionRepository;
import com.cyberassessment.repository.ControlRepository;
import com.cyberassessment.repository.FindingRepository;
import com.cyberassessment.repository.PolicyRepository;
import com.cyberassessment.repository.RegulationRepository;
import com.cyberassessment.repository.RemediationPlanRepository;
import com.cyberassessment.repository.RiskRegisterItemRepository;
import com.cyberassessment.repository.UserRepository;
import com.cyberassessment.service.ApplicationService;
import com.cyberassessment.service.AuditProjectService;
import com.cyberassessment.service.AuditService;
import com.cyberassessment.service.ComplianceService;
import com.cyberassessment.service.ControlExceptionService;
import com.cyberassessment.service.FindingService;
import com.cyberassessment.service.RemediationService;
import com.cyberassessment.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Idempotent rich demo data for local/staging so modules show realistic cross-links.
 * Disable with {@code app.seed.demo-dataset=false} or by turning off {@code app.auth.seed-local-users}.
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class DemoDatasetSeeder implements ApplicationRunner {

    private static final String DEMO = "[Demo] ";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final int DEMO_AUDIT_YEAR = 2026;
    private static final int DEMO_PROJECT_YEAR = 2027;

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final AuditService auditService;
    private final AuditRepository auditRepository;
    private final AuditControlRepository auditControlRepository;
    private final AuditProjectRepository auditProjectRepository;
    private final AuditProjectService auditProjectService;
    private final FindingService findingService;
    private final FindingRepository findingRepository;
    private final ControlExceptionService controlExceptionService;
    private final ControlExceptionRepository controlExceptionRepository;
    private final RiskService riskService;
    private final RiskRegisterItemRepository riskRegisterItemRepository;
    private final RemediationService remediationService;
    private final RemediationPlanRepository remediationPlanRepository;
    private final ComplianceService complianceService;
    private final RegulationRepository regulationRepository;
    private final ComplianceRequirementRepository complianceRequirementRepository;
    private final ControlRepository controlRepository;
    private final PolicyRepository policyRepository;

    @Value("${app.seed.demo-dataset:true}")
    private boolean demoDatasetEnabled;

    @Value("${app.auth.seed-local-users:true}")
    private boolean seedLocalUsers;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!demoDatasetEnabled) {
            log.info("Skipping demo dataset (app.seed.demo-dataset=false)");
            return;
        }
        if (!seedLocalUsers) {
            log.info("Skipping demo dataset (local users not seeded)");
            return;
        }
        if (!userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.warn("Skipping demo dataset: {} not found", ADMIN_EMAIL);
            return;
        }
        runAsAdmin(this::seed);
    }

    private void runAsAdmin(Runnable action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN_EMAIL, "", List.of()));
            SecurityContextHolder.setContext(ctx);
            action.run();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private void seed() {
        User owner = userRepository.findByEmail("owner@example.com").orElse(null);
        User auditor = userRepository.findByEmail("auditor@example.com").orElse(null);
        if (owner == null || auditor == null) {
            log.warn("Skipping demo dataset: default owner/auditor users missing");
            return;
        }

        List<Application> apps = ensureApplications(owner);
        if (apps.size() < 4) {
            return;
        }

        ensureAuditsAndStatuses(apps);
        ensureAuditProject(apps);
        ensureFindings(apps, owner, auditor);
        ensureControlExceptions(apps);
        ensureRisks(apps, owner);
        ensureRemediationPlans(owner);
        ensureComplianceCatalog();

        log.info("Demo dataset check complete (applications named '{}*')", DEMO);
    }

    private List<Application> ensureApplications(User owner) {
        record AppSeed(String name, String description, ApplicationCriticality crit, DataClassification cls,
                       String regulatory, String biz, String tech) {
        }
        AppSeed[] seeds = {
                new AppSeed(DEMO + "Northwind Payments API", "Card-present and e-commerce payment orchestration APIs.",
                        ApplicationCriticality.CRITICAL, DataClassification.RESTRICTED, "PCI-DSS 4.0",
                        "Jordan Lee", "Priya Singh"),
                new AppSeed(DEMO + "Aurora HR Portal", "Employee self-service, benefits, and payroll previews.",
                        ApplicationCriticality.MEDIUM, DataClassification.CONFIDENTIAL, "SOC 2 / HR privacy",
                        "Morgan Ellis", "Casey Nguyen"),
                new AppSeed(DEMO + "Helios Analytics Warehouse", "Centralized metrics store for product and finance KPIs.",
                        ApplicationCriticality.HIGH, DataClassification.CONFIDENTIAL, "SOX ITGC adjacent",
                        "Riley Park", "Devon Clarke"),
                new AppSeed(DEMO + "Vertex B2B Gateway", "Partner-facing APIs and file exchange for inventory sync.",
                        ApplicationCriticality.MEDIUM, DataClassification.INTERNAL, "Contractual security addendum",
                        "Taylor Brooks", "Jamal Ortiz")
        };
        List<Application> out = new ArrayList<>();
        for (AppSeed s : seeds) {
            Application app = applicationRepository.findFirstByName(s.name).orElseGet(() -> {
                ApplicationDto dto = applicationService.create(
                        s.name,
                        s.description,
                        owner.getId(),
                        s.crit,
                        s.cls,
                        s.regulatory,
                        s.biz,
                        s.tech,
                        ApplicationLifecycleStatus.ACTIVE
                );
                return applicationRepository.findById(dto.getId()).orElseThrow();
            });
            out.add(app);
        }
        return out;
    }

    private void ensureAuditsAndStatuses(List<Application> apps) {
        Instant baseDue = Instant.now().plus(45, ChronoUnit.DAYS);
        AuditStatus[] targets = {
                AuditStatus.IN_PROGRESS,
                AuditStatus.SUBMITTED,
                AuditStatus.DRAFT,
                AuditStatus.COMPLETE
        };
        for (int i = 0; i < apps.size(); i++) {
            Application app = apps.get(i);
            final int idx = i;
            AuditDto dto = auditRepository.findByApplicationIdAndYear(app.getId(), DEMO_AUDIT_YEAR)
                    .map(a -> AuditService.toDto(a))
                    .orElseGet(() -> auditService.create(app.getId(), DEMO_AUDIT_YEAR,
                            baseDue.plus(idx, ChronoUnit.DAYS), null));
            if (!Objects.equals(dto.getStatus(), targets[i])) {
                auditService.update(dto.getId(), targets[i], dto.getDueAt());
            }
        }
    }

    private void ensureAuditProject(List<Application> apps) {
        String projectName = DEMO + "FY2027 Cross-system assurance";
        if (auditProjectRepository.findFirstByName(projectName).isPresent()) {
            return;
        }
        List<Long> ids = List.of(apps.get(0).getId(), apps.get(1).getId(), apps.get(2).getId());
        auditProjectService.create(
                projectName,
                "SOC2 + PCI",
                DEMO_PROJECT_YEAR,
                "Synthetic program spanning payments, HR, and analytics for UI walkthroughs.",
                Instant.now().minus(30, ChronoUnit.DAYS),
                Instant.now().plus(120, ChronoUnit.DAYS),
                ids
        );
        log.info("Seeded demo audit project: {}", projectName);
    }

    private void ensureFindings(List<Application> apps, User owner, User auditor) {
        for (int i = 0; i < 4; i++) {
            Application app = apps.get(i);
            Audit audit = auditRepository.findByApplicationIdAndYear(app.getId(), DEMO_AUDIT_YEAR).orElse(null);
            if (audit == null) {
                continue;
            }
            if (findingRepository.countByAudit_Id(audit.getId()) > 0) {
                continue;
            }
            var controls = auditControlRepository.findByAuditId(audit.getId());
            if (controls.isEmpty()) {
                continue;
            }
            Long acId = controls.get(Math.min(i, controls.size() - 1)).getId();
            FindingUpsertRequest req = FindingUpsertRequest.builder()
                    .auditId(audit.getId())
                    .auditControlId(acId)
                    .title(DEMO + switch (i) {
                        case 0 -> "HSM key ceremony documentation incomplete";
                        case 1 -> "Privileged role recertification overdue";
                        case 2 -> "Warehouse access logs retention gap";
                        default -> "Partner JWT validation not enforced on legacy route";
                    })
                    .description("Seeded finding for local demos — replace with real assessment notes.")
                    .severity(i == 0 ? FindingSeverity.CRITICAL : i == 1 ? FindingSeverity.HIGH : FindingSeverity.MEDIUM)
                    .status(i == 3 ? FindingStatus.IN_PROGRESS : FindingStatus.OPEN)
                    .ownerUserId(i % 2 == 0 ? owner.getId() : auditor.getId())
                    .dueAt(Instant.now().plus(14 + i * 3L, ChronoUnit.DAYS))
                    .build();
            findingService.create(req);
        }
    }

    private void ensureControlExceptions(List<Application> apps) {
        if (controlExceptionRepository.countByReasonContaining("[demo-exception]") >= 4) {
            return;
        }
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        for (int i = 0; i < 4; i++) {
            Application app = apps.get(i);
            Audit audit = auditRepository.findByApplicationIdAndYear(app.getId(), DEMO_AUDIT_YEAR).orElse(null);
            if (audit == null) {
                continue;
            }
            List<Finding> findings = findingRepository.findByAuditIdOrderByDueAtAscCreatedAtDesc(audit.getId());
            Long findingId = findings.isEmpty() ? null : findings.get(0).getId();
            var controls = auditControlRepository.findByAuditId(audit.getId());
            Long auditControlId = (findingId != null && findings.get(0).getAuditControl() != null)
                    ? findings.get(0).getAuditControl().getId()
                    : (controls.isEmpty() ? null : controls.get(0).getId());

            controlExceptionService.request(ControlExceptionCreateRequest.builder()
                    .auditId(audit.getId())
                    .auditControlId(auditControlId)
                    .findingId(findingId)
                    .reason("[demo-exception] " + switch (i) {
                        case 0 -> "Temporary compensating monitoring while HSM vendor patch is scheduled.";
                        case 1 -> "Emergency access workflow extension for payroll freeze window.";
                        case 2 -> "Log shipping delay to SIEM during storage migration.";
                        default -> "Legacy partner certificate rollover — 30 day variance.";
                    })
                    .compensatingControl("Weekly manual control checks by security operations; ticket " + (4000 + i))
                    .expiresAt(Instant.now().plus(20 + i * 5L, ChronoUnit.DAYS))
                    .build());
        }
        // tie one exception to owner for realism
        log.info("Seeded demo control exceptions (marker [demo-exception])");
    }

    private void ensureRisks(List<Application> apps, User owner) {
        if (riskRegisterItemRepository.countByTitleStartingWith(DEMO) >= 4) {
            return;
        }
        record R(String title, String impact, int l, int imp, RiskStatus st, int appIdx) {
        }
        R[] seeds = {
                new R(DEMO + "Key custody and rotation posture", "Regulatory and fraud exposure if keys are mishandled.",
                        4, 5, RiskStatus.OPEN, 0),
                new R(DEMO + "Third-party identity federation", "Partner auth bugs could broaden blast radius.",
                        3, 4, RiskStatus.MONITORING, 3),
                new R(DEMO + "Analytics data residency", "Cross-border copy risks for EU employee data.",
                        3, 5, RiskStatus.OPEN, 2),
                new R(DEMO + "Emergency access process drift", "Break-glass events may go unaudited.",
                        4, 3, RiskStatus.MITIGATED, 1)
        };
        for (R s : seeds) {
            Application app = apps.get(Math.min(Math.max(s.appIdx, 0), apps.size() - 1));
            riskService.create(
                    s.title,
                    "Demo risk — " + s.title,
                    s.impact,
                    s.l,
                    s.imp,
                    owner.getId(),
                    app.getId(),
                    null,
                    Instant.now().plus(90, ChronoUnit.DAYS)
            );
        }
        // align status for monitoring/mitigated after create (defaults OPEN)
        List<RiskRegisterItemDto> all = riskService.list();
        for (RiskRegisterItemDto row : all) {
            if (!row.getTitle().startsWith(DEMO)) {
                continue;
            }
            if (row.getTitle().contains("Third-party identity") && row.getStatus() != RiskStatus.MONITORING) {
                riskService.update(row.getId(), null, null, null, null, null, null, RiskStatus.MONITORING, null, null);
            }
            if (row.getTitle().contains("Emergency access") && row.getStatus() != RiskStatus.MITIGATED) {
                riskService.update(row.getId(), null, null, null, null, null, null, RiskStatus.MITIGATED, 6, null);
            }
        }
        log.info("Seeded demo risk register items");
    }

    private void ensureRemediationPlans(User owner) {
        if (remediationPlanRepository.countByTitleStartingWith(DEMO) >= 4) {
            return;
        }
        List<RiskRegisterItemDto> risks = riskService.list().stream()
                .filter(r -> r.getTitle() != null && r.getTitle().startsWith(DEMO))
                .toList();
        if (risks.size() < 4) {
            return;
        }
        String[] titles = {
                DEMO + "Plan: HSM remediation track",
                DEMO + "Plan: Federation hardening",
                DEMO + "Plan: Data residency controls",
                DEMO + "Plan: Emergency access cleanup"
        };
        for (int i = 0; i < 4; i++) {
            Long riskId = risks.get(i).getId();
            RemediationPlanDto plan = remediationService.createPlan(
                    riskId,
                    titles[i],
                    null,
                    null,
                    null,
                    null,
                    Instant.now().plus(60 + i * 10L, ChronoUnit.DAYS)
            );
            remediationService.updatePlan(
                    plan.getId(),
                    null,
                    "Phased remediation with validation gates and security sign-off.",
                    "30 / 60 / 90 day workstreams",
                    "Interim detective controls and enhanced logging",
                    "Reduces inherent risk score and supports audit evidence packs.",
                    null,
                    plan.getTargetCompleteAt()
            );
            remediationService.submitForApproval(plan.getId());
            remediationService.decideApproval(plan.getId(), true, "Demo approval for local dataset.");
            remediationService.createAction(
                    plan.getId(),
                    "Complete design review",
                    "Document control flow and owners.",
                    owner.getId(),
                    Instant.now().plus(21, ChronoUnit.DAYS),
                    1
            );
            if (i < 2) {
                remediationService.createAction(
                        plan.getId(),
                        "Deploy configuration guardrails",
                        "Enforce policy in staging then production.",
                        owner.getId(),
                        Instant.now().plus(35, ChronoUnit.DAYS),
                        2
                );
            }
        }
        log.info("Seeded demo remediation plans and actions");
    }

    private void ensureComplianceCatalog() {
        String code = "DEMO-SAMPLE-REG";
        Long regulationId = regulationRepository.findByCode(code)
                .map(r -> r.getId())
                .orElseGet(() -> complianceService.createRegulation(
                        code,
                        "Sample financial services control framework (demo)",
                        "2024",
                        "Synthetic regulation for obligation mapping walkthroughs in local dev.",
                        true
                ).getId());

        List<?> existing = complianceRequirementRepository.findByRegulationIdOrderByRequirementCodeAsc(regulationId);
        if (existing.size() >= 4) {
            return;
        }

        String[][] reqs = {
                {"DEMO-1.1", "Logical access to cardholder data is restricted."},
                {"DEMO-1.2", "Password and MFA policies are enforced for admins."},
                {"DEMO-2.1", "Audit trails capture privileged actions."},
                {"DEMO-3.1", "Vendor security reviews are documented."}
        };
        for (String[] r : reqs) {
            complianceService.createRequirement(regulationId, r[0], r[0] + " " + r[1], r[1], true);
        }

        controlRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .findFirst()
                .ifPresent(ctrl -> {
                    var reqList = complianceRequirementRepository.findByRegulationIdOrderByRequirementCodeAsc(regulationId);
                    if (!reqList.isEmpty()) {
                        try {
                            complianceService.mapRequirementToControl(reqList.get(0).getId(), ctrl.getId(), 85,
                                    "Demo mapping — high-level coverage.");
                        } catch (IllegalArgumentException ignored) {
                            // duplicate mapping
                        }
                    }
                });

        policyRepository.findByCode("PL-ACCESS").ifPresent(pol -> {
            var reqList = complianceRequirementRepository.findByRegulationIdOrderByRequirementCodeAsc(regulationId);
            if (reqList.size() > 1) {
                try {
                    complianceService.mapPolicyToRequirement(pol.getId(), reqList.get(1).getId(),
                            "Demo link between published access standard and obligation.");
                } catch (IllegalArgumentException ignored) {
                    // duplicate
                }
            }
        });

        log.info("Seeded demo regulation {} with requirements", code);
    }
}
