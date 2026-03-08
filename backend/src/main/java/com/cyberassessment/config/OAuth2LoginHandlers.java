package com.cyberassessment.config;

import com.cyberassessment.entity.IdentityProvider;
import com.cyberassessment.service.AccessRequestService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    private final AccessRequestService accessRequestService;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            response.sendRedirect(frontendBaseUrl + "/login?oauth=error&reason=invalid_oauth_token");
            return;
        }
        String providerId = token.getAuthorizedClientRegistrationId();
        OAuth2User user = token.getPrincipal();
        Map<String, Object> attrs = user.getAttributes();

        IdentityProvider provider = mapProvider(providerId);
        String subject = firstString(attrs, "sub", "id");
        String email = firstString(attrs, "email");
        String name = firstString(attrs, "name", "displayName");

        AccessRequestService.SocialAuthResult result = accessRequestService.processSocialSignIn(provider, subject, email, name);
        if (result.approved()) {
            response.sendRedirect(frontendBaseUrl + "/login?oauth=success&provider=" + provider.name().toLowerCase(Locale.ROOT));
            return;
        }
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        response.sendRedirect(frontendBaseUrl + "/login?oauth=pending&provider=" + provider.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException exception) throws IOException, ServletException {
        String target = UriComponentsBuilder.fromHttpUrl(frontendBaseUrl + "/login")
                .queryParam("oauth", "error")
                .queryParam("reason", "oauth_failure")
                .build().toUriString();
        response.sendRedirect(target);
    }

    private static IdentityProvider mapProvider(String providerId) {
        return "facebook".equalsIgnoreCase(providerId) ? IdentityProvider.FACEBOOK : IdentityProvider.GOOGLE;
    }

    private static String firstString(Map<String, Object> attrs, String... keys) {
        for (String key : keys) {
            Object value = attrs.get(key);
            if (value instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }
}
