package com.legal.document.kafka;

import com.legal.document.service.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Al crearse un caso, crea automáticamente su carpeta en Google Drive.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEventListener {

    private final GoogleDriveService driveService;

    @KafkaListener(topics = "case-events", groupId = "document-service-group")
    public void handleCaseEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");

        if ("CASE_CREATED".equals(eventType)) {
            String caseNumber  = (String) event.get("caseNumber");
            String lawyerEmail = (String) event.get("lawyerEmail");
            log.info("CASE_CREATED detectado: {}. Creando carpeta en Drive...", caseNumber);
            try {
                String folderId = driveService.createCaseFolder(caseNumber, lawyerEmail);
                if (folderId != null) {
                    log.info("Carpeta Drive creada para caso {}: {}", caseNumber, folderId);
                }
            } catch (Exception e) {
                log.error("Error creando carpeta Drive para {}: {}", caseNumber, e.getMessage());
            }
        }
    }
}
