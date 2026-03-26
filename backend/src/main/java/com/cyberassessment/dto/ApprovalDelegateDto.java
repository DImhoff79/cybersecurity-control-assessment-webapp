package com.cyberassessment.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApprovalDelegateDto {
    Long id;
    Long userId;
    String email;
    String displayName;
}
