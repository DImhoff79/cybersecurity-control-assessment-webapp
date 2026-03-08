package com.cyberassessment.dto;

import com.cyberassessment.entity.QuestionnaireTemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireTemplateDto {
    private Long id;
    private Integer versionNo;
    private QuestionnaireTemplateStatus status;
    private String notes;
    private Instant createdAt;
    private Instant publishedAt;
    private Long createdByUserId;
    private String createdByEmail;
    private Long publishedByUserId;
    private String publishedByEmail;
    private Integer itemCount;
}
