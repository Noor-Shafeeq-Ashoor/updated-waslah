
package com.ga.waslah.repository;

import com.ga.waslah.model.StudySpace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing study spaces.
 *
 * <p>Supports standard CRUD operations, dynamic filtering and pagination,
 * retrieving available spaces, and locking a space record when needed.</p>
 */
public interface StudySpaceRepository extends
        JpaRepository<StudySpace, Long>,
        JpaSpecificationExecutor<StudySpace> {

    /**
     * Finds available study spaces belonging to active cafes.
     *
     * @return a list of available study spaces in active cafes
     */
    List<StudySpace> findByAvailableTrueAndCafe_ActiveTrue();

    /**
     * Retrieves a study space while acquiring a pessimistic write lock.
     *
     * <p>This can help prevent concurrent transactions from modifying
     * the same record during operations such as booking.</p>
     *
     * @param id the ID of the study space
     * @return the matching study space, or an empty Optional if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StudySpace s WHERE s.id = :id")
    Optional<StudySpace> findByIdForUpdate(@Param("id") Long id);
}
