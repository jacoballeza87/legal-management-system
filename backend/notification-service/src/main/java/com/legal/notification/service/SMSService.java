package com.legal.notification.service;

import com.legal.notification.config.TwilioConfig;
import com.legal.notification.model.Notification;
import com.legal.notification.model.SMSNotification;
import com.legal.notification.repository.SMSNotificationRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SMSService {

    private final TwilioConfig twilioConfig;
    private final SMSNotificationRepository smsRepo;

    public SMSNotification sendSMS(Notification notification) {
        if (!twilioConfig.isEnabled()) {
            log.warn("Twilio deshabilitado. SMS no enviado a {}", notification.getRecipientPhone());
            return null;
        }
        if (notification.getRecipientPhone() == null || notification.getRecipientPhone().isBlank()) {
            log.warn("Sin número de teléfono para userId={}", notification.getRecipientUserId());
            return null;
        }

        SMSNotification smsNotif = SMSNotification.builder()
            .notification(notification)
            .toPhoneNumber(notification.getRecipientPhone())
            .fromPhoneNumber(twilioConfig.getFromNumber())
            .messageBody(notification.getBody())
            .build();

        try {
            String body = notification.getBody();
            if (body.length() > 1600) body = body.substring(0, 1597) + "...";

            Message message = Message.creator(
                new PhoneNumber(notification.getRecipientPhone()),
                new PhoneNumber(twilioConfig.getFromNumber()),
                body
            ).create();

            smsNotif.setTwilioSid(message.getSid());
            smsNotif.setTwilioStatus(message.getStatus().toString());
            smsNotif.setSmsStatus(SMSNotification.SMSStatus.SENT);
            smsNotif.setSentAt(LocalDateTime.now());
            log.info("SMS enviado a {} | SID: {}", notification.getRecipientPhone(), message.getSid());

        } catch (Exception e) {
            log.error("Error Twilio enviando a {}: {}", notification.getRecipientPhone(), e.getMessage());
            smsNotif.setSmsStatus(SMSNotification.SMSStatus.FAILED);
            smsNotif.setErrorDetails(e.getMessage());
        }

        return smsRepo.save(smsNotif);
    }
}
