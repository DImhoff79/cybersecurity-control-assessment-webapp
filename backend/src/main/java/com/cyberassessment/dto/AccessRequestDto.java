package com.cyberassessment.dto;

import com.cyberassessment.entity.AccessRequestStatus;
import com.cyberassessment.entity.IdentityProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessRequestDto {
    private Long id;
    private String email;
    private String displayName;
    private IdentityProvider provider;
    private AccessRequestStatus status;
    private Instant requestedAt;
    private Instant decidedAt;
    private Long decidedByUserId;
    private String decidedByEmail;
    private String decisionNotes;
}
