package com.legal.document.events;

import com.legal.document.service.GoogleDriveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "google.drive.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class DocumentEventListener {

    private final GoogleDriveService driveService;

    @Autowired
    public DocumentEventListener(GoogleDriveService driveService) {
        this.driveService = driveService;
    }

    @RabbitListener(queues = "case.created.queue")
    public void handleCaseEvent(Map<String, Object> event) throws Exception {
        try {
            String eventType = (String) event.get("eventType");
            if ("CASE_CREATED".equals(eventType)) {
                String caseNumber = (String) event.get("caseNumber");
                log.info("Creando carpeta Drive para caso {}", caseNumber);
                driveService.createCaseFolder(caseNumber, null);
            }
        } catch (Exception e) {
            log.error("Exception on handleCaseEvent: {}", e.getMessage());
        }
    }
}