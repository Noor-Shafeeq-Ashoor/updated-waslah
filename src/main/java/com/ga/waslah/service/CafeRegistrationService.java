package com.ga.waslah.service;

import com.ga.waslah.dto.CafeRegistrationDecisionRequest;
import com.ga.waslah.dto.CafeRegistrationRequest;
import com.ga.waslah.dto.CafeRegistrationResponse;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.Cafe;
import com.ga.waslah.model.CafeRequestStatus;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.CafeRegistrationRequestRepository;
import com.ga.waslah.repository.CafeRepository;
import com.ga.waslah.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CafeRegistrationService {

    private final CafeRegistrationRequestRepository requestRepository;
    private final CafeRepository cafeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CafeRegistrationService(
            CafeRegistrationRequestRepository requestRepository,
            CafeRepository cafeRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.requestRepository = requestRepository;
        this.cafeRepository = cafeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // =========================
    // SUBMIT CAFE REQUEST
    // =========================

    public CafeRegistrationResponse submitRequest(
            String username,
            CafeRegistrationRequest request
    ) {

        User user = getUser(username);

        /*
         * Only normal users can submit a cafe registration request.
         * Existing Cafe Managers do not need to request approval again.
         */
        if (user.getRole() != Role.USER) {
            throw new ForbiddenException(
                    "Only users can submit cafe registration requests"
            );
        }

        com.ga.waslah.model.CafeRegistrationRequest registrationRequest =
                new com.ga.waslah.model.CafeRegistrationRequest();

        registrationRequest.setCafeName(request.getCafeName());
        registrationRequest.setDescription(request.getDescription());
        registrationRequest.setLocation(request.getLocation());

        // The request always starts as PENDING.
        registrationRequest.setStatus(CafeRequestStatus.PENDING);

        // The authenticated user becomes the requester.
        registrationRequest.setRequestedBy(user);

        var savedRequest = requestRepository.save(registrationRequest);

        auditService.log(
                username,
                "CAFE_REQUEST_SUBMITTED",
                "CAFE_REQUEST",
                savedRequest.getId()
        );

        return toResponse(savedRequest);
    }

    // =========================
    // GET MY REQUESTS
    // =========================

    public List<CafeRegistrationResponse> getMyRequests(
            String username
    ) {

        User user = getUser(username);

        return requestRepository.findByRequestedBy(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================
    // GET PENDING REQUESTS
    // =========================

    public List<CafeRegistrationResponse> getPendingRequests(
            String username
    ) {

        User admin = getUser(username);

        checkAdmin(admin);

        return requestRepository
                .findByStatus(CafeRequestStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================
    // GET REQUESTS BY STATUS (admin dashboard)
    // =========================

    /** All café requests, or only those with the given status when it is not null. */
    public List<CafeRegistrationResponse> getRequests(
            String username,
            CafeRequestStatus status
    ) {

        checkAdmin(getUser(username));

        List<com.ga.waslah.model.CafeRegistrationRequest> requests =
                status == null
                        ? requestRepository.findAll()
                        : requestRepository.findByStatus(status);

        return requests.stream().map(this::toResponse).toList();
    }

    // =========================
    // APPROVE / REJECT REQUEST
    // =========================

    public CafeRegistrationResponse decideRequest(
            Long requestId,
            String username,
            CafeRegistrationDecisionRequest decisionRequest
    ) {

        User admin = getUser(username);

        checkAdmin(admin);

        com.ga.waslah.model.CafeRegistrationRequest registrationRequest =
                requestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Cafe registration request not found"
                                ));

        // A request can only be decided while it is still pending.
        if (registrationRequest.getStatus()
                != CafeRequestStatus.PENDING) {

            throw new ForbiddenException(
                    "This request has already been processed"
            );
        }

        CafeRequestStatus decision =
                decisionRequest.getDecision();

        // Admin must choose APPROVED or REJECTED.
        if (decision == CafeRequestStatus.PENDING) {

            throw new ForbiddenException(
                    "Admin must approve or reject the request"
            );
        }

        // =========================
        // REJECT REQUEST
        // =========================

        if (decision == CafeRequestStatus.REJECTED) {

            registrationRequest.setStatus(
                    CafeRequestStatus.REJECTED
            );

            var rejectedRequest = requestRepository.save(registrationRequest);

            auditService.log(
                    username,
                    "CAFE_REQUEST_REJECTED",
                    "CAFE_REQUEST",
                    rejectedRequest.getId()
            );

            return toResponse(rejectedRequest);
        }

        // =========================
        // APPROVE REQUEST
        // =========================

        registrationRequest.setStatus(
                CafeRequestStatus.APPROVED
        );

        User manager = registrationRequest.getRequestedBy();

        // Promote the user to Cafe Manager.
        manager.setRole(Role.CAFE_MANAGER);

        userRepository.save(manager);

        // Create the actual Cafe after approval.
        Cafe cafe = new Cafe();

        cafe.setName(registrationRequest.getCafeName());
        cafe.setDescription(registrationRequest.getDescription());
        cafe.setLocation(registrationRequest.getLocation());

        cafe.setActive(true);

        cafe.setManager(manager);

        cafeRepository.save(cafe);

        var approvedRequest = requestRepository.save(registrationRequest);

        auditService.log(
                username,
                "CAFE_REQUEST_APPROVED",
                "CAFE_REQUEST",
                approvedRequest.getId()
        );

        return toResponse(approvedRequest);
    }

    // =========================
    // HELPER METHODS
    // =========================

    private User getUser(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));
    }

    private void checkAdmin(User user) {

        if (user.getRole() != Role.ADMIN) {

            throw new ForbiddenException(
                    "Only administrators can manage cafe registration requests"
            );
        }
    }

    private CafeRegistrationResponse toResponse(
            com.ga.waslah.model.CafeRegistrationRequest request
    ) {

        return new CafeRegistrationResponse(
                request.getId(),
                request.getCafeName(),
                request.getDescription(),
                request.getLocation(),
                request.getStatus(),
                request.getRequestedBy().getUsername(),
                request.getCreatedAt()
        );
    }
}