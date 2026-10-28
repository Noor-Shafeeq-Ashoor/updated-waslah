
package com.ga.waslah.repository;

import com.ga.waslah.model.Booking;
import com.ga.waslah.model.BookingStatus;
import com.ga.waslah.model.StudySpace;
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

    List<Booking> findByUser_Username(String username);
    long countByStatus(BookingStatus status);
    List<Booking> findByStudySpace(StudySpace studySpace);
    boolean existsByStudySpaceAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            StudySpace studySpace,
            List<BookingStatus> statuses,
            LocalDateTime requestedEndTime,
            LocalDateTime requestedStartTime
    );

    List<Booking> findByStudySpace_Cafe_Manager_Username(String username);

    List<Booking> findByStatusAndEndTimeLessThan(
            BookingStatus status,
            LocalDateTime endTime
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

}
