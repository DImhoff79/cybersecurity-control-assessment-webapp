package com.cyberassessment.dto;

import com.cyberassessment.entity.PolicyVersionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyVersionDto {
    private Long id;
    private Integer versionNumber;
    private String title;
    private String bodyMarkdown;
    private PolicyVersionStatus status;
    private Long createdByUserId;
    private String createdByEmail;
    private Instant publishedAt;
    private Instant createdAt;
}
