package com.legal.notification.repository;

import com.legal.notification.model.EmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {
    Optional<EmailNotification> findByMessageId(String messageId);
}
