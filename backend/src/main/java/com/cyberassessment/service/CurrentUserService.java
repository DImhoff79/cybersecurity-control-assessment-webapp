package com.cyberassessment.service;

import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserPermission;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        String email = null;
        if (auth.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal) {
            Object raw = principal.getAttributes().get("email");
            if (raw instanceof String s) {
                email = s;
            }
        }
        if (email == null || email.isBlank()) {
            email = auth.getName();
        }
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        email = email.toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(email);
    }

    public User getCurrentUserOrThrow() {
        return getCurrentUser().orElseThrow(() -> new IllegalStateException("Not authenticated"));
    }

    public boolean isAdmin() {
        return getCurrentUser()
                .map(u -> u.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    public boolean isAuditManager() {
        return getCurrentUser()
                .map(u -> u.getRole() == UserRole.AUDIT_MANAGER)
                .orElse(false);
    }

    public boolean isAdminOrAuditManager() {
        return getCurrentUser()
                .map(u -> u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.AUDIT_MANAGER)
                .orElse(false);
    }

    public boolean hasPermission(UserPermission permission) {
        if (permission == null) {
            return false;
        }
        return getCurrentUser()
                .map(u -> u.getPermissions() != null && u.getPermissions().contains(permission))
                .orElse(false);
    }
}
