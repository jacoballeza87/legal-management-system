package com.legal.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_user",   columnList = "recipientUserId"),
    @Index(name = "idx_notif_status", columnList = "status"),
    @Index(name = "idx_notif_entity", columnList = "entityType, entityId")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(nullable = false)
    private Long recipientUserId;

    @Column(nullable = false, length = 100)
    private String recipientEmail;

    private String recipientPhone;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    private NotificationPriority priority;

    private String entityType;
    private Long entityId;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Builder.Default
    private Integer retryCount = 0;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum NotificationType  { EMAIL, SMS, IN_APP, PUSH }
    public enum NotificationStatus { PENDING, SENT, FAILED, READ }
    public enum NotificationPriority { LOW, MEDIUM, HIGH, CRITICAL }
}
