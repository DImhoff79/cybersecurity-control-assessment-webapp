package com.cyberassessment.service;

import com.cyberassessment.dto.PolicyAcknowledgementDto;
import com.cyberassessment.dto.PolicyDto;
import com.cyberassessment.dto.PolicyRevisionEventDto;
import com.cyberassessment.dto.PolicyVersionDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.PolicyAcknowledgementRepository;
import com.cyberassessment.repository.PolicyCsfMappingRepository;
import com.cyberassessment.repository.PolicyRepository;
import com.cyberassessment.repository.PolicyRevisionEventRepository;
import com.cyberassessment.repository.PolicyVersionRepository;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository policyVersionRepository;
    private final PolicyAcknowledgementRepository policyAcknowledgementRepository;
    private final PolicyCsfMappingRepository policyCsfMappingRepository;
    private final PolicyRevisionEventRepository policyRevisionEventRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public static PolicyVersionDto toVersionDto(PolicyVersion row) {
        return PolicyVersionDto.builder()
                .id(row.getId())
                .versionNumber(row.getVersionNumber())
                .title(row.getTitle())
                .bodyMarkdown(row.getBodyMarkdown())
                .status(row.getStatus())
                .createdByUserId(row.getCreatedBy() != null ? row.getCreatedBy().getId() : null)
                .createdByEmail(row.getCreatedBy() != null ? row.getCreatedBy().getEmail() : null)
                .publishedAt(row.getPublishedAt())
                .createdAt(row.getCreatedAt())
                .build();
    }

    public static PolicyAcknowledgementDto toAcknowledgementDto(PolicyAcknowledgement row) {
        return PolicyAcknowledgementDto.builder()
                .id(row.getId())
                .policyId(row.getPolicy().getId())
                .policyCode(row.getPolicy().getCode())
                .policyName(row.getPolicy().getName())
                .policyVersionId(row.getPolicyVersion().getId())
                .policyVersionNumber(row.getPolicyVersion().getVersionNumber())
                .policyVersionTitle(row.getPolicyVersion().getTitle())
                .policyVersionBodyMarkdown(row.getPolicyVersion().getBodyMarkdown())
                .userId(row.getUser().getId())
                .userEmail(row.getUser().getEmail())
                .status(deriveAcknowledgementStatus(row))
                .dueAt(row.getDueAt())
                .assignedAt(row.getAssignedAt())
                .acknowledgedAt(row.getAcknowledgedAt())
                .build();
    }

    public static PolicyRevisionEventDto toRevisionEventDto(PolicyRevisionEvent row) {
        return PolicyRevisionEventDto.builder()
                .id(row.getId())
                .policyId(row.getPolicy().getId())
                .policyVersionId(row.getPolicyVersion() != null ? row.getPolicyVersion().getId() : null)
                .eventType(row.getEventType())
                .eventSummary(row.getEventSummary())
                .actorUserId(row.getActor() != null ? row.getActor().getId() : null)
                .actorEmail(row.getActor() != null ? row.getActor().getEmail() : null)
                .createdAt(row.getCreatedAt())
                .build();
    }

    public static PolicyDto toDto(
            Policy row,
            List<PolicyVersion> versions,
            List<PolicyCsfMapping> csfMappings,
            List<PolicyRevisionEvent> revisionEvents
    ) {
        return PolicyDto.builder()
                .id(row.getId())
                .code(row.getCode())
                .name(row.getName())
                .description(row.getDescription())
                .status(row.getStatus())
                .ownerUserId(row.getOwner() != null ? row.getOwner().getId() : null)
                .ownerEmail(row.getOwner() != null ? row.getOwner().getEmail() : null)
                .effectiveAt(row.getEffectiveAt())
                .nextReviewAt(row.getNextReviewAt())
                .publishedVersionId(row.getPublishedVersion() != null ? row.getPublishedVersion().getId() : null)
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .csfFunctions((csfMappings == null ? List.<PolicyCsfMapping>of() : csfMappings).stream()
                        .map(PolicyCsfMapping::getCsfFunction)
                        .distinct()
                        .toList())
                .versions((versions == null ? List.<PolicyVersion>of() : versions).stream()
                        .sorted(Comparator.comparing(PolicyVersion::getVersionNumber).reversed())
                        .map(PolicyService::toVersionDto)
                        .toList())
                .revisionHistory((revisionEvents == null ? List.<PolicyRevisionEvent>of() : revisionEvents).stream()
                        .map(PolicyService::toRevisionEventDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PolicyDto> list() {
        return policyRepository.findAll().stream()
                .sorted(Comparator.comparing(Policy::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(policy -> toDto(
                        policy,
                        policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policy.getId()),
                        policyCsfMappingRepository.findByPolicyIdOrderByIdAsc(policy.getId()),
                        policyRevisionEventRepository.findByPolicyIdOrderByCreatedAtDesc(policy.getId())
                ))
                .toList();
    }

    @Transactional
    public PolicyDto create(
            String code,
            String name,
            String description,
            Long ownerUserId,
            String initialVersionTitle,
            String initialBodyMarkdown,
            List<String> csfFunctions
    ) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Policy code is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Policy name is required");
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        if (policyRepository.findByCode(normalizedCode).isPresent()) {
            throw new IllegalArgumentException("Policy code already exists: " + normalizedCode);
        }
        User owner = ownerUserId == null ? null : userRepository.findById(ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Owner user not found: " + ownerUserId));
        Policy policy = policyRepository.save(Policy.builder()
                .code(normalizedCode)
                .name(name.trim())
                .description(description)
                .owner(owner)
                .status(PolicyStatus.DRAFT)
                .build());

        String title = (initialVersionTitle == null || initialVersionTitle.isBlank())
                ? policy.getName() + " - v1"
                : initialVersionTitle.trim();
        String body = (initialBodyMarkdown == null || initialBodyMarkdown.isBlank())
                ? "## Scope\n\nDescribe scope.\n\n## Requirements\n\nDescribe requirements."
                : initialBodyMarkdown;
        PolicyVersionDto initialVersion = createVersion(policy.getId(), title, body);
        replaceCsfMappings(policy.getId(), csfFunctions);
        logRevisionEvent(
                policy,
                initialVersion.getId(),
                PolicyRevisionEventType.POLICY_CREATED,
                "Policy created with initial version " + initialVersion.getVersionNumber()
        );
        policy = policyRepository.findById(policy.getId()).orElseThrow();
        return toDto(
                policy,
                policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policy.getId()),
                policyCsfMappingRepository.findByPolicyIdOrderByIdAsc(policy.getId()),
                policyRevisionEventRepository.findByPolicyIdOrderByCreatedAtDesc(policy.getId())
        );
    }

    @Transactional
    public PolicyDto update(Long policyId, String name, String description, Long ownerUserId, Instant nextReviewAt, List<String> csfFunctions) {
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        StringBuilder changes = new StringBuilder();
        if (name != null && !name.isBlank()) policy.setName(name.trim());
        if (name != null && !name.isBlank()) changes.append("name, ");
        if (description != null) {
            policy.setDescription(description);
            changes.append("description, ");
        }
        if (ownerUserId != null) {
            User owner = userRepository.findById(ownerUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Owner user not found: " + ownerUserId));
            policy.setOwner(owner);
            changes.append("owner, ");
        }
        if (nextReviewAt != null) {
            policy.setNextReviewAt(nextReviewAt);
            changes.append("next review date, ");
        }
        policy = policyRepository.save(policy);
        if (csfFunctions != null) {
            replaceCsfMappings(policyId, csfFunctions);
            changes.append("CSF mapping, ");
        }
        if (changes.length() > 0) {
            logRevisionEvent(policy, null, PolicyRevisionEventType.POLICY_UPDATED, "Updated " + changes.substring(0, changes.length() - 2));
        }
        return toDto(
                policy,
                policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policy.getId()),
                policyCsfMappingRepository.findByPolicyIdOrderByIdAsc(policy.getId()),
                policyRevisionEventRepository.findByPolicyIdOrderByCreatedAtDesc(policy.getId())
        );
    }

    @Transactional
    public PolicyVersionDto createVersion(Long policyId, String title, String bodyMarkdown) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Version title is required");
        if (bodyMarkdown == null || bodyMarkdown.isBlank()) throw new IllegalArgumentException("Policy content is required");
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        int nextVersion = policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policyId).stream()
                .map(PolicyVersion::getVersionNumber)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        User actor = currentUserService.getCurrentUser().orElse(null);
        PolicyVersion created = policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .versionNumber(nextVersion)
                .title(title.trim())
                .bodyMarkdown(bodyMarkdown)
                .status(PolicyVersionStatus.DRAFT)
                .createdBy(actor)
                .build());
        logRevisionEvent(policy, created.getId(), PolicyRevisionEventType.VERSION_CREATED, "Created policy version " + created.getVersionNumber());
        return toVersionDto(created);
    }

    @Transactional
    public PolicyVersionDto updateVersion(Long policyId, Long policyVersionId, String title, String bodyMarkdown) {
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        PolicyVersion version = policyVersionRepository.findById(policyVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Policy version not found: " + policyVersionId));
        if (!version.getPolicy().getId().equals(policyId)) {
            throw new IllegalArgumentException("Version does not belong to policy");
        }
        if (version.getStatus() != PolicyVersionStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft versions can be edited");
        }
        boolean changed = false;
        if (title != null && !title.isBlank() && !title.trim().equals(version.getTitle())) {
            version.setTitle(title.trim());
            changed = true;
        }
        if (bodyMarkdown != null && !bodyMarkdown.isBlank() && !bodyMarkdown.equals(version.getBodyMarkdown())) {
            version.setBodyMarkdown(bodyMarkdown);
            changed = true;
        }
        version = policyVersionRepository.save(version);
        if (changed) {
            logRevisionEvent(policy, version.getId(), PolicyRevisionEventType.VERSION_UPDATED, "Updated policy version " + version.getVersionNumber());
        }
        return toVersionDto(version);
    }

    @Transactional
    public PolicyDto publish(Long policyId, Long policyVersionId, Instant dueAt) {
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        PolicyVersion version = policyVersionRepository.findById(policyVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Policy version not found: " + policyVersionId));
        if (!version.getPolicy().getId().equals(policy.getId())) {
            throw new IllegalArgumentException("Version does not belong to policy");
        }

        List<PolicyVersion> published = policyVersionRepository.findByPolicyIdAndStatus(policyId, PolicyVersionStatus.PUBLISHED);
        for (PolicyVersion row : published) {
            row.setStatus(PolicyVersionStatus.RETIRED);
            policyVersionRepository.save(row);
        }

        version.setStatus(PolicyVersionStatus.PUBLISHED);
        version.setPublishedAt(Instant.now());
        policyVersionRepository.save(version);

        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setPublishedVersion(version);
        policy.setEffectiveAt(Instant.now());
        if (dueAt != null) {
            policy.setNextReviewAt(dueAt);
        }
        policyRepository.save(policy);
        logRevisionEvent(policy, version.getId(), PolicyRevisionEventType.VERSION_PUBLISHED, "Published policy version " + version.getVersionNumber());

        List<User> attestors = userRepository.findByRole(UserRole.APPLICATION_OWNER);
        for (User user : attestors) {
            policyAcknowledgementRepository.save(PolicyAcknowledgement.builder()
                    .policy(policy)
                    .policyVersion(version)
                    .user(user)
                    .status(PolicyAcknowledgementStatus.PENDING)
                    .dueAt(dueAt)
                    .build());
        }

        return toDto(
                policy,
                policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policyId),
                policyCsfMappingRepository.findByPolicyIdOrderByIdAsc(policyId),
                policyRevisionEventRepository.findByPolicyIdOrderByCreatedAtDesc(policyId)
        );
    }

    @Transactional(readOnly = true)
    public List<NistCsfFunction> getCsfMappings(Long policyId) {
        ensurePolicyExists(policyId);
        return policyCsfMappingRepository.findByPolicyIdOrderByIdAsc(policyId).stream()
                .map(PolicyCsfMapping::getCsfFunction)
                .distinct()
                .toList();
    }

    @Transactional
    public List<NistCsfFunction> updateCsfMappings(Long policyId, List<String> csfFunctions) {
        Policy policy = ensurePolicyExists(policyId);
        List<NistCsfFunction> updated = replaceCsfMappings(policyId, csfFunctions);
        logRevisionEvent(policy, null, PolicyRevisionEventType.CSF_MAPPING_UPDATED, "Updated CSF mapping to " + updated);
        return updated;
    }

    @Transactional(readOnly = true)
    public List<PolicyRevisionEventDto> getRevisionHistory(Long policyId) {
        ensurePolicyExists(policyId);
        return policyRevisionEventRepository.findByPolicyIdOrderByCreatedAtDesc(policyId).stream()
                .map(PolicyService::toRevisionEventDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PolicyAcknowledgementDto> listAcknowledgements(Long policyId) {
        List<PolicyAcknowledgement> rows = policyId == null
                ? policyAcknowledgementRepository.findAll()
                : policyAcknowledgementRepository.findByPolicyIdOrderByAssignedAtDesc(policyId);
        return rows.stream()
                .sorted(Comparator.comparing(PolicyAcknowledgement::getAssignedAt).reversed())
                .map(PolicyService::toAcknowledgementDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PolicyAcknowledgementDto> myAcknowledgements() {
        User user = currentUserService.getCurrentUserOrThrow();
        return policyAcknowledgementRepository.findByUserIdOrderByAssignedAtDesc(user.getId())
                .stream()
                .map(PolicyService::toAcknowledgementDto)
                .toList();
    }

    @Transactional
    public PolicyAcknowledgementDto acknowledge(Long acknowledgementId) {
        User user = currentUserService.getCurrentUserOrThrow();
        PolicyAcknowledgement row = policyAcknowledgementRepository.findByIdAndUserId(acknowledgementId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Policy acknowledgement not found"));
        row.setStatus(PolicyAcknowledgementStatus.ACKNOWLEDGED);
        row.setAcknowledgedAt(Instant.now());
        row = policyAcknowledgementRepository.save(row);
        return toAcknowledgementDto(row);
    }

    @Transactional(readOnly = true)
    public double attestationCompletionPct() {
        long total = policyAcknowledgementRepository.count();
        if (total == 0) return 0.0;
        long complete = policyAcknowledgementRepository.countByStatus(PolicyAcknowledgementStatus.ACKNOWLEDGED);
        return BigDecimal.valueOf((complete * 100.0) / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static PolicyAcknowledgementStatus deriveAcknowledgementStatus(PolicyAcknowledgement row) {
        if (row.getStatus() == PolicyAcknowledgementStatus.ACKNOWLEDGED) return PolicyAcknowledgementStatus.ACKNOWLEDGED;
        if (row.getDueAt() != null && row.getDueAt().isBefore(Instant.now())) return PolicyAcknowledgementStatus.OVERDUE;
        return PolicyAcknowledgementStatus.PENDING;
    }

    private Policy ensurePolicyExists(Long policyId) {
        return policyRepository.findById(policyId).orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
    }

    private List<NistCsfFunction> replaceCsfMappings(Long policyId, List<String> csfFunctions) {
        policyCsfMappingRepository.deleteByPolicyId(policyId);
        Policy policy = ensurePolicyExists(policyId);
        Set<NistCsfFunction> normalized = new LinkedHashSet<>();
        if (csfFunctions != null) {
            for (String function : csfFunctions) {
                if (function == null || function.isBlank()) continue;
                normalized.add(NistCsfFunction.valueOf(function.trim().toUpperCase(Locale.ROOT)));
            }
        }
        normalized.forEach(function -> policyCsfMappingRepository.save(PolicyCsfMapping.builder()
                .policy(policy)
                .csfFunction(function)
                .build()));
        return normalized.stream().toList();
    }

    private void logRevisionEvent(Policy policy, Long policyVersionId, PolicyRevisionEventType type, String summary) {
        User actor = currentUserService.getCurrentUser().orElse(null);
        PolicyVersion version = policyVersionId == null ? null : policyVersionRepository.findById(policyVersionId).orElse(null);
        policyRevisionEventRepository.save(PolicyRevisionEvent.builder()
                .policy(policy)
                .policyVersion(version)
                .eventType(type)
                .eventSummary(summary)
                .actor(actor)
                .build());
    }
}
