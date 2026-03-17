package com.legal.document.events;

import com.legal.document.service.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEventListener {

    private final GoogleDriveService driveService;

    @RabbitListener(queues = "case.created.queue")
    public void handleCaseEvent(Map<String, Object> event) throws Exception{

        try{
 String eventType = (String) event.get("eventType");

        if ("CASE_CREATED".equals(eventType)) {

            String caseNumber = (String) event.get("caseNumber");

            log.info("Creando carpeta Drive para caso {}", caseNumber);

            driveService.createCaseFolder(caseNumber, null);
        }
        }catch (Exception e){
            System.out.println("exception occurred on handle case event"+e);
        }
       
    }
}