
package com.ga.waslah.service;

import com.ga.waslah.dto.CafeRequest;
import com.ga.waslah.dto.CafeResponse;
import com.ga.waslah.dto.PageResponse;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.Cafe;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.CafeRepository;
import com.ga.waslah.repository.UserRepository;
import com.ga.waslah.util.PagingUtil;
import com.ga.waslah.util.Specs;

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
    private final UserRepository userRepository;
    private final AuditService auditService;

    /**
     * Creates the service with the repositories required for cafe operations.
     *
     * @param cafeRepository repository used to access cafe records
     * @param userRepository repository used to retrieve users
     */
    public CafeService(
            CafeRepository cafeRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.cafeRepository = cafeRepository;
        this.userRepository = userRepository;
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
        User manager = getUser(username);

        // Only cafe managers are allowed to create cafes.
        checkCafeManager(manager);

        Cafe cafe = new Cafe();

        cafe.setName(request.getName());
        cafe.setDescription(request.getDescription());
        cafe.setLocation(request.getLocation());

        // Newly created cafes are active by default.
        cafe.setActive(true);

        // Assign the authenticated manager as the cafe owner.
        cafe.setManager(manager);

        Cafe savedCafe = cafeRepository.save(cafe);

        auditService.log(
                username,
                "CAFE_CREATED",
                "CAFE",
                savedCafe.getId()
        );

        return toResponse(savedCafe);
    }

    /**
     * Retrieves all active cafes without pagination.
     *
     * <p>This method is preserved for existing callers while the controller
     * is being updated to use the paginated endpoint.</p>
     *
     * @return a list of active cafes
     */
    public List<CafeResponse> getAllActiveCafes() {
        return cafeRepository.findAll()
                .stream()
                .filter(Cafe::getActive)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves active cafes using optional filters, pagination, and sorting.
     *
     * <p>Name and location filters perform case-insensitive partial matching.
     * Only active cafes are returned to prevent inactive cafes from appearing
     * in public search results.</p>
     *
     * @param name optional cafe name filter
     * @param location optional cafe location filter
     * @param page zero-based page number
     * @param size number of records per page
     * @param sort field used to sort the results
     * @param direction sorting direction: asc or desc
     * @return a page of cafe response DTOs with pagination metadata
     */
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
        User manager = getUser(username);

        // Only cafe managers can access their managed cafes.
        checkCafeManager(manager);

        return cafeRepository.findByManager(manager)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Updates a cafe managed by the specified user.
     *
     * @param id ID of the cafe to update
     * @param username username of the authenticated manager
     * @param request updated cafe details
     * @return the updated cafe response
     */
    public CafeResponse updateCafe(
            Long id,
            String username,
            CafeRequest request
    ) {
        User manager = getUser(username);

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
        User manager = getUser(username);

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
        User manager = getUser(username);

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

    /**
     * Finds a user by username.
     *
     * @param username username of the user
     * @return the matching user
     * @throws ResourceNotFoundException if the user does not exist
     */
    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );
    }

    /**
     * Finds a cafe by ID.
     *
     * @param id ID of the cafe
     * @return the matching cafe
     * @throws ResourceNotFoundException if the cafe does not exist
     */
    private Cafe getCafe(Long id) {
        return cafeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cafe not found")
                );
    }

    /**
     * Ensures that a user has the cafe manager role.
     *
     * @param user user whose role must be checked
     * @throws ForbiddenException if the user is not a cafe manager
     */
    private void checkCafeManager(User user) {
        if (user.getRole() != Role.CAFE_MANAGER) {
            throw new ForbiddenException(
                    "Only Cafe Managers can manage cafes"
            );
        }
    }

    /**
     * Ensures that the supplied manager owns the specified cafe.
     *
     * @param cafe cafe whose ownership must be checked
     * @param manager user attempting to manage the cafe
     * @throws ForbiddenException if the manager does not own the cafe
     */
    private void checkOwnership(Cafe cafe, User manager) {
        if (cafe.getManager() == null
                || !cafe.getManager().getId().equals(manager.getId())) {
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
                cafe.getManager().getUsername()
        );
    }
}
