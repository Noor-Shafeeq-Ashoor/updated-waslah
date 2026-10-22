
package com.ga.waslah.controller;

import com.ga.waslah.service.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Exposes endpoints for receiving real-time user notifications.
 */
@Tag(name = "Notifications", description = "Real-time notifications over Server-Sent Events (SSE).")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "Subscribe to real-time notifications (SSE)",
            description = "Opens a long-lived text/event-stream for the authenticated user. Events: BOOKING_CREATED (to the booker and the café manager) and BOOKING_FAILED (to the booker). Send the JWT in the Authorization header; browsers' native EventSource cannot set headers, so use fetch/EventSource polyfills. Swagger UI does not render live streams."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event stream opened"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(Authentication authentication) {
        return notificationService.subscribe(authentication.getName());
    }
}
