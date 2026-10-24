package com.legal.notifications.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications") // <-- Alineado con el rewrite del Gateway (/api/notifications/**)
public class NotificationController {

    @GetMapping("/health")
    public String healthCheck() {
        return "Notification Service is up and running";
    }
    
    // Endpoints de correo y SMS...
}
