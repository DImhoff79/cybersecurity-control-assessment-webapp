package com.cyberassessment.security;

import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserPermission;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Adds {@code ROLE_*} and {@code PERM_*} from the local {@link User} so {@code @PreAuthorize}
 * matches database permissions (same as {@link CustomUserDetailsService} for basic auth).
 */
@Component
@RequiredArgsConstructor
public class AppOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Object rawEmail = oauth2User.getAttributes().get("email");
        if (!(rawEmail instanceof String email) || email.isBlank()) {
            return oauth2User;
        }
        Optional<User> local = userRepository.findByEmail(email.toLowerCase(Locale.ROOT));
        if (local.isEmpty()) {
            return oauth2User;
        }
        User user = local.get();
        Set<String> seen = new LinkedHashSet<>();
        List<GrantedAuthority> merged = new ArrayList<>();
        for (GrantedAuthority a : oauth2User.getAuthorities()) {
            if (seen.add(a.getAuthority())) {
                merged.add(a);
            }
        }
        addIfNew(seen, merged, "ROLE_" + user.getRole().name());
        Set<UserPermission> stored = user.getPermissions();
        Set<UserPermission> effective =
                stored != null && !stored.isEmpty() ? stored : user.getRole().defaultPermissions();
        for (UserPermission p : effective) {
            addIfNew(seen, merged, "PERM_" + p.name());
        }
        String nameKey = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        return new DefaultOAuth2User(merged, oauth2User.getAttributes(), nameKey);
    }

    private static void addIfNew(Set<String> seen, List<GrantedAuthority> merged, String authority) {
        if (seen.add(authority)) {
            merged.add(new SimpleGrantedAuthority(authority));
        }
    }
}
