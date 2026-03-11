package com.cyberassessment.service;

import com.cyberassessment.dto.UserDto;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserPermission;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CurrentUserService currentUserService;
    @InjectMocks
    private UserService userService;

    @Test
    void createAndFindBranchesWork() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(currentUserService.isAuditManager()).thenReturn(true);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(userRepository.findAll()).thenReturn(List.of(
                User.builder().id(7L).email("new@test.com").displayName("New").role(UserRole.ADMIN)
                        .permissions(Set.of(UserPermission.USER_MANAGEMENT)).build()
        ));
        when(userRepository.findById(7L)).thenReturn(Optional.of(
                User.builder().id(7L).email("new@test.com").displayName("New").role(UserRole.ADMIN)
                        .permissions(Set.of(UserPermission.USER_MANAGEMENT)).build()
        ));
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(
                User.builder().id(7L).email("new@test.com").displayName("New").role(UserRole.ADMIN)
                        .permissions(Set.of(UserPermission.USER_MANAGEMENT)).build()
        ));

        UserDto created = userService.create("new@test.com", "pw", "New", UserRole.ADMIN, Set.of(UserPermission.USER_MANAGEMENT));
        assertThat(created.getId()).isEqualTo(7L);
        assertThat(userService.findAll()).hasSize(1);
        assertThat(userService.findById(7L).getEmail()).isEqualTo("new@test.com");
        assertThat(userService.findByRole(UserRole.ADMIN)).hasSize(1);
    }

    @Test
    void duplicateAndUpdateBranchesWork() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(currentUserService.isAuditManager()).thenReturn(true);
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);
        assertThatThrownBy(() -> userService.create("dup@test.com", "pw", "Dup", UserRole.APPLICATION_OWNER, null))
                .isInstanceOf(IllegalArgumentException.class);

        User existing = User.builder().id(2L).email("u@test.com").displayName("Old")
                .role(UserRole.APPLICATION_OWNER).permissions(UserRole.APPLICATION_OWNER.defaultPermissions()).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDto updated = userService.update(2L, null, null, "Updated", UserRole.ADMIN, Set.of(UserPermission.USER_MANAGEMENT));
        assertThat(updated.getDisplayName()).isEqualTo("Updated");
        assertThat(updated.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void nonAuditManagerCannotManageAuditors() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAuditManager()).thenReturn(false);
        assertThatThrownBy(() -> userService.create("auditor2@test.com", "pw", "Auditor2", UserRole.AUDITOR, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ADMIN or AUDIT_MANAGER");
    }

    @Test
    void nonAdminCannotAssignAuditManagerRole() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAuditManager()).thenReturn(true);
        assertThatThrownBy(() -> userService.create("manager2@test.com", "pw", "Manager2", UserRole.AUDIT_MANAGER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only ADMIN");
    }
}
