package com.ga.waslah.service;

import com.ga.waslah.dto.EmployerRequestRequest;
import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.EmployerRequest;
import com.ga.waslah.model.EmployerRequestStatus;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.EmployerRequestRepository;
import com.ga.waslah.repository.UserRepository;
import com.ga.waslah.dto.EmployerRequestResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles employer requests, including submission, retrieval,
 * approval, and rejection.
 *
 * Approved requests grant the requesting user the EMPLOYER role.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerRequestService {

    private final EmployerRequestRepository employerRequestRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public EmployerRequest createRequest(
            String username,
            EmployerRequestRequest request
    ) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Existing employers do not need to request the role again.
        if (user.getRole() == Role.EMPLOYER) {
            log.warn("Employer request rejected: user already has employer role");

            throw new ConflictException(
                    "You are already an employer"
            );
        }

        var existingRequest =
                employerRequestRepository.findByUserUsername(username);

        if (existingRequest.isPresent()) {

            EmployerRequest oldRequest = existingRequest.get();

            if (oldRequest.getStatus() == EmployerRequestStatus.PENDING) {
                log.warn("Employer request rejected: pending request already exists");

                throw new ConflictException(
                        "You already have a pending employer request"
                );
            }

            if (oldRequest.getStatus() == EmployerRequestStatus.APPROVED) {
                log.warn("Employer request rejected: request already approved");

                throw new ConflictException(
                        "Your employer request has already been approved"
                );
            }

            // A rejected request can be replaced by a new submission.
            employerRequestRepository.delete(oldRequest);
        }

        EmployerRequest employerRequest = new EmployerRequest();

        employerRequest.setCompanyName(request.getCompanyName());
        employerRequest.setCompanyDescription(
                request.getCompanyDescription()
        );
        employerRequest.setJobTypes(request.getJobTypes());
        employerRequest.setCommercialRegistration(
                request.getCommercialRegistration()
        );
        employerRequest.setStatus(EmployerRequestStatus.PENDING);
        employerRequest.setUser(user);

        EmployerRequest savedRequest =
                employerRequestRepository.save(employerRequest);

        log.info("Employer request submitted successfully with id={}",
                savedRequest.getId());

        return savedRequest;
    }

    /**
     * Retrieves the employer request belonging to a user.
     *
     * @param username the username of the requesting user
     * @return the user's employer request
     * @throws ResourceNotFoundException if no request exists
     */
    public EmployerRequest getMyRequest(String username) {

        EmployerRequest request = employerRequestRepository
                .findByUserUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employer request not found"
                        ));

        log.debug("Employer request retrieved successfully");

        return request;
    }

    /**
     * Retrieves all employer requests awaiting an administrator's decision.
     *
     * @return a list of pending employer requests
     */
    public List<EmployerRequest> getPendingRequests() {

        List<EmployerRequest> requests =
                employerRequestRepository.findByStatus(
                        EmployerRequestStatus.PENDING
                );

        log.debug("Retrieved pending employer requests");

        return requests;
    }

    /**
     * Retrieves employer requests with the given status, or all of them when
     * {@code status} is null. Used by the admin dashboard.
     */
    public List<EmployerRequest> getRequests(EmployerRequestStatus status) {
        return status == null
                ? employerRequestRepository.findAll()
                : employerRequestRepository.findByStatus(status);
    }

    /**
     * Approves a pending employer request and grants the user
     * the EMPLOYER role.
     *
     * @param requestId the ID of the request to approve
     * @return the approved employer request
     * @throws ResourceNotFoundException if the request does not exist
     * @throws ConflictException if the request has already been processed
     */
    @Transactional
    public EmployerRequest approveRequest(
            Long requestId,
            String adminUsername
    ) {

        EmployerRequest request =
                employerRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employer request not found"
                                ));

        // Only pending requests can be approved.
        if (request.getStatus() != EmployerRequestStatus.PENDING) {
            log.warn("Employer request approval rejected: request already processed");

            throw new ConflictException(
                    "Employer request has already been processed"
            );
        }

        request.setStatus(EmployerRequestStatus.APPROVED);

        // Grant the employer role only after approval.
        User user = request.getUser();
        user.setRole(Role.EMPLOYER);

        userRepository.save(user);

        EmployerRequest approvedRequest =
                employerRequestRepository.save(request);

        auditService.log(
                adminUsername,
                "EMPLOYER_REQUEST_APPROVED",
                "EMPLOYER_REQUEST",
                approvedRequest.getId()
        );

        log.info("Employer request approved successfully with id={}",
                requestId);

        return approvedRequest;
    }

    /**
     * Rejects a pending employer request without changing the user's role.
     *
     * @param requestId the ID of the request to reject
     * @return the rejected employer request
     * @throws ResourceNotFoundException if the request does not exist
     * @throws ConflictException if the request has already been processed
     */
    @Transactional
    public EmployerRequest rejectRequest(
            Long requestId,
            String adminUsername
    ) {

        EmployerRequest request =
                employerRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employer request not found"
                                ));

        // Only pending requests can be rejected.
        if (request.getStatus() != EmployerRequestStatus.PENDING) {
            log.warn("Employer request rejection rejected: request already processed");

            throw new ConflictException(
                    "Employer request has already been processed"
            );
        }

        request.setStatus(EmployerRequestStatus.REJECTED);

        EmployerRequest rejectedRequest =
                employerRequestRepository.save(request);

        auditService.log(
                adminUsername,
                "EMPLOYER_REQUEST_REJECTED",
                "EMPLOYER_REQUEST",
                rejectedRequest.getId()
        );

        log.info("Employer request rejected successfully with id={}",
                requestId);

        return rejectedRequest;
    }

    /**
     * Converts an employer request entity into a response DTO.
     */
    public EmployerRequestResponse toResponse(EmployerRequest request) {

        return new EmployerRequestResponse(
                request.getId(),
                request.getCompanyName(),
                request.getCompanyDescription(),
                request.getJobTypes(),
                request.getCommercialRegistration(),
                request.getStatus(),
                request.getUser() == null ? null : request.getUser().getUsername()
        );
    }

}
