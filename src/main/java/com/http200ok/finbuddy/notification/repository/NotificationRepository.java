package com.http200ok.finbuddy.notification.repository;

import com.http200ok.finbuddy.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverIdAndDeletedFalseOrderByCreatedAtDesc(Long memberId);
    List<Notification> findByReceiverIdAndDeletedFalse(Long memberId);
    Long countByReceiverIdAndIsReadFalseAndDeletedFalse(Long memberId);
}
