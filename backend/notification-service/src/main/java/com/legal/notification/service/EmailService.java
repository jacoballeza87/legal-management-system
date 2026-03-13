package com.legal.notification.service;

import com.legal.notification.model.EmailNotification;
import com.legal.notification.model.Notification;
import com.legal.notification.repository.EmailNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final SesClient sesClient;
    private final EmailNotificationRepository emailRepo;
    private final TemplateEngine templateEngine;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    @Value("${aws.ses.from-name}")
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
            SendEmailRequest request = SendEmailRequest.builder()
                .source(fromName + " <" + fromEmail + ">")
                .destination(Destination.builder()
                    .toAddresses(notification.getRecipientEmail())
                    .build())
                .message(Message.builder()
                    .subject(Content.builder()
                        .data(notification.getSubject())
                        .charset("UTF-8")
                        .build())
                    .body(Body.builder()
                        .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                        .text(Content.builder().data(textBody).charset("UTF-8").build())
                        .build())
                    .build())
                .build();

            SendEmailResponse sesResponse = sesClient.sendEmail(request);
            emailNotif.setMessageId(sesResponse.messageId());
            emailNotif.setSesRequestId(sesResponse.responseMetadata().requestId());
            emailNotif.setEmailStatus(EmailNotification.EmailStatus.SENT);
            emailNotif.setSentAt(LocalDateTime.now());
            log.info("Email enviado a {} | MessageId: {}", notification.getRecipientEmail(), sesResponse.messageId());

        } catch (SesException e) {
            log.error("Error SES enviando a {}: {}", notification.getRecipientEmail(), e.getMessage());
            emailNotif.setEmailStatus(EmailNotification.EmailStatus.DELIVERY_FAILED);
            emailNotif.setErrorDetails(e.getMessage());
            throw new RuntimeException("Error SES: " + e.getMessage(), e);
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
