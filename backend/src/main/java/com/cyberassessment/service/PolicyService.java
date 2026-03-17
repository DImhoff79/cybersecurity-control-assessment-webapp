package com.cyberassessment.service;

import com.cyberassessment.dto.PolicyAcknowledgementDto;
import com.cyberassessment.dto.PolicyDto;
import com.cyberassessment.dto.PolicyVersionDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.PolicyAcknowledgementRepository;
import com.cyberassessment.repository.PolicyRepository;
import com.cyberassessment.repository.PolicyVersionRepository;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository policyVersionRepository;
    private final PolicyAcknowledgementRepository policyAcknowledgementRepository;
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
                .userId(row.getUser().getId())
                .userEmail(row.getUser().getEmail())
                .status(deriveAcknowledgementStatus(row))
                .dueAt(row.getDueAt())
                .assignedAt(row.getAssignedAt())
                .acknowledgedAt(row.getAcknowledgedAt())
                .build();
    }

    public static PolicyDto toDto(Policy row, List<PolicyVersion> versions) {
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
                .versions((versions == null ? List.<PolicyVersion>of() : versions).stream()
                        .sorted(Comparator.comparing(PolicyVersion::getVersionNumber).reversed())
                        .map(PolicyService::toVersionDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PolicyDto> list() {
        return policyRepository.findAll().stream()
                .sorted(Comparator.comparing(Policy::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(policy -> toDto(policy, policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policy.getId())))
                .toList();
    }

    @Transactional
    public PolicyDto create(String code, String name, String description, Long ownerUserId, String initialVersionTitle, String initialBodyMarkdown) {
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
        createVersion(policy.getId(), title, body);
        policy = policyRepository.findById(policy.getId()).orElseThrow();
        return toDto(policy, policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policy.getId()));
    }

    @Transactional
    public PolicyDto update(Long policyId, String name, String description, Long ownerUserId, Instant nextReviewAt) {
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        if (name != null && !name.isBlank()) policy.setName(name.trim());
        if (description != null) policy.setDescription(description);
        if (ownerUserId != null) {
            User owner = userRepository.findById(ownerUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Owner user not found: " + ownerUserId));
            policy.setOwner(owner);
        }
        if (nextReviewAt != null) policy.setNextReviewAt(nextReviewAt);
        policy = policyRepository.save(policy);
        return toDto(policy, policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policy.getId()));
    }

    @Transactional
    public PolicyVersionDto createVersion(Long policyId, String title, String bodyMarkdown) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Version title is required");
        if (bodyMarkdown == null || bodyMarkdown.isBlank()) throw new IllegalArgumentException("Policy content is required");
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        int nextVersion = (int) policyVersionRepository.countByPolicyId(policyId) + 1;
        User actor = currentUserService.getCurrentUser().orElse(null);
        PolicyVersion created = policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .versionNumber(nextVersion)
                .title(title.trim())
                .bodyMarkdown(bodyMarkdown)
                .status(PolicyVersionStatus.DRAFT)
                .createdBy(actor)
                .build());
        return toVersionDto(created);
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

        return toDto(policy, policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policyId));
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
}
