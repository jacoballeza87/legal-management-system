package com.legal.notification.repository;

import com.legal.notification.model.SMSNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SMSNotificationRepository extends JpaRepository<SMSNotification, Long> {
    Optional<SMSNotification> findByTwilioSid(String twilioSid);
}
