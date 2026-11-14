
package com.ga.workspace_service.repository;

import com.ga.workspace_service.model.Booking;
import com.ga.workspace_service.model.BookingStatus;
import com.ga.workspace_service.model.StudySpace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends
        JpaRepository<Booking, Long>,
        JpaSpecificationExecutor<Booking> {

    List<Booking> findByUserId(Long userId);
    long countByStatus(BookingStatus status);
    List<Booking> findByStudySpace(StudySpace studySpace);
    boolean existsByStudySpaceAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            StudySpace studySpace,
            List<BookingStatus> statuses,
            LocalDateTime requestedEndTime,
            LocalDateTime requestedStartTime
    );

    List<Booking> findByStudySpace_Cafe_ManagerUserId(Long managerUserId);

    List<Booking> findByStatusAndEndTimeLessThan(
            BookingStatus status,
            LocalDateTime endTime
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

}
