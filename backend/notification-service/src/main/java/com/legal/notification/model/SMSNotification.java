package com.legal.notification.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_notifications")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SMSNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sms_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(nullable = false)
    private String toPhoneNumber;

    @Column(nullable = false)
    private String fromPhoneNumber;

    @Column(nullable = false, length = 1600)
    private String messageBody;

    private String twilioSid;
    private String twilioStatus;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SMSStatus smsStatus = SMSStatus.QUEUED;

    private LocalDateTime sentAt;
    private String errorDetails;

    public enum SMSStatus { QUEUED, SENT, DELIVERED, FAILED, UNDELIVERED }
}
