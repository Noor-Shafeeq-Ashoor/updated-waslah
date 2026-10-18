
package com.ga.waslah.repository;

import com.ga.waslah.model.Cafe;
import com.ga.waslah.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Repository for managing cafe records.
 *
 * <p>Supports standard CRUD operations, finding cafes by their manager,
 * and dynamic filtering, pagination, and sorting.</p>
 */
public interface CafeRepository extends
        JpaRepository<Cafe, Long>,
        JpaSpecificationExecutor<Cafe> {

    /**
     * Finds cafes managed by the specified user.
     *
     * @param manager the user who manages the cafes
     * @return a list of cafes managed by that user
     */
    List<Cafe> findByManager(User manager);
}
