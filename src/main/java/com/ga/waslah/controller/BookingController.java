
package com.ga.waslah.controller;

import com.ga.waslah.dto.BookingRequest;
import com.ga.waslah.dto.BookingResponse;
import com.ga.waslah.service.BookingService;
import com.ga.waslah.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.ga.waslah.dto.PageResponse;
import com.ga.waslah.model.BookingStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDateTime;

/** Handles HTTP requests related to bookings.*/
@Tag(name = "Bookings", description = "Study-space booking workflow for users: create, list, view and cancel own bookings.")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final NotificationService notificationService;

    /**
     * Creates a booking for the authenticated user.
     * If booking creation fails, sends a real-time failure
     * notification to the user who attempted the booking.
     * The exception is rethrown so the application's normal
     * error handling can process the failed request.
     */
    @Operation(
            summary = "Create a booking",
            description = "Creates a PENDING booking. Rejected when end is not after start, the space is unavailable, or the slot overlaps a PENDING/CONFIRMED booking (the study space row is locked during the check to prevent double booking). Triggers a real-time notification and an audit-log entry."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created (status PENDING)"),
            @ApiResponse(responseCode = "400", description = "Validation error, invalid time range or slot already booked"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Study space is not available"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();

        try {
            BookingResponse response =
                    bookingService.createBooking(username, request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (RuntimeException exception) {
            notificationService.sendToUser(
                    username,
                    "BOOKING_FAILED",
                    "Your booking could not be completed. "
                            + "Please review the details and try again."
            );

            throw exception;
        }
    }

    @Operation(
            summary = "Search my bookings (paginated)",
            description = "Only the caller's own bookings. Optional filters: status, studySpaceId, from / to (ISO date-time, applied to the start time). Pagination: page (0-based), size (1-100, default 10). Sorting: sort = startTime | endTime | createdAt | status, direction = asc | desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of bookings"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, page, size, sort field or direction"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @GetMapping
    public ResponseEntity<PageResponse<BookingResponse>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long studySpaceId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                bookingService.getMyBookings(
                        authentication.getName(),
                        status,
                        studySpaceId,
                        from,
                        to,
                        page,
                        size,
                        sort,
                        direction
                )
        );
    }

    @Operation(
            summary = "Get one of my bookings",
            description = "Returns a booking by id. Only the booking's owner may view it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "The booking belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                bookingService.getBookingById(id, authentication.getName())
        );
    }


    @Operation(
            summary = "Confirm a pending booking (deprecated)",
            description = "Deprecated duplicate of PUT /api/manager/bookings/{id}/confirm, "
                    + "kept so existing clients keep working. It shares the same locked "
                    + "implementation. Allows the assigned cafe manager to confirm "
                    + "a pending booking.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking confirmed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Not the assigned cafe manager"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Booking is not pending, or it has already ended")
    })
    @PutMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable Long id,
            Authentication authentication
    ) {

        BookingResponse response = bookingService.confirmBooking(
                id,
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Cancel my booking",
            description = "Status transition PENDING/CONFIRMED -> CANCELLED, which frees the time slot. Only the owner may cancel; COMPLETED and already CANCELLED bookings cannot be cancelled. Writes an audit-log entry and sends a BOOKING_CANCELLED notification."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Not your booking, or it can no longer be cancelled"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication.getName();

        BookingResponse response =
                bookingService.cancelBooking(id, username);

        notificationService.sendToUser(
                username,
                "BOOKING_CANCELLED",
                "Your booking has been cancelled. Booking ID: " + id
        );

        return ResponseEntity.ok(response);
    }

}
