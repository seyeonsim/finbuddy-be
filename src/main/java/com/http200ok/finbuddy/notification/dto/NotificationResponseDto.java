package com.http200ok.finbuddy.notification.dto;

import com.http200ok.finbuddy.notification.domain.Notification;
import com.http200ok.finbuddy.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    private Long notificationId;
    private NotificationType notificationType;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto fromEntity(Notification notification) {
        return NotificationResponseDto.builder()
                .notificationId(notification.getId())
                .notificationType(notification.getNotificationType())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}