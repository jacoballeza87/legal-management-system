package com.legal.notification.service;

import com.legal.notification.dto.NotificationRequest;
import com.legal.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InactivityAlertService {

    private final NotificationService notificationService;
    private final RestTemplate restTemplate;

    @Value("${notification.inactivity.threshold-days:7}")
    private int thresholdDays;

    @Value("${services.case-service-url:http://case-service}")
    private String caseServiceUrl;

    @SuppressWarnings("unchecked")
    public void checkAndAlertInactiveCases() {
        log.info("Verificando casos inactivos (umbral: {} días)...", thresholdDays);
        try {
            String url = caseServiceUrl + "/api/cases/inactive?days=" + thresholdDays;
            List<Map<String, Object>> inactiveCases = restTemplate.getForObject(url, List.class);

            if (inactiveCases == null || inactiveCases.isEmpty()) {
                log.info("No hay casos inactivos.");
                return;
            }

            log.info("{} casos inactivos encontrados. Enviando alertas...", inactiveCases.size());

            for (Map<String, Object> caseData : inactiveCases) {
                String caseNumber    = (String) caseData.get("caseNumber");
                Long   caseId        = Long.valueOf(caseData.get("id").toString());
                Long   lawyerId      = Long.valueOf(caseData.get("assignedLawyerId").toString());
                String lawyerEmail   = (String) caseData.get("assignedLawyerEmail");
                String lastActivity  = (String) caseData.get("lastActivityDate");

                NotificationRequest request = NotificationRequest.builder()
                    .recipientUserId(lawyerId)
                    .recipientEmail(lawyerEmail)
                    .subject("⚠️ Caso Inactivo: " + caseNumber)
                    .body("El caso " + caseNumber + " no ha tenido actividad en los últimos "
                        + thresholdDays + " días. Última actividad: " + lastActivity
                        + ". Por favor, actualice el estado del caso.")
                    .type(Notification.NotificationType.EMAIL)
                    .priority(Notification.NotificationPriority.HIGH)
                    .entityType("CASE")
                    .entityId(caseId)
                    .eventType("CASE_INACTIVITY_ALERT")
                    .templateName("inactivity-alert")
                    .templateData(Map.of(
                        "caseNumber",    caseNumber,
                        "thresholdDays", thresholdDays,
                        "lastActivity",  lastActivity
                    ))
                    .build();

                notificationService.sendNotification(request);
            }
        } catch (Exception e) {
            log.error("Error verificando casos inactivos: {}", e.getMessage());
        }
    }
}
