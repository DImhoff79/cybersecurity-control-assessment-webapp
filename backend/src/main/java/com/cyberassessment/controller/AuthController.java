package com.cyberassessment.controller;

import com.cyberassessment.dto.UserDto;
import com.cyberassessment.service.CurrentUserService;
import com.cyberassessment.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final org.springframework.beans.factory.ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;
    @Value("${app.auth.allow-basic:true}")
    private boolean allowBasicAuth;
    @Value("${app.auth.mode:mixed}")
    private String authMode;

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return currentUserService.getCurrentUser()
                .map(UserService::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/providers")
    public Map<String, Object> providers() {
        Set<String> registered = new HashSet<>();
        ClientRegistrationRepository repo = clientRegistrationRepositoryProvider.getIfAvailable();
        if (repo instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof ClientRegistration registration) {
                    registered.add(registration.getRegistrationId());
                }
            }
        }
        return Map.of(
                "authMode", authMode,
                "basic", Map.of(
                        "enabled", allowBasicAuth
                ),
                "google", Map.of(
                        "enabled", registered.contains("google"),
                        "url", "/oauth2/authorization/google"
                ),
                "facebook", Map.of(
                        "enabled", registered.contains("facebook"),
                        "url", "/oauth2/authorization/facebook"
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return ResponseEntity.noContent().build();
    }
}
