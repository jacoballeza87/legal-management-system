package com.legal.notification.listeners;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CaseEventListener {

    private final PdfGeneratorService pdfService;
    private final SmsService smsService; // Integración con Twilio/AWS SNS
    private final EmailService emailService;
    private final QrCodeService qrService;

    // RN-F-018 & RN-F-022: Al crear caso, hacer PDF y código QR
    @KafkaListener(topics = "case-events")
    public void handleCaseCreated(LegalCase newCase) {
        byte[] pdf = pdfService.generateCaseSummary(newCase);
        String qrCodeUrl = qrService.generateQrCode("https://legal-app.com/cases/" + newCase.getDossierNumber());
        
        emailService.sendCreationEmail(newCase.getAssigneeEmail(), pdf, qrCodeUrl);
        emailService.sendCreationEmail("sysadmin@legal.com", pdf, qrCodeUrl);
    }

    // RN-F-020: Alerta Inactividad
    @KafkaListener(topics = "notification-events")
    public void handleInactivity(Long caseId) {
        // Enviar SMS y Email
        smsService.sendSms("+1234567890", "ALERTA: El caso " + caseId + " lleva 3 días inactivo.");
    }
}
