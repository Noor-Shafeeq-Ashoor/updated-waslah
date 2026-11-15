package com.ga.workspace_service.service;

import com.ga.workspace_service.dto.CafeRegistrationDecisionRequest;
import com.ga.workspace_service.dto.CafeRegistrationRequest;
import com.ga.workspace_service.dto.CafeRegistrationResponse;
import com.ga.workspace_service.exception.ForbiddenException;
import com.ga.workspace_service.exception.ResourceNotFoundException;
import com.ga.workspace_service.model.Cafe;
import com.ga.workspace_service.model.CafeRequestStatus;
import com.ga.workspace_service.model.Role;
import com.ga.workspace_service.repository.CafeRegistrationRequestRepository;
import com.ga.workspace_service.repository.CafeRepository;

import com.ga.workspace_service.security.CurrentUser;
import com.ga.workspace_service.security.JwtPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CafeRegistrationService {

    private final CafeRegistrationRequestRepository requestRepository;
    private final CafeRepository cafeRepository;
    private final CurrentUser currentUser;
    private final IdentityClient identityClient;
    private final AuditService auditService;

    public CafeRegistrationService(
            CafeRegistrationRequestRepository requestRepository,
            CafeRepository cafeRepository,
            CurrentUser currentUser,
            IdentityClient identityClient,
            AuditService auditService
    ) {
        this.requestRepository = requestRepository;
        this.cafeRepository = cafeRepository;
        this.currentUser = currentUser;
        this.identityClient = identityClient;
        this.auditService = auditService;
    }

    // =========================
    // SUBMIT CAFE REQUEST
    // =========================

    public CafeRegistrationResponse submitRequest(
            String username,
            CafeRegistrationRequest request
    ) {

        JwtPrincipal user = getUser(username);

        /*
         * Only normal users can submit a cafe registration request.
         * Existing Cafe Managers do not need to request approval again.
         */
        if (user.role() != Role.USER) {
            throw new ForbiddenException(
                    "Only users can submit cafe registration requests"
            );
        }

        com.ga.workspace_service.model.CafeRegistrationRequest registrationRequest =
                new com.ga.workspace_service.model.CafeRegistrationRequest();

        registrationRequest.setCafeName(request.getCafeName());
        registrationRequest.setDescription(request.getDescription());
        registrationRequest.setLocation(request.getLocation());

        // The request always starts as PENDING.
        registrationRequest.setStatus(CafeRequestStatus.PENDING);

        // The authenticated user becomes the requester.
        registrationRequest.setRequestedByUserId(user.id());

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

        JwtPrincipal user = getUser(username);

        return requestRepository.findByRequestedByUserId(user.id())
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

        JwtPrincipal admin = getUser(username);

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

        List<com.ga.workspace_service.model.CafeRegistrationRequest> requests =
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

        JwtPrincipal admin = getUser(username);

        checkAdmin(admin);

        com.ga.workspace_service.model.CafeRegistrationRequest registrationRequest =
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

        Long managerUserId = registrationRequest.getRequestedByUserId();

        // Create the actual Cafe after approval.
        Cafe cafe = new Cafe();

        cafe.setName(registrationRequest.getCafeName());
        cafe.setDescription(registrationRequest.getDescription());
        cafe.setLocation(registrationRequest.getLocation());

        cafe.setActive(true);

        cafe.setManagerUserId(managerUserId);

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

    private JwtPrincipal getUser(String username) {
        return currentUser.get();
    }

    private void checkAdmin(JwtPrincipal user) {

        if (user.role() != Role.ADMIN) {

            throw new ForbiddenException(
                    "Only administrators can manage cafe registration requests"
            );
        }
    }

    private CafeRegistrationResponse toResponse(
            com.ga.workspace_service.model.CafeRegistrationRequest request
    ) {

        return new CafeRegistrationResponse(
                request.getId(),
                request.getCafeName(),
                request.getDescription(),
                request.getLocation(),
                request.getStatus(),
                identityClient.username(request.getRequestedByUserId()),
                request.getCreatedAt()
        );
    }
}