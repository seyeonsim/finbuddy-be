package com.http200ok.finbuddy.notification.service;

import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.notification.domain.Notification;
import com.http200ok.finbuddy.notification.domain.NotificationType;
import com.http200ok.finbuddy.notification.dto.NotificationResponseDto;
import com.http200ok.finbuddy.notification.repository.EmitterRepository;
import com.http200ok.finbuddy.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000L * 60; // 1시간
    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;

    // 알림 구독 요청 시 호출됨
    public SseEmitter subscribe(Long memberId, String lastEventId) {
        String emitterId = memberId + "_" + System.currentTimeMillis();
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));
        emitter.onError((e) -> emitterRepository.deleteById(emitterId));

        // 연결 직후 더미 이벤트 전송(503 방지)
        String eventId = memberId + "_" + System.currentTimeMillis();
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name("connect")
                    .data("Connected!"));
        } catch (Exception e) {
            emitterRepository.deleteById(emitterId);
        }

        // 미수신 이벤트가 있으면 전송
        if (lastEventId != null && !lastEventId.isEmpty()) {
            Map<String, Object> events = emitterRepository.findAllEventCacheStartWithByMemberId(String.valueOf(memberId));
            events.entrySet().stream()
                    .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                    .forEach(entry -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .id(entry.getKey())
                                    .name("notification")
                                    .data(entry.getValue()));
                        } catch (Exception e) {
                            emitterRepository.deleteById(emitterId);
                        }
                    });
        }

        return emitter;
    }

    // 알림 보내는 메서드
    @Transactional
    public void sendNotification(Member member, NotificationType notificationType, String content) {
        Notification notification = Notification.builder()
                .receiver(member)
                .notificationType(notificationType)
                .content(content)
                .build();

        notificationRepository.save(notification);

        String memberId = member.getId().toString();
        String eventId = memberId + "_" + System.currentTimeMillis();

        // 이벤트 캐시에 저장
        emitterRepository.saveEventCache(eventId, NotificationResponseDto.fromEntity(notification));

        sendToClient(memberId, notification, eventId);
    }

    // 클라이언트에게 이벤트 전송
    private void sendToClient(String memberId, Notification notification, String eventId) {
        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmitterStartWithByMemberId(memberId);
        sseEmitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(eventId)
                        .name("notification")
                        .data(NotificationResponseDto.fromEntity(notification)));
            } catch (Exception e) {
                emitterRepository.deleteById(key);
            }
        });
    }

    // 알림 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(Long memberId) {
        List<Notification> notifications = notificationRepository.findByReceiverIdAndDeletedFalseOrderByCreatedAtDesc(memberId);
        return notifications.stream()
                .map(NotificationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 알림 읽음 표시
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!notification.getReceiver().getId().equals(memberId)) {
            throw new SecurityException("권한이 없는 알림입니다.");
        }

        notification.markAsRead();
    }

    // 알림 단일 삭제 (소프트 삭제)
    @Transactional
    public void deleteNotification(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!notification.getReceiver().getId().equals(memberId)) {
            throw new SecurityException("권한이 없는 알림입니다.");
        }

        notification.delete();
    }

    // 사용자의 모든 알림 삭제 (소프트 삭제)
    @Transactional
    public void deleteAllNotifications(Long memberId) {
        List<Notification> notifications = notificationRepository.findByReceiverIdAndDeletedFalse(memberId);
        notifications.forEach(Notification::delete);
    }

    // 읽지 않은 알림 개수 조회
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long memberId) {
        return notificationRepository.countByReceiverIdAndIsReadFalseAndDeletedFalse(memberId);
    }
}
