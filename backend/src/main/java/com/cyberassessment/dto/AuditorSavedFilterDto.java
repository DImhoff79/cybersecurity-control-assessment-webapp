package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditorSavedFilterDto {
    private Long id;
    private String name;
    private Boolean shared;
    private Long createdByUserId;
    private String createdByEmail;
    private Instant createdAt;
    private Map<String, Object> filterState;
}
