
package com.ga.waslah.service;

import com.ga.waslah.dto.CreateStudySessionRequest;
import com.ga.waslah.dto.StudySessionResponse;
import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.*;
import com.ga.waslah.repository.BookingRepository;
import com.ga.waslah.repository.StudySessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public StudySessionResponse createSession(
            String username,
            CreateStudySessionRequest request
    ) {

        // Load the booking that will host the session.
        Booking booking = bookingRepository
                .findById(request.bookingId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found")
                );

        // Only the booking owner may organize the session.
        if (!booking.getUser().getUsername().equals(username)) {
            throw new ForbiddenException(
                    "You cannot create a session for another user's booking"
            );
        }

        // Sessions require a confirmed booking.
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException(
                    "The booking must be confirmed before creating a session"
            );
        }

        // Reject sessions that exceed the physical space capacity.
        if (request.capacity() > booking.getStudySpace().getCapacity()) {
            throw new ConflictException(
                    "Session capacity exceeds study space capacity"
            );
        }

        // Only one study session can be created per booking.
        if (studySessionRepository.existsByBooking_Id(booking.getId())) {
            throw new ConflictException(
                    "This booking already has a study session"
            );
        }

        StudySession session = new StudySession();

        session.setTitle(request.title());
        session.setDescription(request.description());
        session.setCapacity(request.capacity());
        session.setBooking(booking);
        session.setOrganizer(booking.getUser());
        session.setStatus(StudySessionStatus.OPEN);

        StudySession savedSession = studySessionRepository.save(session);

        log.info("Study session created: id={}", savedSession.getId());

        return toResponse(savedSession);
    }


    private StudySessionResponse toResponse(StudySession session) {

        Booking booking = session.getBooking();

        return new StudySessionResponse(
                session.getId(),
                session.getTitle(),
                session.getDescription(),
                session.getCapacity(),
                session.getStatus(),
                booking.getId(),
                booking.getStudySpace().getId(),
                session.getOrganizer().getUsername(),
                booking.getStartTime(),
                booking.getEndTime()
        );
    }
}
