
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


public interface StudySpaceRepository extends
        JpaRepository<StudySpace, Long>,
        JpaSpecificationExecutor<StudySpace> {

    List<StudySpace> findByAvailableTrueAndCafe_ActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StudySpace s WHERE s.id = :id")
    Optional<StudySpace> findByIdForUpdate(@Param("id") Long id);
}
