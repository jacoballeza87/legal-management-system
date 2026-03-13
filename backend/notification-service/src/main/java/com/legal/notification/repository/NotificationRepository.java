package com.legal.notification.repository;

import com.legal.notification.model.Notification;
import com.legal.notification.model.Notification.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByRecipientUserIdAndStatus(Long userId, NotificationStatus status);

    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.createdAt < :cutoff")
    List<Notification> findStaleNotifications(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE Notification n SET n.status = :status, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :id")
    void markAsRead(@Param("id") Long id, @Param("status") NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'FAILED', n.errorMessage = :err WHERE n.id = :id")
    void markAsFailed(@Param("id") Long id, @Param("err") String errorMessage);

    long countByRecipientUserIdAndStatus(Long userId, NotificationStatus status);
}
