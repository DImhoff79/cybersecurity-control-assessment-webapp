package com.cyberassessment.service;

import com.cyberassessment.dto.AccessRequestDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.AccessRequestRepository;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessRequestService {
    private final AccessRequestRepository accessRequestRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ObjectProvider<PasswordEncoder> passwordEncoderProvider;

    public static AccessRequestDto toDto(AccessRequest request) {
        User decider = request.getDecidedBy();
        return AccessRequestDto.builder()
                .id(request.getId())
                .email(request.getEmail())
                .displayName(request.getDisplayName())
                .provider(request.getProvider())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .decidedAt(request.getDecidedAt())
                .decidedByUserId(decider != null ? decider.getId() : null)
                .decidedByEmail(decider != null ? decider.getEmail() : null)
                .decisionNotes(request.getDecisionNotes())
                .build();
    }

    public record SocialAuthResult(boolean approved, boolean newlyRequested, String message) {}

    @Transactional
    public SocialAuthResult processSocialSignIn(IdentityProvider provider, String providerSubject, String email, String displayName) {
        if (providerSubject == null || providerSubject.isBlank()) {
            return new SocialAuthResult(false, false, "Identity provider did not return a stable user id.");
        }
        if (email == null || email.isBlank()) {
            return new SocialAuthResult(false, false, "Identity provider did not return an email address.");
        }
        email = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            return new SocialAuthResult(true, false, "Approved");
        }

        AccessRequest existing = accessRequestRepository.findByProviderAndProviderSubject(provider, providerSubject).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == AccessRequestStatus.APPROVED) {
                return new SocialAuthResult(true, false, "Approved");
            }
            if (existing.getStatus() == AccessRequestStatus.PENDING) {
                return new SocialAuthResult(false, false, "Your access request is still pending admin approval.");
            }
            existing.setStatus(AccessRequestStatus.PENDING);
            existing.setDecisionNotes("Reopened by new social sign-in attempt.");
            existing.setDecidedAt(null);
            existing.setDecidedBy(null);
            existing.setEmail(email);
            existing.setDisplayName(displayName);
            accessRequestRepository.save(existing);
            return new SocialAuthResult(false, true, "Access request submitted and pending approval.");
        }

        AccessRequest created = AccessRequest.builder()
                .email(email)
                .displayName(displayName)
                .provider(provider)
                .providerSubject(providerSubject)
                .status(AccessRequestStatus.PENDING)
                .build();
        accessRequestRepository.save(created);
        return new SocialAuthResult(false, true, "Access request submitted and pending approval.");
    }

    @Transactional(readOnly = true)
    public List<AccessRequestDto> listPending() {
        if (!currentUserService.hasPermission(UserPermission.USER_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: USER_MANAGEMENT");
        }
        return accessRequestRepository.findByStatusOrderByRequestedAtDesc(AccessRequestStatus.PENDING)
                .stream().map(AccessRequestService::toDto).toList();
    }

    @Transactional
    public AccessRequestDto approve(Long requestId, UserRole role, String notes) {
        if (!currentUserService.hasPermission(UserPermission.USER_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: USER_MANAGEMENT");
        }
        if (role == UserRole.AUDIT_MANAGER && !currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only ADMIN can assign AUDIT_MANAGER role");
        }
        if (role == UserRole.AUDITOR && !(currentUserService.isAdmin() || currentUserService.isAuditManager())) {
            throw new IllegalArgumentException("Only ADMIN or AUDIT_MANAGER can assign AUDITOR role");
        }
        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Access request not found"));
        if (request.getStatus() != AccessRequestStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be approved");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            UserRole effectiveRole = role != null ? role : UserRole.APPLICATION_OWNER;
            user = userRepository.save(User.builder()
                    .email(request.getEmail())
                    .passwordHash(passwordEncoderProvider.getObject().encode(UUID.randomUUID().toString()))
                    .displayName(request.getDisplayName())
                    .role(effectiveRole)
                    .permissions(effectiveRole.defaultPermissions())
                    .build());
        }

        request.setStatus(AccessRequestStatus.APPROVED);
        request.setDecidedAt(Instant.now());
        request.setDecidedBy(currentUserService.getCurrentUser().orElse(null));
        request.setDecisionNotes(notes);
        request = accessRequestRepository.save(request);
        return toDto(request);
    }

    @Transactional
    public AccessRequestDto reject(Long requestId, String notes) {
        if (!currentUserService.hasPermission(UserPermission.USER_MANAGEMENT)) {
            throw new IllegalArgumentException("Missing permission: USER_MANAGEMENT");
        }
        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Access request not found"));
        if (request.getStatus() != AccessRequestStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be rejected");
        }
        request.setStatus(AccessRequestStatus.REJECTED);
        request.setDecidedAt(Instant.now());
        request.setDecidedBy(currentUserService.getCurrentUser().orElse(null));
        request.setDecisionNotes(notes);
        request = accessRequestRepository.save(request);
        return toDto(request);
    }
}
