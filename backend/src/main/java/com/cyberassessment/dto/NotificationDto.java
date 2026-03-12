package com.cyberassessment.dto;

import com.cyberassessment.entity.NotificationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private NotificationCategory category;
    private String title;
    private String message;
    private Long auditId;
    private Instant readAt;
    private Instant createdAt;
}
