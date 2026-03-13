package com.legal.notification.scheduler;

import com.legal.notification.service.InactivityAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InactivityCheckScheduler {

    private final InactivityAlertService inactivityAlertService;

    /**
     * Lunes a Viernes 8:00 AM.
     * Configurable via: notification.inactivity.check-cron
     */
    @Scheduled(cron = "${notification.inactivity.check-cron:0 0 8 * * MON-FRI}")
    public void checkInactiveCases() {
        log.info("=== [SCHEDULER] Verificación de casos inactivos ===");
        inactivityAlertService.checkAndAlertInactiveCases();
        log.info("=== [SCHEDULER] Verificación completada ===");
    }
}
