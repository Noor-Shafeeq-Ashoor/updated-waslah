package com.ga.workspace_service.service;

import com.ga.workspace_service.dto.CreateStudySessionRequest;
import com.ga.workspace_service.dto.JoinSessionResponse;
import com.ga.workspace_service.dto.SessionEventResponse;
import com.ga.workspace_service.dto.StudySessionResponse;
import com.ga.workspace_service.exception.ConflictException;
import com.ga.workspace_service.exception.ForbiddenException;
import com.ga.workspace_service.exception.ResourceNotFoundException;
import com.ga.workspace_service.model.*;
import com.ga.workspace_service.repository.*;
import com.ga.workspace_service.security.CurrentUser;
import com.ga.workspace_service.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages study sessions and their waitlists.
 * All mutating methods acquire the SAME PostgreSQL transaction-level advisory lock before reading state
 * used for decisions. This provides a straightforward serial execution order for study-session writes across
 * application instances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudySessionService {

    private static final List<StudySessionStatus> ACTIVE_STATUSES = List.of(StudySessionStatus.OPEN, StudySessionStatus.FULL);

    private final StudySessionRepository studySessionRepository;
    private final BookingRepository bookingRepository;
    private final SessionParticipantRepository participantRepository;
    private final CurrentUser currentUser;
    private final IdentityClient identityClient;
    private final SessionWaitlistRepository waitlistRepository;
    private final ParticipationLockService participationLockService;
    private final SessionActivityService sessionActivityService;
    private final SessionEventRepository sessionEventRepository;

    /** Creates a session from a confirmed booking and reserves the first seat for its organizer. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StudySessionResponse createSession(String username, CreateStudySessionRequest request) {
        participationLockService.lockStudySessionWrites();

        JwtPrincipal organizer = currentUser.get();
        Booking booking = bookingRepository.findById(request.bookingId()).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUserId().equals(organizer.id())) {
            throw new ForbiddenException("You cannot create a session for another user's booking");
        }
        ensureBookingActive(booking);

        if (request.capacity() == null || request.capacity() < 2) {
            throw new ConflictException("Session capacity must be at least 2");
        }
        if (request.capacity() > booking.getStudySpace().getCapacity()) {
            throw new ConflictException("Session capacity exceeds study space capacity");
        }
        if (studySessionRepository.existsByBooking_Id(booking.getId())) {
            throw new ConflictException("This booking already has a study session");
        }
        if (hasOverlappingParticipation(organizer.id(), booking)) {
            throw new ConflictException("You already participate in an overlapping study session");
        }

        StudySession session = new StudySession();
        session.setTitle(request.title());
        session.setDescription(request.description());
        session.setCapacity(request.capacity());
        session.setBooking(booking);
        session.setOrganizerUserId(organizer.id());
        session.setStatus(request.capacity() == 1 ? StudySessionStatus.FULL : StudySessionStatus.OPEN);
        StudySession saved = studySessionRepository.save(session);

        SessionParticipant firstParticipant = new SessionParticipant();
        firstParticipant.setSession(saved);
        firstParticipant.setUserId(organizer.id());
        participantRepository.save(firstParticipant);

        sessionActivityService.record(saved, organizer.id(), SessionEventType.CREATED,
                "Session created with capacity " + saved.getCapacity());

        log.info("Created study session {} for organizer {}", saved.getId(), username);
        return toResponse(saved);
    }

    /**
     * Joins an available session; otherwise queues the user.
     * Existing waitlisted users always have priority over new arrivals.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JoinSessionResponse joinSession(Long sessionId, String username) {
        participationLockService.lockStudySessionWrites();

        JwtPrincipal user = currentUser.get();

        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Study session not found"));
        ensureSessionJoinable(session);

        if (participantRepository.existsBySession_IdAndUserId(sessionId, user.id())) {
            throw new ConflictException("You have already joined this study session");
        }
        if (waitlistRepository.existsBySession_IdAndUserId(sessionId, user.id())) {
            throw new ConflictException("You are already on this session's waitlist");
        }

        long confirmed = participantRepository.countBySession_Id(sessionId);
        List<SessionWaitlist> waiting = waitlistRepository.findBySession_IdOrderByPositionAsc(sessionId);

        if (confirmed < session.getCapacity() && waiting.isEmpty()) {
            if (hasOverlappingParticipation(user.id(), session.getBooking())) {
                throw new ConflictException("You already participate in an overlapping study session");
            }


            addParticipant(session, user.id());
            updateCapacityStatus(session);

            sessionActivityService.record(session, user.id(), SessionEventType.JOINED,
                    "Joined; status now " + session.getStatus(),
                    List.of(session.getOrganizerUserId()),
                    username + " joined your study session \"" + session.getTitle() + "\"");
            log.info("User {} joined session {}", username, sessionId);
            return new JoinSessionResponse(sessionId, "JOINED",
                    "You have successfully joined the study session", null);
        }

        // Preserve FIFO fairness: new arrivals queue behind existing waiters,
        // even when a seat is temporarily vacant because earlier waiters conflict.
        int nextPosition = waitlistRepository.findFirstBySession_IdOrderByPositionDesc(sessionId)
                .map(entry -> entry.getPosition() + 1)
                .orElse(1);
        SessionWaitlist entry = new SessionWaitlist();
        entry.setSession(session);
        entry.setUserId(user.id());
        entry.setPosition(nextPosition);
        waitlistRepository.save(entry);
        sessionActivityService.record(session, user.id(), SessionEventType.WAITLISTED,
                "Waitlist position " + nextPosition,
                List.of(user.id()),
                "You are number " + nextPosition + " on the waiting list for \"" + session.getTitle() + "\"");
        log.info("User {} waitlisted for session {} at position {}", username, sessionId, nextPosition);
        return new JoinSessionResponse(sessionId, "WAITLISTED",
                "The session has a waiting list; you have been added to it", nextPosition);
    }

    /** Leaves a session and fills the released seat from the eligible FIFO waitlist. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void leaveSession(Long sessionId, String username) {
        participationLockService.lockStudySessionWrites();

        JwtPrincipal user = currentUser.get();
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Study session not found"));
        if (!ACTIVE_STATUSES.contains(session.getStatus())) {
            throw new ConflictException("Cannot leave an inactive study session");
        }
        if (session.getOrganizerUserId().equals(user.id())) {
            throw new ConflictException("The organizer cannot leave their own study session");
        }
        SessionParticipant participant = participantRepository
                .findBySession_IdAndUserId(sessionId, user.id())
                .orElse(null);

        if (participant == null) {
            // Not a participant: a waitlisted user may leave the queue instead.
            SessionWaitlist waiting = waitlistRepository
                    .findBySession_IdAndUserId(sessionId, user.id())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "You are not a participant or waitlisted in this session"));
            waitlistRepository.delete(waiting);
            sessionActivityService.record(session, user.id(), SessionEventType.LEFT_WAITLIST,
                    "Left the waiting list from position " + waiting.getPosition(),
                    List.of(user.id()),
                    "You have left the waiting list for \"" + session.getTitle() + "\"");
            log.info("User {} left the waitlist of session {}", username, sessionId);
            return;
        }

        participantRepository.delete(participant);
        participantRepository.flush();
        session.setStatus(StudySessionStatus.OPEN);
        sessionActivityService.record(session, user.id(), SessionEventType.LEFT,
                "Left the session",
                List.of(session.getOrganizerUserId()),
                username + " left your study session \"" + session.getTitle() + "\"");
        promoteEligibleWaiters(session);
        updateCapacityStatus(session);
        log.info("User {} left session {}", username, sessionId);
    }

    /**
     * Fills available seats with the earliest eligible waitlisted users.
     * Ineligible users retain their original positions and can be promoted later.
     * Call only while holding the global transaction-level advisory lock.
     */
    private void promoteEligibleWaiters(StudySession session) {
        if (!ACTIVE_STATUSES.contains(session.getStatus())) {
            return;
        }
        Booking booking = session.getBooking();
        if (booking.getStatus() != BookingStatus.CONFIRMED || !booking.getEndTime().isAfter(LocalDateTime.now())) {
            return;
        }

        long count = participantRepository.countBySession_Id(session.getId());
        if (count >= session.getCapacity()) {
            return;
        }

        List<SessionWaitlist> queue = waitlistRepository
                .findBySession_IdOrderByPositionAsc(session.getId());
        for (SessionWaitlist entry : queue) {
            if (count >= session.getCapacity()) {
                break;
            }

            Long candidate = entry.getUserId();
            if (participantRepository.existsBySession_IdAndUserId(session.getId(), candidate)
                    || hasOverlappingParticipation(candidate, booking)) {
                continue;
            }
            addParticipant(session, candidate);
            waitlistRepository.delete(entry);
            sessionActivityService.record(session, candidate, SessionEventType.PROMOTED,
                    "Promoted from waitlist position " + entry.getPosition(),
                    List.of(candidate),
                    "A seat opened up: you have joined \"" + session.getTitle() + "\"");
            count++;
            log.info("Promoted user {} in session {}", candidate, session.getId());
        }
        // Flush before a subsequent count query in the same transaction.
        participantRepository.flush();
        waitlistRepository.flush();
        session.setStatus(count >= session.getCapacity() ? StudySessionStatus.FULL : StudySessionStatus.OPEN);
    }

   private void updateCapacityStatus(StudySession session) {
        long count = participantRepository.countBySession_Id(session.getId());
        session.setStatus(count >= session.getCapacity() ? StudySessionStatus.FULL : StudySessionStatus.OPEN);
    }

    private void addParticipant(StudySession session, Long userId) {
        SessionParticipant participant = new SessionParticipant();
        participant.setSession(session);
        participant.setUserId(userId);
        participantRepository.saveAndFlush(participant);
    }

    private boolean hasOverlappingParticipation(Long userId, Booking booking) {
        return participantRepository.existsOverlappingSession(
                userId, booking.getStartTime(), booking.getEndTime(), ACTIVE_STATUSES);
    }

    private void ensureSessionJoinable(StudySession session) {
        if (!ACTIVE_STATUSES.contains(session.getStatus())) {
            throw new ConflictException("This study session is not accepting participants");
        }
        ensureBookingActive(session.getBooking());
    }

    private void ensureBookingActive(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("The booking must be confirmed");
        }
        if (!booking.getEndTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException("The booking has already ended");
        }
    }

    /** Converts an entity to the existing response DTO. */
    private StudySessionResponse toResponse(StudySession session) {
        Booking booking = session.getBooking();
        return new StudySessionResponse(
                session.getId(), session.getTitle(), session.getDescription(),
                session.getCapacity(), session.getStatus(), booking.getId(),
                booking.getStudySpace().getId(), identityClient.username(session.getOrganizerUserId()),
                booking.getStartTime(), booking.getEndTime(),participantRepository.countBySession_Id(session.getId()),
                waitlistRepository.countBySession_Id(session.getId()));
    }

    /**
     * Returns the history of a session, oldest first. Only the organizer,
     * current participants, waitlisted users and administrators may read it.
     */
    @Transactional(readOnly = true)
    public List<SessionEventResponse> listEvents(Long sessionId, String username) {
        JwtPrincipal user = currentUser.get();
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Study session not found"));

        boolean allowed = user.role() == Role.ADMIN
                || session.getOrganizerUserId().equals(user.id())
                || participantRepository.existsBySession_IdAndUserId(sessionId, user.id())
                || waitlistRepository.existsBySession_IdAndUserId(sessionId, user.id());

        if (!allowed) {
            throw new ForbiddenException("You are not allowed to view this session's history");
        }

        return sessionEventRepository.findBySession_IdOrderByIdAsc(sessionId)
                .stream()
                .map(e -> SessionEventResponse.from(e, identityClient.username(e.getUserId())))
                .toList();
    }

    /**
     * Returns all study sessions, newest first.
     */
    @Transactional(readOnly = true)
    public List<StudySessionResponse> listSessions() {
        return studySessionRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "id")
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }
}
