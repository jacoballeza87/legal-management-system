package com.legal.notification.service;

import com.legal.notification.model.EmailNotification;
import com.legal.notification.model.Notification;
import com.legal.notification.repository.EmailNotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Envío de correo vía SMTP (SendGrid) usando JavaMailSender de Spring.
 *
 * Reemplaza la integración anterior con el SDK de AWS SES — la lógica de
 * negocio (guardar el registro de EmailNotification, plantillas Thymeleaf,
 * HTML de respaldo) no cambió, solo el mecanismo de envío.
 *
 * Los campos messageId/sesRequestId del modelo se mantienen por compatibilidad
 * con el esquema existente; ahora contienen un ID generado localmente en vez
 * del ID que devolvía SES.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailNotificationRepository emailRepo;
    private final TemplateEngine templateEngine;

    @Value("${notification.mail.from-email}")
    private String fromEmail;

    @Value("${notification.mail.from-name}")
    private String fromName;

    public EmailNotification sendEmail(Notification notification, String htmlBody, String textBody) {
        EmailNotification emailNotif = EmailNotification.builder()
            .notification(notification)
            .toEmail(notification.getRecipientEmail())
            .fromEmail(fromEmail)
            .fromName(fromName)
            .subject(notification.getSubject())
            .htmlBody(htmlBody)
            .textBody(textBody)
            .build();

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject(notification.getSubject());
            // setText(text, html) arma automáticamente un multipart/alternative,
            // equivalente a Body.html()+Body.text() que usaba el SDK de SES.
            helper.setText(textBody, htmlBody);

            mailSender.send(mimeMessage);

            String generatedId = "smtp-" + UUID.randomUUID();
            emailNotif.setMessageId(generatedId);
            emailNotif.setSesRequestId(generatedId);
            emailNotif.setEmailStatus(EmailNotification.EmailStatus.SENT);
            emailNotif.setSentAt(LocalDateTime.now());
            log.info("Email enviado a {} | MessageId: {}", notification.getRecipientEmail(), generatedId);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Error enviando email a {}: {}", notification.getRecipientEmail(), e.getMessage());
            emailNotif.setEmailStatus(EmailNotification.EmailStatus.DELIVERY_FAILED);
            emailNotif.setErrorDetails(e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }

        return emailRepo.save(emailNotif);
    }

    public String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        return templateEngine.process("emails/" + templateName, context);
    }

    /** Fallback: HTML básico sin template */
    public String buildDefaultHtml(String subject, String body) {
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>body{font-family:Arial,sans-serif;background:#f5f5f5;padding:20px;}
            .box{max-width:600px;margin:auto;background:white;border-radius:8px;padding:30px;
                 box-shadow:0 2px 10px rgba(0,0,0,.1);}
            .header{background:#1F3864;color:white;padding:20px;border-radius:6px 6px 0 0;text-align:center;}
            </style></head><body>
            <div class="box">
              <div class="header"><h2>⚖️ Legal Management System</h2></div>
              <div style="padding:20px">
                <h3>%s</h3><p>%s</p>
              </div>
            </div></body></html>
            """.formatted(subject, body);
    }
}
