package com.cyberassessment.service;

import com.cyberassessment.dto.UserDto;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserRole;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public static UserDto toDto(User u) {
        if (u == null) return null;
        return UserDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .displayName(u.getDisplayName())
                .role(u.getRole())
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
    public UserDto create(String email, String plainPassword, String displayName, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User already exists with email: " + email);
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(plainPassword))
                .displayName(displayName)
                .role(role)
                .build();
        user = userRepository.save(user);
        return toDto(user);
    }

    @Transactional
    public UserDto update(Long id, String displayName, UserRole role) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (displayName != null) user.setDisplayName(displayName);
        if (role != null) user.setRole(role);
        user = userRepository.save(user);
        return toDto(user);
    }
}
