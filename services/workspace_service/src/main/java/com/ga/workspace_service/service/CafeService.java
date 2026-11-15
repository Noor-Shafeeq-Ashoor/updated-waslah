
package com.ga.workspace_service.service;

import com.ga.workspace_service.dto.CafeRequest;
import com.ga.workspace_service.dto.CafeResponse;
import com.ga.workspace_service.dto.PageResponse;
import com.ga.workspace_service.exception.ForbiddenException;
import com.ga.workspace_service.exception.ResourceNotFoundException;
import com.ga.workspace_service.model.Cafe;
import com.ga.workspace_service.model.Role;
import com.ga.workspace_service.repository.CafeRepository;
import com.ga.workspace_service.security.CurrentUser;
import com.ga.workspace_service.security.JwtPrincipal;
import com.ga.workspace_service.util.PagingUtil;
import com.ga.workspace_service.util.Specs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Handles cafe-related business logic, including creating, updating,
 * activating, deactivating, and searching for cafes.
 */
@Service
public class CafeService {

    private final CafeRepository cafeRepository;
    private final CurrentUser currentUser;
    private final IdentityClient identityClient;
    private final AuditService auditService;


    public CafeService(
            CafeRepository cafeRepository,
            CurrentUser currentUser,
            IdentityClient identityClient,
            AuditService auditService
    ) {
        this.cafeRepository = cafeRepository;
        this.currentUser = currentUser;
        this.identityClient = identityClient;
        this.auditService = auditService;
    }

    /**
     * Creates a cafe owned by the authenticated cafe manager.
     *
     * @param username username of the authenticated manager
     * @param request cafe details supplied by the client
     * @return the saved cafe as a response DTO
     */
    public CafeResponse createCafe(
            String username,
            CafeRequest request
    ) {
        JwtPrincipal manager = getUser(username);

        // Only cafe managers are allowed to create cafes.
        checkCafeManager(manager);

        Cafe cafe = new Cafe();

        cafe.setName(request.getName());
        cafe.setDescription(request.getDescription());
        cafe.setLocation(request.getLocation());

        // Newly created cafes are active by default.
        cafe.setActive(true);

        // Assign the authenticated manager as the cafe owner.
        cafe.setManagerUserId(manager.id());

        Cafe savedCafe = cafeRepository.save(cafe);

        auditService.log(
                username,
                "CAFE_CREATED",
                "CAFE",
                savedCafe.getId()
        );

        return toResponse(savedCafe);
    }


    public List<CafeResponse> getAllActiveCafes() {
        return cafeRepository.findAll()
                .stream()
                .filter(Cafe::getActive)
                .map(this::toResponse)
                .toList();
    }


    public PageResponse<CafeResponse> getActiveCafes(
            String name,
            String location,
            int page,
            int size,
            String sort,
            String direction
    ) {
        // Always restrict public search results to active cafes.
        Specification<Cafe> specification =
                Specs.<Cafe>equal(Boolean.TRUE, "active")
                        .and(Specs.containsIgnoreCase(name, "name"))
                        .and(Specs.containsIgnoreCase(location, "location"));

        // Only these fields may be used for sorting.
        // The default order is by cafe name, ascending.
        Pageable pageable = PagingUtil.createPageable(
                page,
                size,
                sort,
                direction,
                "name",
                Set.of("name", "location")
        );

        // Apply filters and pagination in the database query.
        Page<Cafe> cafePage = cafeRepository.findAll(
                specification,
                pageable
        );

        // Convert the page of Cafe entities into response DTOs.
        List<CafeResponse> content = cafePage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        // Return the results together with useful pagination information.
        return new PageResponse<>(
                content,
                cafePage.getNumber(),
                cafePage.getSize(),
                cafePage.getTotalElements(),
                cafePage.getTotalPages(),
                cafePage.isFirst(),
                cafePage.isLast()
        );
    }

    /**
     * Retrieves a cafe by its ID.
     *
     * @param id ID of the cafe
     * @return the cafe response
     */
    public CafeResponse getCafeById(Long id) {
        Cafe cafe = getCafe(id);
        return toResponse(cafe);
    }

    /**
     * Retrieves all cafes managed by the specified user.
     *
     * @param username username of the cafe manager
     * @return cafes belonging to that manager
     */
    public List<CafeResponse> getManagerCafes(String username) {
        JwtPrincipal manager = getUser(username);

        // Only cafe managers can access their managed cafes.
        checkCafeManager(manager);

        return cafeRepository.findByManagerUserId(manager.id())
                .stream()
                .map(this::toResponse)
                .toList();
    }


    public CafeResponse updateCafe(
            Long id,
            String username,
            CafeRequest request
    ) {
        JwtPrincipal manager = getUser(username);

        checkCafeManager(manager);

        Cafe cafe = getCafe(id);

        // Managers may only update cafes they own.
        checkOwnership(cafe, manager);

        cafe.setName(request.getName());
        cafe.setDescription(request.getDescription());
        cafe.setLocation(request.getLocation());

        Cafe savedCafe = cafeRepository.save(cafe);

        auditService.log(
                username,
                "CAFE_UPDATED",
                "CAFE",
                savedCafe.getId()
        );

        return toResponse(savedCafe);
    }

    /**
     * Deactivates a cafe without deleting its database record.
     *
     * @param id ID of the cafe to deactivate
     * @param username username of the authenticated manager
     */
    public void deactivateCafe(Long id, String username) {
        JwtPrincipal manager = getUser(username);

        checkCafeManager(manager);

        Cafe cafe = getCafe(id);

        // Only the cafe's manager can deactivate it.
        checkOwnership(cafe, manager);

        cafe.setActive(false);

        cafeRepository.save(cafe);

        auditService.log(
                username,
                "CAFE_DEACTIVATED",
                "CAFE",
                cafe.getId()
        );
    }

    /**
     * Activates a cafe managed by the specified user.
     *
     * @param id ID of the cafe to activate
     * @param username username of the authenticated manager
     */
    public void activateCafe(Long id, String username) {
        JwtPrincipal manager = getUser(username);

        checkCafeManager(manager);

        Cafe cafe = getCafe(id);

        checkOwnership(cafe, manager);

        cafe.setActive(true);

        cafeRepository.save(cafe);

        auditService.log(
                username,
                "CAFE_ACTIVATED",
                "CAFE",
                cafe.getId()
        );
    }


    private JwtPrincipal getUser(String username)  {
        return currentUser.get();
    }

    private Cafe getCafe(Long id) {
        return cafeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cafe not found")
                );
    }


    private void checkCafeManager(JwtPrincipal user) {
        if (user.role() != Role.CAFE_MANAGER) {
            throw new ForbiddenException(
                    "Only Cafe Managers can manage cafes"
            );
        }
    }


    private void checkOwnership(Cafe cafe, JwtPrincipal manager) {
        if (cafe.getManagerUserId() == null
                || !cafe.getManagerUserId().equals(manager.id())) {
            throw new ForbiddenException(
                    "You are not allowed to manage this cafe"
            );
        }
    }

    /**
     * Converts a Cafe entity into a CafeResponse DTO.
     *
     * @param cafe cafe entity to convert
     * @return the response DTO
     */
    private CafeResponse toResponse(Cafe cafe) {
        return new CafeResponse(
                cafe.getId(),
                cafe.getName(),
                cafe.getDescription(),
                cafe.getLocation(),
                cafe.getActive(),
                identityClient.username(cafe.getManagerUserId())
        );
    }
}
