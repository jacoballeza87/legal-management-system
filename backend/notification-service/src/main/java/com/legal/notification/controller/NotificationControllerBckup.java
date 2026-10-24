package com.legal.notification.controller;

import com.legal.notification.dto.NotificationRequest;
import com.legal.notification.dto.NotificationResponse;
import com.legal.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** Enviar notificación manual — solo ADMIN */
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> send(@Valid @RequestBody NotificationRequest request) {
        notificationService.sendNotification(request);
        return ResponseEntity.accepted().body(Map.of("message", "Notificación encolada exitosamente"));
    }

    /** Mis notificaciones */
    @GetMapping("/me/{userId}")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    /** Contar no leídas */
    @GetMapping("/me/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> countUnread(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    /** Marcar como leída */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}
