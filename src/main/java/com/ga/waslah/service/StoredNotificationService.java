package com.ga.waslah.service;

import com.ga.waslah.dto.NotificationResponse;
import com.ga.waslah.dto.PageResponse;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.Notification;
import com.ga.waslah.repository.NotificationRepository;
import com.ga.waslah.repository.UserRepository;
import com.ga.waslah.util.PagingUtil;
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
    private final UserRepository userRepository;

    /** Saves one notification per recipient. Unknown usernames are skipped. */
    @Transactional
    public int saveForUsers(
            Collection<String> usernames,
            String type,
            String message,
            String referenceType,
            Long referenceId
    ) {
        int saved = 0;
        for (String username : usernames) {
            var user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                log.warn("Notification {} skipped: recipient not found", type);
                continue;
            }
            Notification notification = new Notification();
            notification.setRecipient(user);
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
            String username, boolean unreadOnly, int page, int size) {

        Pageable pageable = PagingUtil.createPageable(
                page, size, "createdAt", "desc", "createdAt",
                Set.of("createdAt"));

        Page<Notification> result = unreadOnly
                ? notificationRepository.findByRecipient_UsernameAndReadFalse(username, pageable)
                : notificationRepository.findByRecipient_Username(username, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(NotificationResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isFirst(), result.isLast());
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return notificationRepository.countByRecipient_UsernameAndReadFalse(username);
    }

    /** Marks one of the caller's own notifications as read (idempotent). */
    @Transactional
    public NotificationResponse markRead(Long id, String username) {
        Notification notification = notificationRepository
                .findByIdAndRecipient_Username(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }
}
