package com.cyberassessment.dto;

import com.cyberassessment.entity.ApplicationCriticality;
import com.cyberassessment.entity.ApplicationLifecycleStatus;
import com.cyberassessment.entity.ApplicationPurpose;
import com.cyberassessment.entity.DataClassification;
import com.cyberassessment.entity.HostingModel;
import com.cyberassessment.entity.IntegrationScope;
import com.cyberassessment.entity.UserAudienceScope;
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
    private ApplicationPurpose applicationPurpose;
    private HostingModel hostingModel;
    private UserAudienceScope userAudienceScope;
    private IntegrationScope integrationScope;
    /** Null = not answered / unsure */
    private Boolean dataScopePii;
    /** @deprecated Use {@link #dataScopeHipaa}; kept in JSON for older clients (same value as HIPAA). */
    private Boolean dataScopePhi;
    private Boolean dataScopePci;
    private Boolean dataScopeSox;
    private Boolean dataScopeHipaa;
    private Instant createdAt;
}
