package com.cyberassessment.config;

import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.entity.NistCsfFunction;
import com.cyberassessment.entity.Policy;
import com.cyberassessment.entity.PolicyCsfMapping;
import com.cyberassessment.entity.PolicyStatus;
import com.cyberassessment.entity.PolicyVersion;
import com.cyberassessment.entity.PolicyVersionStatus;
import com.cyberassessment.repository.PolicyCsfMappingRepository;
import com.cyberassessment.repository.PolicyAcknowledgementRepository;
import com.cyberassessment.repository.PolicyRepository;
import com.cyberassessment.repository.PolicyRevisionEventRepository;
import com.cyberassessment.repository.PolicyVersionRepository;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository policyVersionRepository;
    private final PolicyCsfMappingRepository policyCsfMappingRepository;
    private final PolicyAcknowledgementRepository policyAcknowledgementRepository;
    private final PolicyRevisionEventRepository policyRevisionEventRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.seed-local-users:true}")
    private boolean seedLocalUsers;

    @Value("${app.seed.test-policies:true}")
    private boolean seedTestPolicies;

    @Value("${app.seed.test-policies-refresh:false}")
    private boolean refreshTestPolicies;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedLocalUsers) {
            log.info("Skipping local default user seeding (app.auth.seed-local-users=false)");
        } else {
            ensureDefaultUser("admin@example.com", "admin123", "System Administrator", UserRole.ADMIN);
            ensureDefaultUser("owner@example.com", "owner123", "Sample Application Owner", UserRole.APPLICATION_OWNER);
            ensureDefaultUser("auditor@example.com", "auditor123", "Sample Auditor", UserRole.AUDITOR);
            ensureDefaultUser("audit.manager@example.com", "manager123", "Sample Audit Manager", UserRole.AUDIT_MANAGER);
        }

        if (!seedTestPolicies) {
            log.info("Skipping sample policy seeding (app.seed.test-policies=false)");
            return;
        }
        if (!seedLocalUsers) {
            log.info("Skipping sample policy seeding (app.auth.seed-local-users=false; demo policies require demo user accounts)");
            return;
        }
        if (refreshTestPolicies) {
            log.info("Sample policy refresh mode enabled (app.seed.test-policies-refresh=true)");
        }
        seedSamplePolicies();
    }

    private void ensureDefaultUser(String email, String rawPassword, String displayName, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .displayName(displayName)
                .role(role)
                .permissions(role.defaultPermissions())
                .build();
        userRepository.save(user);
        log.info("Created default user: {} / {}", email, rawPassword);
    }

    private void seedSamplePolicies() {
        User manager = userRepository.findByEmail("audit.manager@example.com")
                .orElseGet(() -> ensureInlineUser("audit.manager@example.com", "manager123", "Sample Audit Manager", UserRole.AUDIT_MANAGER));
        User owner = userRepository.findByEmail("owner@example.com")
                .orElseGet(() -> ensureInlineUser("owner@example.com", "owner123", "Sample Application Owner", UserRole.APPLICATION_OWNER));

        ensurePolicySeed(
                "PL-TEST1",
                "Test Policy",
                "General security governance baseline used for UI testing.",
                owner,
                List.of(NistCsfFunction.GOVERN, NistCsfFunction.PROTECT),
                List.of(
                        versionSeed(1, "Version v1 - Test Policy", """
                                ## Scope
                                This policy applies to all production systems and shared services.

                                ## Requirements
                                1. Owners must maintain a current inventory.
                                2. Security reviews are required for major changes.
                                """, PolicyVersionStatus.RETIRED),
                        versionSeed(2, "Version v2 - Test Policy - working draft", """
                                ## Scope
                                This is the scope. Describe scope. It is a thing.

                                ## Requirements
                                These are the reqs.

                                Describe requirements.
                                """, PolicyVersionStatus.DRAFT)
                )
        );

        ensurePolicySeed(
                "PL-ACCESS",
                "Access Control Standard",
                "Covers authentication, least privilege, and access recertification expectations.",
                owner,
                List.of(NistCsfFunction.PROTECT, NistCsfFunction.DETECT),
                List.of(
                        versionSeed(1, "Access Control v1", """
                                <h2>Purpose</h2>
                                <p>Define baseline authentication and authorization controls.</p>
                                <h2>Core Controls</h2>
                                <ul>
                                  <li>MFA is required for administrative access.</li>
                                  <li>Privileged access must be time-bound.</li>
                                  <li>Quarterly access reviews are required.</li>
                                </ul>
                                """, PolicyVersionStatus.PUBLISHED)
                )
        );

        ensurePolicySeed(
                "PL-IR",
                "Incident Response Policy",
                "Documents preparation, detection, response, and recovery obligations.",
                manager,
                List.of(NistCsfFunction.DETECT, NistCsfFunction.RESPOND, NistCsfFunction.RECOVER),
                List.of(
                        versionSeed(1, "Incident Response v1", """
                                ## Purpose
                                Establish incident handling and communication standards.

                                ## SLAs
                                - Sev-1 acknowledgement within 15 minutes
                                - Sev-2 acknowledgement within 1 hour
                                """, PolicyVersionStatus.PUBLISHED),
                        versionSeed(2, "Incident Response v2 Draft", """
                                ## Draft Updates
                                - Added ransomware tabletop testing requirements
                                - Clarified evidence retention timelines
                                """, PolicyVersionStatus.DRAFT)
                )
        );
    }

    private void ensurePolicySeed(
            String code,
            String name,
            String description,
            User owner,
            List<NistCsfFunction> csfFunctions,
            List<PolicyVersionSeed> versions
    ) {
        Optional<Policy> existing = policyRepository.findByCode(code);
        if (existing.isPresent() && !refreshTestPolicies) {
            return;
        }
        Policy policy = existing.orElseGet(() -> Policy.builder()
                .code(code)
                .build());
        policy.setName(name);
        policy.setDescription(description);
        policy.setOwner(owner);
        policy.setStatus(PolicyStatus.DRAFT);
        policy.setPublishedVersion(null);
        policy = policyRepository.save(policy);

        PolicyVersion latestPublished = null;
        List<PolicyVersion> existingVersions = policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policy.getId());
        if (existing.isPresent() && refreshTestPolicies) {
            // Reset seeded policy versions so numbering and content always match seed definitions.
            policyAcknowledgementRepository.deleteByPolicyId(policy.getId());
            policyAcknowledgementRepository.flush();
            policyRevisionEventRepository.deleteByPolicyId(policy.getId());
            policyRevisionEventRepository.flush();
            policyVersionRepository.deleteByPolicyId(policy.getId());
            policyVersionRepository.flush();
            existingVersions = List.of();
        }
        final Policy policyRef = policy;
        for (PolicyVersionSeed seed : versions) {
            PolicyVersion row = existingVersions.stream()
                    .filter(v -> seed.versionNumber() == v.getVersionNumber())
                    .findFirst()
                    .orElseGet(() -> PolicyVersion.builder()
                            .policy(policyRef)
                            .versionNumber(seed.versionNumber())
                            .createdBy(owner)
                            .build());
            row.setPolicy(policy);
            row.setTitle(seed.title());
            row.setBodyMarkdown(seed.body());
            row.setStatus(seed.status());
            row.setCreatedBy(owner);
            row.setPublishedAt(seed.status() == PolicyVersionStatus.PUBLISHED
                    ? Instant.now().minus(seed.versionNumber(), ChronoUnit.DAYS)
                    : null);
            row = policyVersionRepository.save(row);
            if (seed.status() == PolicyVersionStatus.PUBLISHED) {
                if (latestPublished == null || row.getVersionNumber() > latestPublished.getVersionNumber()) {
                    latestPublished = row;
                }
            }
        }

        List<PolicyCsfMapping> existingMappings = policyCsfMappingRepository.findByPolicyIdOrderByIdAsc(policy.getId());
        Set<NistCsfFunction> desired = csfFunctions.stream().collect(Collectors.toSet());
        Set<NistCsfFunction> current = existingMappings.stream()
                .map(PolicyCsfMapping::getCsfFunction)
                .collect(Collectors.toSet());
        for (PolicyCsfMapping mapping : existingMappings) {
            if (!desired.contains(mapping.getCsfFunction())) {
                policyCsfMappingRepository.delete(mapping);
            }
        }
        for (NistCsfFunction function : desired) {
            if (!current.contains(function)) {
                policyCsfMappingRepository.save(PolicyCsfMapping.builder()
                        .policy(policy)
                        .csfFunction(function)
                        .build());
            }
        }

        if (latestPublished != null) {
            policy.setStatus(PolicyStatus.ACTIVE);
            policy.setPublishedVersion(latestPublished);
            policy.setEffectiveAt(latestPublished.getPublishedAt());
            policy.setNextReviewAt(Instant.now().plus(180, ChronoUnit.DAYS));
        } else {
            PolicyVersion latest = policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policy.getId()).stream()
                    .max(Comparator.comparing(PolicyVersion::getVersionNumber))
                    .orElse(null);
            policy.setStatus(PolicyStatus.DRAFT);
            policy.setPublishedVersion(null);
            policy.setEffectiveAt(null);
            policy.setNextReviewAt(latest != null ? Instant.now().plus(90, ChronoUnit.DAYS) : null);
        }
        policyRepository.save(policy);

        if (existing.isPresent()) {
            log.info("Refreshed sample policy {}", code);
        } else {
            log.info("Seeded sample policy {}", code);
        }
    }

    private User ensureInlineUser(String email, String rawPassword, String displayName, UserRole role) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .displayName(displayName)
                .role(role)
                .permissions(role.defaultPermissions())
                .build();
        return userRepository.save(user);
    }

    private static PolicyVersionSeed versionSeed(int versionNumber, String title, String body, PolicyVersionStatus status) {
        return new PolicyVersionSeed(versionNumber, title, body, status);
    }

    private record PolicyVersionSeed(int versionNumber, String title, String body, PolicyVersionStatus status) {}
}
