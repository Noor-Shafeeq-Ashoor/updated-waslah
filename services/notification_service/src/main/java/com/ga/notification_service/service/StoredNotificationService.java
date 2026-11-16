package com.ga.notification_service.service;

import com.ga.notification_service.dto.NotificationResponse;
import com.ga.notification_service.dto.PageResponse;
import com.ga.notification_service.exception.ResourceNotFoundException;
import com.ga.notification_service.model.Notification;
import com.ga.notification_service.repository.NotificationRepository;
import com.ga.notification_service.util.PagingUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

/**
 * Stores notifications so that users who were offline (not connected to the
 * SSE stream) can read them later.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoredNotificationService {

    private final NotificationRepository notificationRepository;


    /** Saves one notification per recipient. Unknown usernames are skipped. */
    @Transactional
    public int saveForUsers(
            Collection<Long> recipientUserIds,
            String type,
            String message,
            String referenceType,
            Long referenceId
    ) {
        int saved = 0;
        for (Long userId : recipientUserIds) {
            if (userId == null) {
                continue;
            }
            Notification notification = new Notification();
            notification.setRecipientUserId(userId);
            notification.setType(type);
            notification.setMessage(message);
            notification.setReferenceType(referenceType);
            notification.setReferenceId(referenceId);
            notificationRepository.save(notification);
            saved++;
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(
            Long userId, boolean unreadOnly, int page, int size) {

        Pageable pageable = PagingUtil.createPageable(
                page, size, "createdAt", "desc", "createdAt",
                Set.of("createdAt"));

        Page<Notification> result = unreadOnly
                ? notificationRepository.findByRecipientUserIdAndReadFalse(userId, pageable)
                : notificationRepository.findByRecipientUserId(userId, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(NotificationResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isFirst(), result.isLast());
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(userId);
    }


    /** Marks one of the caller's own notifications as read (idempotent). */
    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        Notification notification = notificationRepository
                .findByIdAndRecipientUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }
}
