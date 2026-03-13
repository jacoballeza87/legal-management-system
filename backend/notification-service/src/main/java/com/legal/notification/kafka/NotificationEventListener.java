package com.legal.notification.kafka;

import com.legal.notification.dto.NotificationRequest;
import com.legal.notification.model.Notification;
import com.legal.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "case-events", groupId = "notification-service-group")
    public void handleCaseEvent(Map<String, Object> event) {
        String eventType  = (String) event.get("eventType");
        Long   caseId     = Long.valueOf(event.get("caseId").toString());
        String caseNumber = (String) event.get("caseNumber");
        log.info("Evento de caso recibido: {} | caso={}", eventType, caseNumber);

        switch (eventType) {
            case "CASE_CREATED" -> notifyLawyerAndClient(event, caseId, caseNumber,
                "📁 Nuevo caso asignado: " + caseNumber,
                "Se ha creado y asignado un nuevo caso: " + caseNumber,
                "case-created", eventType);

            case "CASE_UPDATED" -> notifyLawyerAndClient(event, caseId, caseNumber,
                "🔄 Caso actualizado: " + caseNumber,
                "El caso " + caseNumber + " ha sido actualizado.",
                "case-updated", eventType);

            case "CASE_STATUS_CHANGED" -> {
                String newStatus = (String) event.get("newStatus");
                notifyLawyerAndClient(event, caseId, caseNumber,
                    "📋 Estado cambiado: " + caseNumber,
                    "El caso " + caseNumber + " cambió a estado: " + newStatus,
                    "case-status-changed", eventType);
            }

            case "CASE_CLOSED" -> notifyLawyerAndClient(event, caseId, caseNumber,
                "✅ Caso cerrado: " + caseNumber,
                "El caso " + caseNumber + " ha sido cerrado.",
                "case-closed", eventType);

            case "COLLABORATOR_ADDED" -> {
                if (event.get("collaboratorId") != null && event.get("collaboratorEmail") != null) {
                    notificationService.sendNotification(NotificationRequest.builder()
                        .recipientUserId(Long.valueOf(event.get("collaboratorId").toString()))
                        .recipientEmail((String) event.get("collaboratorEmail"))
                        .subject("👥 Agregado al caso: " + caseNumber)
                        .body("Has sido agregado como colaborador en el caso " + caseNumber + ".")
                        .type(Notification.NotificationType.EMAIL)
                        .entityType("CASE").entityId(caseId).eventType(eventType)
                        .templateName("collaborator-added").build());
                }
            }

            default -> log.debug("Evento no procesado: {}", eventType);
        }
    }

    @KafkaListener(topics = "user-events", groupId = "notification-service-group")
    public void handleUserEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("Evento de usuario recibido: {}", eventType);

        if ("USER_REGISTERED".equals(eventType)) {
            Long   userId = Long.valueOf(event.get("userId").toString());
            String email  = (String) event.get("email");
            notificationService.sendNotification(NotificationRequest.builder()
                .recipientUserId(userId)
                .recipientEmail(email)
                .subject("🎉 Bienvenido al Sistema Legal")
                .body("Tu cuenta ha sido creada exitosamente. Ya puedes acceder al sistema.")
                .type(Notification.NotificationType.EMAIL)
                .entityType("USER").entityId(userId).eventType(eventType)
                .templateName("welcome").build());
        }
    }

    private void notifyLawyerAndClient(Map<String, Object> event, Long caseId,
            String caseNumber, String subject, String body, String template, String eventType) {

        if (event.get("lawyerId") != null && event.get("lawyerEmail") != null) {
            notificationService.sendNotification(NotificationRequest.builder()
                .recipientUserId(Long.valueOf(event.get("lawyerId").toString()))
                .recipientEmail((String) event.get("lawyerEmail"))
                .subject(subject).body(body)
                .type(Notification.NotificationType.EMAIL)
                .priority(Notification.NotificationPriority.MEDIUM)
                .entityType("CASE").entityId(caseId).eventType(eventType)
                .templateName(template).build());
        }

        if (event.get("clientEmail") != null && event.get("clientId") != null) {
            notificationService.sendNotification(NotificationRequest.builder()
                .recipientUserId(Long.valueOf(event.get("clientId").toString()))
                .recipientEmail((String) event.get("clientEmail"))
                .subject(subject).body(body)
                .type(Notification.NotificationType.EMAIL)
                .priority(Notification.NotificationPriority.LOW)
                .entityType("CASE").entityId(caseId).eventType(eventType)
                .templateName(template).build());
        }
    }
}
