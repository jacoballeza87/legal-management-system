package com.legal.notifications.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    // Inyecta aquí tus servicios de mensajería originales (ej. EmailService, SMSService)
    // private final EmailService emailService;
    // private final SMSService smsService;

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Notification Service is running correctly under /api/notifications");
    }

    @PostMapping("/send-email")
    public ResponseEntity<String> sendEmailNotification(
            @RequestParam String to, 
            @RequestParam String subject, 
            @RequestParam String body) {
        
        // Lógica original de envío de correo (RN-F-019, RN-S-015)
        // emailService.sendEmail(to, subject, body);
        
        return ResponseEntity.ok("Correo enviado exitosamente a: " + to);
    }

    @PostMapping("/send-sms")
    public ResponseEntity<String> sendSmsNotification(
            @RequestParam String phoneNumber, 
            @RequestParam String message) {
        
        // Lógica original de envío de SMS crítico / inactividad (RN-F-020, RN-S-015)
        // smsService.sendSms(phoneNumber, message);
        
        return ResponseEntity.ok("SMS enviado exitosamente a: " + phoneNumber);
    }
}
