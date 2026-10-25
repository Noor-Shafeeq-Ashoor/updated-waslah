
package com.ga.waslah.service;

import com.ga.waslah.dto.CreateStudySessionRequest;
import com.ga.waslah.dto.JoinSessionResponse;
import com.ga.waslah.dto.StudySessionResponse;
import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.*;
import com.ga.waslah.repository.*;
import com.ga.waslah.model.SessionParticipant;
import com.ga.waslah.repository.SessionParticipantRepository;

import java.util.List;

import com.ga.waslah.repository.UserRepository;
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
    private final SessionParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final SessionWaitlistRepository waitlistRepository;



    @Transactional
    public StudySessionResponse createSession(
            String username,
            CreateStudySessionRequest request
    ) {

        // 1. Lock the organizer to serialize participation changes.
        User organizer = userRepository
                .findByUsernameForUpdate(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        // 2. Find the booking associated with the session.
        Booking booking = bookingRepository
                .findById(request.bookingId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found")
                );

        // 3. Verify that the organizer owns this booking.
        if (!booking.getUser().getId().equals(organizer.getId())) {
            throw new ForbiddenException(
                    "You cannot create a session for another user's booking"
            );
        }

        // 4. Only confirmed bookings may host sessions.
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException(
                    "The booking must be confirmed before creating a session"
            );
        }

        // 5. Ensure the physical study space can accommodate
        // the requested number of participants.
        if (request.capacity() > booking.getStudySpace().getCapacity()) {
            throw new ConflictException(
                    "Session capacity exceeds study space capacity"
            );
        }

        // 6. Prevent creating multiple sessions for one booking.
        if (studySessionRepository.existsByBooking_Id(booking.getId())) {
            throw new ConflictException(
                    "This booking already has a study session"
            );
        }

        // 7. Check whether the organizer already participates
        // in another session during the same time period.
        boolean hasConflict = participantRepository.existsOverlappingSession(
                organizer.getId(),
                booking.getStartTime(),
                booking.getEndTime(),
                List.of(
                        StudySessionStatus.OPEN,
                        StudySessionStatus.FULL
                )
        );

        if (hasConflict) {
            throw new ConflictException(
                    "You are already participating in another "
                            + "study session during this time"
            );
        }

        // 8. Create the study session.
        StudySession session = new StudySession();

        session.setTitle(request.title());
        session.setDescription(request.description());
        session.setCapacity(request.capacity());
        session.setBooking(booking);
        session.setOrganizer(organizer);

        // A capacity of one is immediately full because
        // the organizer occupies the first seat.
        session.setStatus(
                request.capacity() == 1
                        ? StudySessionStatus.FULL
                        : StudySessionStatus.OPEN
        );

        StudySession savedSession =
                studySessionRepository.save(session);

        // 9. Register the organizer as the first participant.
        SessionParticipant participant = new SessionParticipant();

        participant.setSession(savedSession);
        participant.setUser(organizer);

        participantRepository.save(participant);

        log.info(
                "Study session {} created by {} with capacity {}",
                savedSession.getId(),
                username,
                savedSession.getCapacity()
        );

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


    /**
     * Allows an authenticated user to join a study session.
     *
     * Concurrency strategy:
     * 1. Lock the user to prevent overlapping participation.
     * 2. Lock the session to protect available seats.
     * 3. Check existing participation and schedule conflicts.
     * 4. Allocate a seat or place the user on the waitlist.
    */
    @Transactional
    public JoinSessionResponse joinSession(
            Long sessionId,
            String username
    ) {

        // 1. Lock the user first.
        User user = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        // 2. Lock the session to prevent overbooking.
        StudySession session = studySessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Study session not found")
                );

        // 3. Only OPEN or FULL sessions can accept join requests.
        if (session.getStatus() != StudySessionStatus.OPEN
                && session.getStatus() != StudySessionStatus.FULL) {
            throw new ConflictException(
                    "This study session is not accepting participants"
            );
        }

        // 4. Prevent duplicate confirmed participation.
        if (participantRepository.existsBySession_IdAndUser_Id(
                sessionId, user.getId())) {
            throw new ConflictException(
                    "You have already joined this study session"
            );
        }

        // 5. Prevent duplicate waitlist registration.
        if (waitlistRepository.existsBySession_IdAndUser_Id(
                sessionId, user.getId())) {
            throw new ConflictException(
                    "You are already on this session's waitlist"
            );
        }

        // 6. Count confirmed participants.
        long participantCount =
                participantRepository.countBySession_Id(sessionId);

        // 7. Check for schedule conflicts.
        Booking booking = session.getBooking();

        boolean hasConflict = participantRepository.existsOverlappingSession(
                user.getId(),
                booking.getStartTime(),
                booking.getEndTime(),
                List.of(
                        StudySessionStatus.OPEN,
                        StudySessionStatus.FULL
                )
        );

        // 8. Allocate an available seat.
        if (participantCount < session.getCapacity()) {

            if (hasConflict) {
                throw new ConflictException(
                        "You are already participating in another "
                                + "study session during this time"
                );
            }

            SessionParticipant participant = new SessionParticipant();
            participant.setSession(session);
            participant.setUser(user);

            participantRepository.save(participant);

            // Mark the session FULL when the final seat is taken.
            if (participantCount + 1 == session.getCapacity()) {
                session.setStatus(StudySessionStatus.FULL);
            }

            log.info(
                    "User {} joined study session {}",
                    username,
                    sessionId
            );

            return new JoinSessionResponse(
                    sessionId,
                    "JOINED",
                    "You have successfully joined the study session",
                    null
            );
        }

        // 9. Session is full: add the user to the waitlist.
        // Users may wait for overlapping sessions, but cannot
        // hold confirmed seats in overlapping sessions.
        int nextPosition = waitlistRepository
                .findFirstBySession_IdOrderByPositionDesc(sessionId)
                .map(entry -> entry.getPosition() + 1)
                .orElse(1);

        SessionWaitlist waitlistEntry = new SessionWaitlist();
        waitlistEntry.setSession(session);
        waitlistEntry.setUser(user);
        waitlistEntry.setPosition(nextPosition);

        waitlistRepository.save(waitlistEntry);

        log.info(
                "User {} added to waitlist for session {} at position {}",
                username,
                sessionId,
                nextPosition
        );

        return new JoinSessionResponse(
                sessionId,
                "WAITLISTED",
                "The session is full. You have been added to the waitlist",
                nextPosition
        );
    }

}
