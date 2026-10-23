package com.legal.cases.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InactivityCron {
    
    private final CaseRepository caseRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // RN-F-020 & RN-O-010: Alerta de Inactividad (Corre diario)
    @Scheduled(cron = "0 0 0 * * ?")
    public void monitorInactivity() {
        LocalDateTime limitDate = LocalDateTime.now().minusDays(3);
        List<LegalCase> inactiveCases = caseRepo.findCasesWithoutUpdatesSince(limitDate);
        
        for (LegalCase inactive : inactiveCases) {
            kafkaTemplate.send("notification-events", "INACTIVITY_ALERT", inactive.getId());
        }
    }
}
