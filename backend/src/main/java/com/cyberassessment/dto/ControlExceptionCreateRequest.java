package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlExceptionCreateRequest {
    private Long auditId;
    private Long auditControlId;
    private String reason;
    private String compensatingControl;
    private Instant expiresAt;
}
