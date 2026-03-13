package com.legal.notification.mapper;

import com.legal.notification.dto.NotificationResponse;
import com.legal.notification.model.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}
