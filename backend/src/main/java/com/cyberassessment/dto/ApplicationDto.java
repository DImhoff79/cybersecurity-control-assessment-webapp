package com.cyberassessment.dto;

import com.cyberassessment.entity.ApplicationCriticality;
import com.cyberassessment.entity.ApplicationLifecycleStatus;
import com.cyberassessment.entity.DataClassification;
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
    private ApplicationCriticality criticality;
    private DataClassification dataClassification;
    private String regulatoryScope;
    private String businessOwnerName;
    private String technicalOwnerName;
    private ApplicationLifecycleStatus lifecycleStatus;
    private Instant createdAt;
}
