package com.legal.notification.dto;

import com.legal.notification.model.Notification.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long recipientUserId;
    private String recipientEmail;
    private String subject;
    private NotificationType type;
    private NotificationStatus status;
    private NotificationPriority priority;
    private String entityType;
    private Long entityId;
    private String eventType;
    private Integer retryCount;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
