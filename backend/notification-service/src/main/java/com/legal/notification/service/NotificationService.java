package com.legal.notification.service;

import com.legal.notification.dto.NotificationRequest;
import com.legal.notification.dto.NotificationResponse;
import com.legal.notification.mapper.NotificationMapper;
import com.legal.notification.model.Notification;
import com.legal.notification.model.Notification.*;
import com.legal.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final EmailService emailService;
    private final SMSService smsService;
    private final NotificationMapper mapper;

    @Async("taskExecutor")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public void sendNotification(NotificationRequest request) {
        Notification notification = Notification.builder()
            .recipientUserId(request.getRecipientUserId())
            .recipientEmail(request.getRecipientEmail())
            .recipientPhone(request.getRecipientPhone())
            .subject(request.getSubject())
            .body(request.getBody())
            .type(request.getType())
            .priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.MEDIUM)
            .entityType(request.getEntityType())
            .entityId(request.getEntityId())
            .eventType(request.getEventType())
            .status(NotificationStatus.PENDING)
            .build();

        notification = notificationRepo.save(notification);

        try {
            switch (request.getType()) {
                case EMAIL -> {
                    String html;
                    if (request.getTemplateName() != null) {
                        Map<String, Object> data = request.getTemplateData() != null
                            ? request.getTemplateData()
                            : Map.of("subject", request.getSubject(), "body", request.getBody());
                        html = emailService.renderTemplate(request.getTemplateName(), data);
                    } else {
                        html = emailService.buildDefaultHtml(request.getSubject(), request.getBody());
                    }
                    emailService.sendEmail(notification, html, request.getBody());
                }
                case SMS -> smsService.sendSMS(notification);
                case IN_APP -> log.info("Notificación IN_APP almacenada para userId={}", request.getRecipientUserId());
                default  -> log.warn("Tipo no implementado: {}", request.getType());
            }
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error procesando notificación id={}: {}", notification.getId(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
        }

        notificationRepo.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepo.findByRecipientUserIdOrderByCreatedAtDesc(userId)
            .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepo.countByRecipientUserIdAndStatus(userId, NotificationStatus.PENDING);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepo.markAsRead(notificationId, NotificationStatus.READ);
    }
}
