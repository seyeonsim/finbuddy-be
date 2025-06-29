package com.http200ok.finbuddy.notification.service;

import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.notification.domain.NotificationType;
import com.http200ok.finbuddy.notification.dto.NotificationResponseDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface NotificationService {
    SseEmitter subscribe(Long memberId, String lastEventId);
    void sendNotification(Member member, NotificationType notificationType, String content);
    List<NotificationResponseDto> getNotifications(Long memberId);
    void markAsRead(Long notificationId, Long memberId);
    void deleteNotification(Long notificationId, Long memberId);
    void deleteAllNotifications(Long memberId);
    Long getUnreadCount(Long memberId);
}
