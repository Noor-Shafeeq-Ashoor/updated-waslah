package com.ga.notification_service.controller;

import com.ga.notification_service.security.CurrentUser;
import com.ga.notification_service.dto.NotificationResponse;
import com.ga.notification_service.dto.PageResponse;
import com.ga.notification_service.service.NotificationService;
import com.ga.notification_service.service.StoredNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Exposes endpoints for receiving real-time user notifications.
 */
@Tag(name = "Notifications", description = "Real-time notifications over Server-Sent Events (SSE).")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final StoredNotificationService storedNotificationService;
    private final CurrentUser currentUser;

    public NotificationController(
            NotificationService notificationService,
            StoredNotificationService storedNotificationService,
            CurrentUser currentUser
    ) {
        this.notificationService = notificationService;
        this.storedNotificationService = storedNotificationService;
        this.currentUser = currentUser;
    }

    @Operation(
            summary = "Subscribe to real-time notifications (SSE)",
            description = "Opens a long-lived text/event-stream for the authenticated user. Events: BOOKING_CREATED (to the booker and the café manager), BOOKING_FAILED, BOOKING_CONFIRMED, BOOKING_REJECTED and BOOKING_CANCELLED (to the booker), and the study-session events SESSION_JOINED and SESSION_LEFT (to the organizer), SESSION_WAITLISTED, SESSION_PROMOTED and SESSION_LEFT_WAITLIST (to the user concerned), and SESSION_CANCELLED / SESSION_COMPLETED (to every participant and waitlisted user). Every event is also stored (delivered by notification-service from Kafka); see GET /api/notifications. Send the JWT in the Authorization header; browsers' native EventSource cannot set headers, so use fetch/EventSource polyfills. Swagger UI does not render live streams."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event stream opened"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(Authentication authentication) {
        return notificationService.subscribe(currentUser.get().id());
    }

    @Operation(
            summary = "List my stored notifications (paginated)",
            description = "Notifications that were stored for the caller, newest first, so events missed while offline can still be read. Query: unreadOnly (default false), page (0-based), size (1-100, default 10)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of notifications"),
            @ApiResponse(responseCode = "400", description = "Invalid page or size"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @GetMapping
    public PageResponse<NotificationResponse> getMyNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return storedNotificationService.list(
                currentUser.get().id(), unreadOnly, page, size);
    }

    @Operation(
            summary = "Count my unread notifications",
            description = "Returns {\"unread\": n} for the caller."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unread count"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(Authentication authentication) {
        return Map.of("unread",
                storedNotificationService.unreadCount(currentUser.get().id()));
    }

    @Operation(
            summary = "Mark one of my notifications as read",
            description = "Only the recipient can mark a notification as read. Repeating the call is harmless."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "404", description = "Notification not found (or not yours)")
    })
    @PutMapping("/{id}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return storedNotificationService.markRead(id, currentUser.get().id());
    }
}
