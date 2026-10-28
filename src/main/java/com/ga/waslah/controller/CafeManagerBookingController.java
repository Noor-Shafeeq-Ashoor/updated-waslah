package com.ga.waslah.controller;

import com.ga.waslah.dto.BookingResponse;
import com.ga.waslah.service.CafeManagerBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Café Manager Bookings", description = "Café managers review bookings for their own cafés. CAFE_MANAGER role required.")
@RestController
@RequestMapping("/api/manager/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CAFE_MANAGER')")
public class CafeManagerBookingController {

    private final CafeManagerBookingService bookingService;

    @Operation(
            summary = "List bookings for my cafés",
            description = "Returns every booking for study spaces in cafés managed by the caller."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of bookings"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)")
    })
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getMyCafeBookings(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                bookingService.getMyCafeBookings(
                        authentication.getName()
                )
        );
    }

    @Operation(
            summary = "Confirm a pending booking",
            description = "Status transition PENDING -> CONFIRMED. Only for bookings in the caller's own cafés. A booking whose end time has passed cannot be confirmed. Runs under the shared booking/session lock, so it cannot overwrite a concurrent cancellation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking confirmed"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Not a café manager, or not your café"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "409", description = "Booking is not PENDING, or it has already ended")
    })
    @PutMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                bookingService.confirmBooking(
                        id,
                        authentication.getName()
                )
        );
    }

    @Operation(
            summary = "Reject a pending booking or cancel a confirmed one",
            description = "Status transition PENDING or CONFIRMED -> CANCELLED, which frees the time slot. Cancelling a CONFIRMED booking also cancels its linked study session, clears its waitlist and notifies the participants (SESSION_CANCELLED). Only for bookings in the caller's own cafés."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking rejected (CANCELLED)"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Not a café manager, or not your café"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "409", description = "Booking is already CANCELLED or COMPLETED")
    })
    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> rejectBooking(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                bookingService.rejectBooking(
                        id,
                        authentication.getName()
                )
        );
    }
}