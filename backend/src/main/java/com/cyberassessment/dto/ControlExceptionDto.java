package com.cyberassessment.dto;

import com.cyberassessment.entity.ControlExceptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlExceptionDto {
    private Long id;
    private Long auditId;
    private Integer auditYear;
    private String applicationName;
    private Long auditControlId;
    private Long findingId;
    private String findingTitle;
    private String controlId;
    private String controlName;
    private ControlExceptionStatus status;
    private String reason;
    private String compensatingControl;
    private String decisionNotes;
    private Long requestedByUserId;
    private String requestedByEmail;
    private String requestedByDisplayName;
    private Long decidedByUserId;
    private String decidedByEmail;
    private String decidedByDisplayName;
    private Instant requestedAt;
    private Instant decidedAt;
    private Instant expiresAt;
    private String slaState;
}
