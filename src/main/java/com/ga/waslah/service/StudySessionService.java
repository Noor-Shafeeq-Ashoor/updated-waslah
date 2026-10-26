
package com.ga.waslah.service;

import com.ga.waslah.dto.CreateStudySessionRequest;
import com.ga.waslah.dto.JoinSessionResponse;
import com.ga.waslah.dto.StudySessionResponse;
import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.*;
import com.ga.waslah.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles the business logic for study sessions.
 * Features:
 * - Create study sessions from confirmed bookings.
 * - Register the organizer automatically.
 * - Join sessions with capacity and schedule checks.
 * - Add users to a waitlist when sessions are full.
 * - Leave sessions and promote eligible waiting users.
*/
@Service
@RequiredArgsConstructor
@Slf4j
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final BookingRepository bookingRepository;
    private final SessionParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final SessionWaitlistRepository waitlistRepository;

    /**
     * Creates a study session using an existing confirmed booking
     * The organizer is automatically registered as the first participant.
     */
    @Transactional
    public StudySessionResponse createSession(String username, CreateStudySessionRequest request) {

        // Lock the organizer to serialize participation changes.
        User organizer = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        // Find the booking.
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found")
                );

        // Verify booking ownership.
        if (!booking.getUser().getId().equals(organizer.getId())) {
            throw new ForbiddenException(
                    "You cannot create a session for another user's booking"
            );
        }

        // Only confirmed bookings may host sessions.
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException(
                    "The booking must be confirmed before creating a session"
            );
        }

        // Prevent creating sessions for expired bookings.
        if (!booking.getEndTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException(
                    "Cannot create a session for an expired booking"
            );
        }

        // Ensure capacity is within the physical study space limit.
        if (request.capacity() > booking.getStudySpace().getCapacity()) {
            throw new ConflictException(
                    "Session capacity exceeds study space capacity"
            );
        }

        // Prevent multiple sessions for the same booking.
        if (studySessionRepository.existsByBooking_Id(booking.getId())) {
            throw new ConflictException(
                    "This booking already has a study session"
            );
        }

        // Prevent overlapping confirmed participation.
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

        // Create the session.
        StudySession session = new StudySession();
        session.setTitle(request.title());
        session.setDescription(request.description());
        session.setCapacity(request.capacity());
        session.setBooking(booking);
        session.setOrganizer(organizer);

        // The organizer occupies the first seat.
        session.setStatus(
                request.capacity() == 1
                        ? StudySessionStatus.FULL
                        : StudySessionStatus.OPEN
        );

        StudySession savedSession = studySessionRepository.save(session);

        // Register organizer as a participant.
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

    /**
     * Converts a StudySession entity into a response DTO.
     */
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
     * Lock order:
     * 1. User
     * 2. Study session
     * Available seats are allocated only when there is no
     * existing waitlist and no schedule conflict.
     */
    @Transactional
    public JoinSessionResponse joinSession(
            Long sessionId,
            String username
    ) {

        // Lock user first.
        User user = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Lock session to protect capacity.
        StudySession session = studySessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Study session not found"));

        // Validate session status.
        if (session.getStatus() != StudySessionStatus.OPEN && session.getStatus() != StudySessionStatus.FULL) {
            throw new ConflictException("This study session is not accepting participants");
        }

        Booking booking = session.getBooking();

        // Validate booking status.
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException(
                    "Cannot join a session with an unconfirmed booking"
            );
        }

        // Prevent joining an expired session.
        if (!booking.getEndTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException(
                    "This study session has already ended"
            );
        }

        // Prevent duplicate confirmed participation.
        if (participantRepository.existsBySession_IdAndUser_Id(
                sessionId, user.getId())) {
            throw new ConflictException(
                    "You have already joined this study session"
            );
        }

        // Prevent duplicate waitlist registration.
        if (waitlistRepository.existsBySession_IdAndUser_Id(
                sessionId, user.getId())) {
            throw new ConflictException(
                    "You are already on this session's waitlist"
            );
        }

        long participantCount =
                participantRepository.countBySession_Id(sessionId);

        // Check whether the session already has waiting users.
        boolean hasWaitingUsers = !waitlistRepository
                .findBySession_IdOrderByPositionAsc(sessionId)
                .isEmpty();

        // Prevent newly arriving users from bypassing the waitlist.
        if (participantCount < session.getCapacity() && hasWaitingUsers) {
            throw new ConflictException(
                    "This session has a waiting list. "
                            + "Available seats are reserved for waitlist promotion"
            );
        }

        // Allocate an available seat.
        if (participantCount < session.getCapacity()) {

            // Check for overlapping confirmed participation.
            boolean hasConflict =
                    participantRepository.existsOverlappingSession(
                            user.getId(),
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

            SessionParticipant participant = new SessionParticipant();
            participant.setSession(session);
            participant.setUser(user);

            participantRepository.save(participant);

            // Update session status when the final seat is taken.
            if (participantCount + 1 >= session.getCapacity()) {
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

        // Session is full: add user to the waitlist.
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

    /**
     * Removes an authenticated participant from a study session.
     * The organizer cannot leave their own session.
     * After a seat is released, waitlist promotion is attempted.
     */
    @Transactional
    public void leaveSession(Long sessionId, String username) {

        // Lock user first.
        User user = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        // Lock session before modifying participation.
        StudySession session = studySessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Study session not found")
                );

        // Prevent leaving inactive sessions.
        if (session.getStatus() == StudySessionStatus.CANCELLED
                || session.getStatus() == StudySessionStatus.COMPLETED) {
            throw new ConflictException(
                    "You cannot leave an inactive study session"
            );
        }

        // Organizer cannot leave without cancelling the session.
        if (session.getOrganizer().getId().equals(user.getId())) {
            throw new ConflictException(
                    "The organizer cannot leave their own study session"
            );
        }

        // Find the participant record.
        SessionParticipant participant = participantRepository
                .findBySession_IdAndUser_Id(sessionId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "You are not a participant of this session"
                        )
                );

        // Remove participant and flush the deletion.
        participantRepository.delete(participant);
        participantRepository.flush();

        // A seat is now available.
        session.setStatus(StudySessionStatus.OPEN);

        // Attempt to promote the earliest eligible waiting user.
        boolean promoted = promoteNextEligibleUser(session);

        if (promoted) {
            log.info(
                    "Waitlist promotion completed for session {}",
                    sessionId
            );
        }

        log.info(
                "User {} left study session {}",
                username,
                sessionId
        );
    }

    /**
     * Promotes the earliest eligible waitlisted user.
     * This method must be called while the session is locked
     * inside an active transaction.
     * IMPORTANT:
   */
    private boolean promoteNextEligibleUser(StudySession session) {

        Long sessionId = session.getId();

        // Do not promote users into inactive sessions.
        if (session.getStatus() == StudySessionStatus.CANCELLED
                || session.getStatus() == StudySessionStatus.COMPLETED) {
            return false;
        }

        // Only confirmed bookings can host active sessions.
        Booking booking = session.getBooking();

        if (booking.getStatus() != BookingStatus.CONFIRMED
                || !booking.getEndTime().isAfter(LocalDateTime.now())) {
            return false;
        }

        long participantCount =
                participantRepository.countBySession_Id(sessionId);

        // No available seat.
        if (participantCount >= session.getCapacity()) {
            return false;
        }

        // Retrieve users in waitlist order.
        List<SessionWaitlist> waitingUsers =
                waitlistRepository.findBySession_IdOrderByPositionAsc(sessionId);

        for (SessionWaitlist entry : waitingUsers) {

            User waitingUser = entry.getUser();

            // Skip users with overlapping confirmed sessions.
            boolean hasConflict =
                    participantRepository.existsOverlappingSession(
                            waitingUser.getId(),
                            booking.getStartTime(),
                            booking.getEndTime(),
                            List.of(
                                    StudySessionStatus.OPEN,
                                    StudySessionStatus.FULL
                            )
                    );

            if (hasConflict) {
                continue;
            }

            // Create confirmed participation.
            SessionParticipant participant = new SessionParticipant();
            participant.setSession(session);
            participant.setUser(waitingUser);

            participantRepository.save(participant);

            // Remove the user from the waitlist.
            waitlistRepository.delete(entry);

            // Mark the session FULL if the final seat was taken.
            if (participantCount + 1 >= session.getCapacity()) {
                session.setStatus(StudySessionStatus.FULL);
            } else {
                session.setStatus(StudySessionStatus.OPEN);
            }

            log.info(
                    "User {} promoted from waitlist for session {}",
                    waitingUser.getUsername(),
                    sessionId
            );

            return true;
        }

        return false;
    }
}
