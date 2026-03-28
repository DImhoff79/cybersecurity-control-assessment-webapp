package com.cyberassessment.security;

import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserPermission;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadsStoredPermissionsAsAuthorities() {
        User user = User.builder()
                .email("mgr@test.com")
                .passwordHash("hash")
                .role(UserRole.AUDIT_MANAGER)
                .permissions(EnumSet.of(UserPermission.AUDIT_MANAGEMENT, UserPermission.REPORT_VIEW))
                .build();
        when(userRepository.findByEmail("mgr@test.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("mgr@test.com");
        assertThat(details.getUsername()).isEqualTo("mgr@test.com");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(authStrings(details)).containsExactlyInAnyOrder(
                "ROLE_AUDIT_MANAGER",
                "PERM_AUDIT_MANAGEMENT",
                "PERM_REPORT_VIEW");
    }

    @Test
    void usesRoleDefaultPermissionsWhenNoneStored() {
        User user = User.builder()
                .email("aud@test.com")
                .passwordHash("h")
                .role(UserRole.AUDITOR)
                .permissions(Collections.emptySet())
                .build();
        when(userRepository.findByEmail("aud@test.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("aud@test.com");
        assertThat(authStrings(details)).contains("ROLE_AUDITOR");
        assertThat(authStrings(details)).contains("PERM_REPORT_VIEW");
        assertThat(authStrings(details)).anyMatch(a -> a.startsWith("PERM_"));
    }

    @Test
    void missingUserThrows() {
        when(userRepository.findByEmail("nope@test.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nope@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private static java.util.Set<String> authStrings(UserDetails details) {
        return details.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
