package com.http200ok.finbuddy.notification.controller;

import com.http200ok.finbuddy.notification.dto.NotificationResponseDto;
import com.http200ok.finbuddy.notification.service.NotificationService;
import com.http200ok.finbuddy.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // SSE 연결
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        Long memberId = userDetails.getMemberId();
        return notificationService.subscribe(memberId, lastEventId);
    }

    // 알림 목록 조회
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        List<NotificationResponseDto> notifications = notificationService.getNotifications(memberId);
        return ResponseEntity.ok(notifications);
    }

    // 알림 읽음 표시
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("notificationId") Long notificationId) {
        Long memberId = userDetails.getMemberId();
        notificationService.markAsRead(notificationId, memberId);
        return ResponseEntity.ok().build();
    }

    // 알림 단일 삭제
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("notificationId") Long notificationId) {
        Long memberId = userDetails.getMemberId();
        notificationService.deleteNotification(notificationId, memberId);
        return ResponseEntity.ok().build();
    }

    // 사용자의 모든 알림 삭제
    @DeleteMapping("/member")
    public ResponseEntity<Void> deleteAllNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        notificationService.deleteAllNotifications(memberId);
        return ResponseEntity.ok().build();
    }

    // 읽지 않은 알림 개수 조회
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        Long count = notificationService.getUnreadCount(memberId);
        return ResponseEntity.ok(count);
    }
}