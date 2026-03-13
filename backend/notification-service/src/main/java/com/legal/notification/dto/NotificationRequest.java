package com.legal.notification.dto;

import com.legal.notification.model.Notification.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationRequest {

    @NotNull
    private Long recipientUserId;

    @NotBlank @Email
    private String recipientEmail;

    private String recipientPhone;

    @NotBlank @Size(max = 200)
    private String subject;

    @NotBlank
    private String body;

    @NotNull
    private NotificationType type;

    private NotificationPriority priority;
    private String entityType;
    private Long entityId;
    private String eventType;
    private String templateName;
    private Map<String, Object> templateData;
}
