package com.cyberassessment.service;

import com.cyberassessment.dto.UserDto;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserPermission;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public static UserDto toDto(User u) {
        if (u == null) return null;
        return UserDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .displayName(u.getDisplayName())
                .role(u.getRole())
                .permissions(u.getPermissions())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream().map(UserService::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return userRepository.findById(id).map(UserService::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UserDto> findByRole(UserRole role) {
        return userRepository.findByRole(role).stream().map(UserService::toDto).collect(Collectors.toList());
    }

    @Transactional
    public UserDto create(String email, String plainPassword, String displayName, UserRole role, Set<UserPermission> permissions) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        UserRole effectiveRole = role != null ? role : UserRole.APPLICATION_OWNER;
        ensureRoleAssignmentAllowed(null, effectiveRole);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("User already exists with email: " + normalizedEmail);
        }
        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(plainPassword))
                .displayName(displayName)
                .role(effectiveRole)
                .permissions(resolvePermissions(effectiveRole, permissions))
                .build();
        user = userRepository.save(user);
        return toDto(user);
    }

    @Transactional
    public UserDto update(Long id, String email, String plainPassword, String displayName, UserRole role, Set<UserPermission> permissions) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        ensureRoleAssignmentAllowed(user, role);
        if (email != null) {
            String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
            if (normalizedEmail.isBlank()) {
                throw new IllegalArgumentException("Email cannot be blank");
            }
            if (!normalizedEmail.equals(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
                throw new IllegalArgumentException("User already exists with email: " + normalizedEmail);
            }
            user.setEmail(normalizedEmail);
        }
        if (plainPassword != null && !plainPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(plainPassword));
        }
        if (displayName != null) user.setDisplayName(displayName);
        if (role != null) {
            user.setRole(role);
            if (permissions == null) {
                user.setPermissions(resolvePermissions(role, null));
            }
        }
        if (permissions != null) {
            user.setPermissions(resolvePermissions(user.getRole(), permissions));
        }
        user = userRepository.save(user);
        return toDto(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        ensureRoleAssignmentAllowed(user, null);
        userRepository.delete(user);
    }

    private Set<UserPermission> resolvePermissions(UserRole role, Set<UserPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new HashSet<>(role.defaultPermissions());
        }
        return new HashSet<>(permissions);
    }

    private void ensureRoleAssignmentAllowed(User existingUser, UserRole targetRole) {
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAuditManager = currentUserService.isAuditManager();

        boolean assignsAuditManager = targetRole == UserRole.AUDIT_MANAGER
                && (existingUser == null || existingUser.getRole() != UserRole.AUDIT_MANAGER);
        if (assignsAuditManager && !isAdmin) {
            throw new IllegalArgumentException("Only ADMIN can assign AUDIT_MANAGER role");
        }

        boolean touchesAuditor = (existingUser != null && existingUser.getRole() == UserRole.AUDITOR)
                || targetRole == UserRole.AUDITOR;
        if (touchesAuditor && !(isAdmin || isAuditManager)) {
            throw new IllegalArgumentException("Only ADMIN or AUDIT_MANAGER can assign or manage auditors");
        }
    }
}
