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
public class ApplicationDto {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerEmail;
    private String ownerDisplayName;
    private Instant createdAt;
}
