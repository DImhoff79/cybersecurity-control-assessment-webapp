package com.cyberassessment.service;

import com.cyberassessment.dto.UserDto;
import com.cyberassessment.entity.User;
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
    @InjectMocks
    private UserService userService;

    @Test
    void createAndFindBranchesWork() {
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(userRepository.findAll()).thenReturn(List.of(
                User.builder().id(7L).email("new@test.com").displayName("New").role(UserRole.ADMIN).build()
        ));
        when(userRepository.findById(7L)).thenReturn(Optional.of(
                User.builder().id(7L).email("new@test.com").displayName("New").role(UserRole.ADMIN).build()
        ));
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(
                User.builder().id(7L).email("new@test.com").displayName("New").role(UserRole.ADMIN).build()
        ));

        UserDto created = userService.create("new@test.com", "pw", "New", UserRole.ADMIN);
        assertThat(created.getId()).isEqualTo(7L);
        assertThat(userService.findAll()).hasSize(1);
        assertThat(userService.findById(7L).getEmail()).isEqualTo("new@test.com");
        assertThat(userService.findByRole(UserRole.ADMIN)).hasSize(1);
    }

    @Test
    void duplicateAndUpdateBranchesWork() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);
        assertThatThrownBy(() -> userService.create("dup@test.com", "pw", "Dup", UserRole.APPLICATION_OWNER))
                .isInstanceOf(IllegalArgumentException.class);

        User existing = User.builder().id(2L).email("u@test.com").displayName("Old").role(UserRole.APPLICATION_OWNER).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDto updated = userService.update(2L, "Updated", UserRole.ADMIN);
        assertThat(updated.getDisplayName()).isEqualTo("Updated");
        assertThat(updated.getRole()).isEqualTo(UserRole.ADMIN);
    }
}
