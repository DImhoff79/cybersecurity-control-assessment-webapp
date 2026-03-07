package com.cyberassessment.controller;

import com.cyberassessment.dto.UserDto;
import com.cyberassessment.service.CurrentUserService;
import com.cyberassessment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CurrentUserService currentUserService;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return currentUserService.getCurrentUser()
                .map(UserService::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
