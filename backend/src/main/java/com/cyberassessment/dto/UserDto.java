package com.cyberassessment.dto;

import com.cyberassessment.entity.UserRole;
import com.cyberassessment.entity.UserPermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String displayName;
    private UserRole role;
    private Set<UserPermission> permissions;
}
