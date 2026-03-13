package com.legal.notification.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_notifications")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(nullable = false)
    private String toEmail;

    private String ccEmails;
    private String bccEmails;

    @Column(nullable = false)
    private String fromEmail;

    @Column(nullable = false)
    private String fromName;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "LONGTEXT")
    private String htmlBody;

    @Column(columnDefinition = "LONGTEXT")
    private String textBody;

    private String templateName;
    private String messageId;
    private String sesRequestId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EmailStatus emailStatus = EmailStatus.QUEUED;

    private LocalDateTime sentAt;
    private String errorDetails;

    public enum EmailStatus { QUEUED, SENT, BOUNCED, COMPLAINED, DELIVERY_FAILED }
}
